/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.mcp.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableShape;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.framework.model.ValidationException;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.java.json.JsonSettings;
import software.amazon.smithy.java.logging.InternalLogger;
import software.amazon.smithy.java.mcp.model.CallToolResult;
import software.amazon.smithy.java.mcp.model.Capabilities;
import software.amazon.smithy.java.mcp.model.InitializeResult;
import software.amazon.smithy.java.mcp.model.JsonArraySchema;
import software.amazon.smithy.java.mcp.model.JsonObjectSchema;
import software.amazon.smithy.java.mcp.model.JsonPrimitiveSchema;
import software.amazon.smithy.java.mcp.model.JsonPrimitiveType;
import software.amazon.smithy.java.mcp.model.JsonRpcErrorResponse;
import software.amazon.smithy.java.mcp.model.JsonRpcRequest;
import software.amazon.smithy.java.mcp.model.JsonRpcResponse;
import software.amazon.smithy.java.mcp.model.ListToolsResult;
import software.amazon.smithy.java.mcp.model.McpResource;
import software.amazon.smithy.java.mcp.model.Message;
import software.amazon.smithy.java.mcp.model.MessageContent;
import software.amazon.smithy.java.mcp.model.PromptArgument;
import software.amazon.smithy.java.mcp.model.PromptGetResult;
import software.amazon.smithy.java.mcp.model.PromptInfo;
import software.amazon.smithy.java.mcp.model.Prompts;
import software.amazon.smithy.java.mcp.model.PromptsResult;
import software.amazon.smithy.java.mcp.model.ServerInfo;
import software.amazon.smithy.java.mcp.model.TextContent;
import software.amazon.smithy.java.mcp.model.ToolInfo;
import software.amazon.smithy.java.mcp.model.Tools;
import software.amazon.smithy.java.server.Operation;
import software.amazon.smithy.java.server.Server;
import software.amazon.smithy.java.server.Service;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public final class McpServer implements Server {

    private static final InternalLogger LOG = InternalLogger.getLogger(McpServer.class);

    private static final JsonCodec CODEC = JsonCodec.builder()
            .settings(JsonSettings.builder()
                    .serializeTypeInDocuments(false)
                    .useJsonName(true)
                    .build())
            .build();

    private final Map<String, Tool> tools;
    private final Map<String, PromptInfo> prompts;
    private final Thread listener;
    private final InputStream is;
    private final OutputStream os;
    private final String name;
    private final List<McpServerProxy> proxies;
    private final CountDownLatch done = new CountDownLatch(1);

    McpServer(McpServerBuilder builder) {
        this.tools = createTools(builder.serviceList);
        this.prompts = createPrompts();
        this.is = builder.is;
        this.os = builder.os;
        this.name = builder.name;
        this.proxies = new ArrayList<>(builder.proxyList);
        this.listener = new Thread(() -> {
            try {
                this.listen();
            } catch (Exception e) {
                LOG.error("Error handling request", e);
            } finally {
                done.countDown();
            }
        });
        listener.setName("stdio-dispatcher");
        listener.setDaemon(true);
    }

    private void listen() {
        var scan = new Scanner(is, StandardCharsets.UTF_8);
        while (scan.hasNextLine()) {
            var line = scan.nextLine();
            try {
                var jsonRequest = CODEC.deserializeShape(line, JsonRpcRequest.builder());
                handleRequest(jsonRequest);
            } catch (Exception e) {
                LOG.error("Error decoding request", e);
            }
        }
    }

    private void handleRequest(JsonRpcRequest req) {
        try {
            validate(req);
            switch (req.getMethod()) {
                case "initialize" -> writeResponse(req.getId(),
                        InitializeResult.builder()
                                .capabilities(Capabilities.builder()
                                        .tools(Tools.builder().listChanged(true).build())
                                        .prompts(Prompts.builder().listChanged(true).build())
                                        .build())
                                .serverInfo(ServerInfo.builder()
                                        .name(name)
                                        .version("1.0.0")
                                        .build())
                                .build());
                case "prompts/list" -> writeResponse(req.getId(),
                        PromptsResult.builder().prompts(prompts.values().stream().toList()).build());
                case "prompts/get" -> {
                    var promptName = req.getParams().getMember("name").asString();
                    var promptArguments = req.getParams().getMember("arguments");

                    if (prompts == null) {
                        LOG.error("PROMPTS IS NULL");
                        internalError(req, new RuntimeException("Prompts not found: " + promptName));
                    }

                    var prompt = prompts.get(promptName);

                    if (prompt == null) {
                        LOG.error("PROMPT IS NULL");
                        internalError(req, new RuntimeException("Prompt not found: " + promptName));
                        return;
                    }

                    var result = generatePromptResult(prompt, promptArguments);
                    writeResponse(req.getId(), result);
                }
                case "tools/list" -> writeResponse(req.getId(),
                        ListToolsResult.builder().tools(tools.values().stream().map(Tool::toolInfo).toList()).build());
                case "tools/call" -> {
                    var operationName = req.getParams().getMember("name").asString();
                    var tool = tools.get(operationName);

                    // Check if this tool should be dispatched to a proxy
                    if (tool.proxy() != null) {
                        // Forward the request to the proxy
                        JsonRpcRequest proxyRequest = JsonRpcRequest.builder()
                                .id(req.getId())
                                .method(req.getMethod())
                                .params(req.getParams())
                                .jsonrpc(req.getJsonrpc())
                                .build();

                        // Get response asynchronously
                        tool.proxy().rpc(proxyRequest).thenAccept(response -> {
                            // Pass through the response directly
                            synchronized (this) {
                                try {
                                    String serializedResponse = CODEC.serializeToString(response);
                                    os.write(serializedResponse.getBytes(StandardCharsets.UTF_8));
                                    os.write('\n');
                                    os.flush();
                                } catch (Exception e) {
                                    LOG.error("Error writing proxy response", e);
                                }
                            }
                        }).exceptionally(ex -> {
                            LOG.error("Error from proxy RPC", ex);
                            internalError(req, new RuntimeException("Proxy error: " + ex.getMessage(), ex));
                            return null;
                        });

                        // Don't send a response here as it will be sent when the future completes
                        return;
                    } else {
                        // Handle locally
                        var operation = tool.operation();
                        var argumentsDoc = req.getParams().getMember("arguments");
                        var adaptedDoc = adaptDocument(argumentsDoc, operation.getApiOperation().inputSchema());
                        var input = adaptedDoc.asShape(operation.getApiOperation().inputBuilder());
                        var output = operation.function().apply(input, null);
                        var result = CallToolResult.builder()
                                .content(List.of(TextContent.builder()
                                        .text(CODEC.serializeToString((SerializableShape) output))
                                        .build()))
                                .build();
                        writeResponse(req.getId(), result);
                    }
                }
                default -> {
                    //For now don't do anything
                }
            }
        } catch (Exception e) {
            internalError(req, e);
        }
    }

    private void validate(JsonRpcRequest req) {
        Document id = req.getId();
        boolean isRequest = !req.getMethod().startsWith("notifications/");
        if (isRequest) {
            if (id == null) {
                throw ValidationException.builder()
                        .withoutStackTrace()
                        .message("Requests are expected to have ids")
                        .build();
            } else if (!(id.isType(ShapeType.INTEGER) || id.isType(ShapeType.STRING))) {
                throw ValidationException.builder()
                        .withoutStackTrace()
                        .message("Request id is of invalid type " + id.type().name())
                        .build();
            }
        }
    }

    private static final byte[] TOOLS_CHANGED = """
            {"jsonrpc":"2.0","method":"notifications/tools/list_changed"}
            """.getBytes(StandardCharsets.UTF_8); // newline is important here

    public void addNewService(Service service) {
        try {
            synchronized (os) {
                tools.putAll(createTools(List.of(service)));
                os.write(TOOLS_CHANGED);
                os.flush();
            }
        } catch (Exception e) {
            LOG.error("Failed to flush tools changed notification");
        }
    }

    private void writeResponse(Document id, SerializableStruct value) {
        writeResponse(JsonRpcResponse.builder()
                .id(id)
                .result(Document.of(value))
                .jsonrpc("2.0")
                .build());
    }

    private void writeResponse(JsonRpcResponse response) {
        synchronized (os) {
            try {
                os.write(CODEC.serializeToString(response).getBytes(StandardCharsets.UTF_8));
                os.write('\n');
                os.flush();
            } catch (Exception e) {
                LOG.error("Error encoding response", e);
            }
        }
    }

    private void internalError(JsonRpcRequest req, Exception exception) {
        String s;
        try (var sw = new StringWriter();
                var pw = new PrintWriter(sw)) {
            exception.printStackTrace(pw);
            s = sw.toString().replace("\n", "| ");
        } catch (Exception e) {
            LOG.error("Error encoding response", e);
            throw new RuntimeException(e);
        }

        var error = JsonRpcErrorResponse.builder()
                .code(500)
                .message(s)
                .build();
        var response = JsonRpcResponse.builder()
                .id(req.getId())
                .error(error)
                .jsonrpc("2.0")
                .build();
        synchronized (os) {
            try {
                os.write(CODEC.serializeToString(response).getBytes(StandardCharsets.UTF_8));
                os.write('\n');
            } catch (Exception e) {
                LOG.error("Error encoding response", e);
            }
        }
    }

    private static Map<String, PromptInfo> createPrompts() {
        List<PromptInfo> prompts = new ArrayList<>();
        // Add git-commit prompt
        var gitCommit = PromptInfo.builder()
                .name("git-commit")
                .description("Generate a Git commit message")
                .arguments(List.of(
                        PromptArgument.builder()
                                .name("changes")
                                .description("Git diff or description of changes")
                                .required(true)
                                .build()))
                .build();
        prompts.add(gitCommit);

        // Add explain-code prompt
        var explainCode = PromptInfo.builder()
                .name("explain-code")
                .description("Explain how code works")
                .arguments(List.of(
                        PromptArgument.builder()
                                .name("code")
                                .description("Code to explain")
                                .required(false)
                                .build(),
                        PromptArgument.builder()
                                .name("language")
                                .description("Programming language")
                                .required(false)
                                .build()))
                .build();
        prompts.add(explainCode);

        // Fun prompts for testing

        // Pirate translator
        var pirateTalk = PromptInfo.builder()
                .name("pirate-talk")
                .description("Translate text into pirate speak")
                .arguments(List.of(
                        PromptArgument.builder()
                                .name("text")
                                .description("Text to translate")
                                .required(true)
                                .build()))
                .build();
        prompts.add(pirateTalk);

        // Zombie apocalypse survival plan
        var zombiePlan = PromptInfo.builder()
                .name("zombie-plan")
                .description("Generate a zombie apocalypse survival plan")
                .arguments(List.of(
                        PromptArgument.builder()
                                .name("location")
                                .description("Your current location")
                                .required(true)
                                .build(),
                        PromptArgument.builder()
                                .name("resources")
                                .description("Resources you have available")
                                .required(false)
                                .build()))
                .build();
        prompts.add(zombiePlan);

        // Haiku generator
        var haikuGen = PromptInfo.builder()
                .name("haiku")
                .description("Generate a haiku about a topic")
                .arguments(List.of(
                        PromptArgument.builder()
                                .name("topic")
                                .description("Topic for the haiku")
                                .required(true)
                                .build()))
                .build();
        prompts.add(haikuGen);

        // Example prompts for different prompt types

        // Static prompt example
        var staticExample = PromptInfo.builder()
                .name("static-example")
                .description("Example of a static prompt with predefined messages")
                .build();
        prompts.add(staticExample);

        // Multi-step workflow example
        var multiStepExample = PromptInfo.builder()
                .name("multi-step-example")
                .description("Example of a multi-step workflow with conversation history")
                .build();
        prompts.add(multiStepExample);

        // Dynamic prompt example
        var dynamicExample = PromptInfo.builder()
                .name("dynamic-example")
                .description("Example of a dynamic prompt with resource content")
                .arguments(List.of(
                        PromptArgument.builder()
                                .name("logs")
                                .description("Log content to analyze")
                                .required(false)
                                .build()))
                .build();
        prompts.add(dynamicExample);
        return prompts.stream().collect(Collectors.toMap(PromptInfo::getName, Function.identity()));
    }

    private static Map<String, Tool> createTools(List<Service> serviceList) {
        var tools = new ConcurrentHashMap<String, Tool>();
        for (Service service : serviceList) {
            var serviceName = service.schema().id().getName();
            for (var operation : service.getAllOperations()) {
                var operationName = operation.name();
                Schema schema = operation.getApiOperation().schema();
                var toolInfo = ToolInfo.builder()
                        .name(operationName)
                        .description(createDescription(serviceName,
                                operationName,
                                schema))
                        .inputSchema(createJsonObjectSchema(operation.getApiOperation().inputSchema(), new HashSet<>()))
                        .build();
                tools.put(operationName, new Tool(toolInfo, operation));
            }
        }
        return tools;
    }

    private static JsonObjectSchema createJsonObjectSchema(Schema schema, Set<ShapeId> visited) {
        var targetId = schema.id();
        if (!visited.add(targetId)) {
            // if we're in a recursive cycle, just say "type": "object" and bail
            return JsonObjectSchema.builder().build();
        }

        var properties = new HashMap<String, Document>();
        var requiredProperties = new ArrayList<String>();
        boolean isMember = schema.isMember();
        var members = isMember ? schema.memberTarget().members() : schema.members();
        var type = isMember ? schema.memberTarget().type() : schema.type();
        for (var member : members) {
            var name = member.memberName();
            if (member.hasTrait(TraitKey.REQUIRED_TRAIT)) {
                requiredProperties.add(name);
            }

            var jsonSchema = switch (member.type()) {
                case LIST, SET -> createJsonArraySchema(member.memberTarget(), visited);
                case MAP, STRUCTURE, UNION, DOCUMENT -> createJsonObjectSchema(member.memberTarget(), visited);
                default -> createJsonPrimitiveSchema(member);
            };

            properties.put(name, Document.of(jsonSchema));
        }

        visited.remove(targetId);
        var builder = JsonObjectSchema.builder()
                .properties(properties)
                .required(requiredProperties)
                .description(memberDescription(schema));
        if (type.isShapeType(ShapeType.DOCUMENT)) {
            builder.additionalProperties(true);
        }
        return builder.build();
    }

    private static JsonArraySchema createJsonArraySchema(Schema schema, Set<ShapeId> visited) {
        var listMember = schema.listMember();
        var items = switch (listMember.type()) {
            case LIST, SET -> createJsonArraySchema(listMember.memberTarget(), visited);
            case MAP, STRUCTURE, UNION -> createJsonObjectSchema(listMember.memberTarget(), visited);
            default -> createJsonPrimitiveSchema(listMember);
        };
        return JsonArraySchema.builder()
                .description(memberDescription(schema))
                .items(Document.of(items))
                .build();
    }

    private static JsonPrimitiveSchema createJsonPrimitiveSchema(Schema member) {
        var type = switch (member.type()) {
            case BYTE, SHORT, INTEGER, INT_ENUM, LONG, FLOAT, DOUBLE -> JsonPrimitiveType.NUMBER;
            case ENUM, BLOB, STRING, BIG_DECIMAL, BIG_INTEGER -> JsonPrimitiveType.STRING;
            case TIMESTAMP -> resolveTimestampType(member.memberTarget());
            case BOOLEAN -> JsonPrimitiveType.BOOLEAN;
            default -> throw new RuntimeException(member + " is not a primitive type");
        };

        return JsonPrimitiveSchema.builder()
                .typeMember(type)
                .description(memberDescription(member))
                .build();
    }

    private static String memberDescription(Schema schema) {
        String description = null;
        var trait = schema.getTrait(TraitKey.DOCUMENTATION_TRAIT);
        if (trait != null) {
            description = trait.getValue();
        }
        if (schema.isMember()) {
            var memberDescription = memberDescription(schema.memberTarget());
            if (description != null && memberDescription != null) {
                description = appendSentences(description, memberDescription);
            } else if (memberDescription != null) {
                description = memberDescription;
            }
        }
        return description;
    }

    private static JsonPrimitiveType resolveTimestampType(Schema schema) {
        var trait = schema.getTrait(TraitKey.TIMESTAMP_FORMAT_TRAIT);
        if (trait == null) {
            // default is epoch-seconds
            return JsonPrimitiveType.NUMBER;
        }
        return switch (trait.getFormat()) {
            case EPOCH_SECONDS -> JsonPrimitiveType.NUMBER;
            case DATE_TIME, HTTP_DATE -> JsonPrimitiveType.STRING;
            default -> throw new RuntimeException("unknown timestamp format: " + trait.getFormat());
        };
    }

    private static String createDescription(
            String serviceName,
            String operationName,
            Schema schema
    ) {
        var documentationTrait = schema.getTrait(TraitKey.DOCUMENTATION_TRAIT);
        if (documentationTrait != null) {
            return "This tool invokes %s API of %s.".formatted(operationName, serviceName) +
                    documentationTrait.getValue();
        } else {
            return "This tool invokes %s API of %s.".formatted(operationName, serviceName);
        }
    }

    @Override
    public void start() {
        // Start all proxies first
        for (McpServerProxy proxy : proxies) {
            proxy.start();
            proxy.initialize(this::writeResponse);

            // Get the tools from this proxy
            try {
                List<ToolInfo> proxyTools = proxy.listTools();

                // Add each tool to our tool map
                for (var toolInfo : proxyTools) {
                    tools.put(toolInfo.getName(), new Tool(toolInfo, proxy));
                }
            } catch (Exception e) {
                LOG.error("Failed to fetch tools from proxy", e);
            }
        }

        // Start the listener thread
        listener.start();
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        List<CompletableFuture<Void>> shutdownFutures = new ArrayList<>();

        // Shutdown all proxies
        for (McpServerProxy proxy : proxies) {
            shutdownFutures.add(proxy.shutdown());
        }

        // Wait for all to complete
        if (shutdownFutures.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        } else {
            return CompletableFuture.allOf(shutdownFutures.toArray(new CompletableFuture[0]));
        }
    }

    public void awaitCompletion() throws InterruptedException {
        done.await();
    }

    private PromptGetResult generatePromptResult(PromptInfo prompt, Document arguments) {
        // Create a basic prompt result with the prompt description
        var messages = new ArrayList<Message>();

        switch (prompt.getName()) {
            case "git-commit":
                messages.add(Message.builder()
                        .role("user")
                        .content(MessageContent.builder()
                                .typeMember("text")
                                .text("Generate a Git commit message for the following changes:\n\n" +
                                        (arguments != null && arguments.getMember("changes") != null
                                                ? arguments.getMember("changes").asString()
                                                : ""))
                                .build())
                        .build());
                break;

            case "explain-code":
                String codeLanguage = arguments != null && arguments.getMember("language") != null
                        ? " written in " + arguments.getMember("language").asString()
                        : "";
                String codeContent = arguments != null && arguments.getMember("code") != null
                        ? "```\n" + arguments.getMember("code").asString() + "\n```"
                        : "";

                messages.add(Message.builder()
                        .role("user")
                        .content(MessageContent.builder()
                                .typeMember("text")
                                .text("Please explain the following code" + codeLanguage + ":\n\n" + codeContent)
                                .build())
                        .build());
                break;

            case "pirate-talk":
                messages.add(Message.builder()
                        .role("user")
                        .content(MessageContent.builder()
                                .typeMember("text")
                                .text("Translate the following text into pirate speak:\n\n" +
                                        (arguments != null && arguments.getMember("text") != null
                                                ? arguments.getMember("text").asString()
                                                : ""))
                                .build())
                        .build());
                break;

            case "zombie-plan":
                String location = arguments != null && arguments.getMember("location") != null
                        ? arguments.getMember("location").asString()
                        : "an unknown location";
                String resources = arguments != null && arguments.getMember("resources") != null
                        ? " with these resources: " + arguments.getMember("resources").asString()
                        : " with limited resources";

                messages.add(Message.builder()
                        .role("user")
                        .content(MessageContent.builder()
                                .typeMember("text")
                                .text("Create a detailed zombie apocalypse survival plan for someone in " +
                                        location + resources)
                                .build())
                        .build());
                break;

            case "haiku":
                String topic = arguments != null && arguments.getMember("topic") != null
                        ? arguments.getMember("topic").asString()
                        : "nature";

                messages.add(Message.builder()
                        .role("user")
                        .content(MessageContent.builder()
                                .typeMember("text")
                                .text("Generate a haiku about " + topic)
                                .build())
                        .build());
                break;

            case "static-example":
                // Static prompt with predefined messages
                messages.add(Message.builder()
                        .role("user")
                        .content(MessageContent.builder()
                                .typeMember("text")
                                .text("What are the best practices for AWS Lambda functions?")
                                .build())
                        .build());
                break;

            case "multi-step-example":
                // Multi-step workflow with conversation history
                messages.add(Message.builder()
                        .role("user")
                        .content(MessageContent.builder()
                                .typeMember("text")
                                .text("I'm getting a timeout error in my Lambda function")
                                .build())
                        .build());

                messages.add(Message.builder()
                        .role("assistant")
                        .content(MessageContent.builder()
                                .typeMember("text")
                                .text("I'll help you troubleshoot this. What's the timeout setting for your Lambda?")
                                .build())
                        .build());

                messages.add(Message.builder()
                        .role("user")
                        .content(MessageContent.builder()
                                .typeMember("text")
                                .text("It's set to the default 3 seconds")
                                .build())
                        .build());
                break;

            case "dynamic-example":
                // Dynamic prompt that incorporates arguments and resources
                String resourceText = "No logs available";
                if (arguments != null && arguments.getMember("logs") != null) {
                    resourceText = arguments.getMember("logs").asString();
                }

                // User message with text
                messages.add(Message.builder()
                        .role("user")
                        .content(MessageContent.builder()
                                .typeMember("text")
                                .text("Analyze these logs for errors:")
                                .build())
                        .build());

                // Resource content message
                messages.add(Message.builder()
                        .role("user")
                        .content(MessageContent.builder()
                                .typeMember("resource")
                                .resource(McpResource.builder()
                                        .uri("logs://recent")
                                        .text(resourceText)
                                        .mimeType("text/plain")
                                        .build())
                                .build())
                        .build());
                break;

            default:
                messages.add(Message.builder()
                        .role("user")
                        .content(MessageContent.builder()
                                .typeMember("text")
                                .text("Please provide information about: " + prompt.getName())
                                .build())
                        .build());
        }

        return PromptGetResult.builder()
                .description(prompt.getDescription())
                .messages(messages)
                .build();
    }

    private String createPromptText(PromptInfo prompt, Document arguments) {
        // Simple implementation - just create a text prompt based on the prompt name and arguments
        StringBuilder sb = new StringBuilder();

        switch (prompt.getName()) {
            case "git-commit":
                sb.append("Generate a Git commit message for the following changes:\\n\\n");
                if (arguments != null && arguments.getMember("changes") != null) {
                    sb.append(arguments.getMember("changes").asString());
                }
                break;

            case "explain-code":
                sb.append("Please explain the following code");

                if (arguments != null && arguments.getMember("language") != null) {
                    sb.append(" written in ").append(arguments.getMember("language").asString());
                }

                sb.append(":\\n\\n");

                if (arguments != null && arguments.getMember("code") != null) {
                    sb.append("```\\n");
                    sb.append(arguments.getMember("code").asString());
                    sb.append("\\n```");
                }
                break;

            case "pirate-talk":
                sb.append("Translate the following text into pirate speak:\\n\\n");
                if (arguments != null && arguments.getMember("text") != null) {
                    sb.append(arguments.getMember("text").asString());
                }
                break;

            case "zombie-plan":
                sb.append("Create a detailed zombie apocalypse survival plan for someone in ");
                if (arguments != null && arguments.getMember("location") != null) {
                    sb.append(arguments.getMember("location").asString());
                } else {
                    sb.append("an unknown location");
                }

                if (arguments != null && arguments.getMember("resources") != null) {
                    sb.append(" with these resources: ").append(arguments.getMember("resources").asString());
                } else {
                    sb.append(" with limited resources");
                }
                break;

            case "haiku":
                sb.append("Generate a haiku about ");
                if (arguments != null && arguments.getMember("topic") != null) {
                    sb.append(arguments.getMember("topic").asString());
                } else {
                    sb.append("nature");
                }
                break;

            default:
                sb.append("Please provide information about: ").append(prompt.getName());
        }

        return sb.toString();
    }

    private record Prompt(String promptName, PromptInfo promptInfo, Message message) {}

    private record Tool(ToolInfo toolInfo, Operation operation, McpServerProxy proxy, boolean requiredAdapting) {

        Tool(ToolInfo toolInfo, Operation operation) {
            this(toolInfo, operation, null, false);
        }

        Tool(ToolInfo toolInfo, McpServerProxy proxy) {
            this(toolInfo, null, proxy, false);
        }
    }

    private static String appendSentences(String first, String second) {
        first = first.trim();
        if (!first.endsWith(".")) {
            first = first + ". ";
        }
        return first + second;
    }

    private static Document adaptDocument(Document doc, Schema schema) {
        var fromType = doc.type();
        var toType = schema.type();
        return switch (toType) {
            case BIG_DECIMAL -> switch (fromType) {
                case STRING -> Document.of(new BigDecimal(doc.asString()));
                case BIG_INTEGER -> doc;
                default -> badType(fromType, toType);
            };
            case BIG_INTEGER -> switch (fromType) {
                case STRING -> Document.of(new BigInteger(doc.asString()));
                case BIG_INTEGER -> doc;
                default -> badType(fromType, toType);
            };
            case BLOB -> switch (fromType) {
                case STRING -> Document.of(doc.asString().getBytes(StandardCharsets.UTF_8));
                case BLOB -> doc;
                default -> badType(fromType, toType);
            };
            case STRUCTURE, UNION -> {
                var convertedMembers = new HashMap<String, Document>();
                var members = schema.members();
                for (var member : members) {
                    var memberName = member.memberName();
                    var memberDoc = doc.getMember(memberName);
                    if (memberDoc != null) {
                        convertedMembers.put(memberName, adaptDocument(memberDoc, member.memberTarget()));
                    }
                }
                yield Document.of(convertedMembers);
            }
            case LIST, SET -> {
                var listMember = schema.listMember();
                var convertedList = new ArrayList<Document>();
                for (var item : doc.asList()) {
                    convertedList.add(adaptDocument(item, listMember.memberTarget()));
                }
                yield Document.of(convertedList);
            }
            case MAP -> {
                var mapValue = schema.mapValueMember();
                var convertedMap = new HashMap<String, Document>();
                for (var entry : doc.asStringMap().entrySet()) {
                    convertedMap.put(entry.getKey(), adaptDocument(entry.getValue(), mapValue.memberTarget()));
                }
                yield Document.of(convertedMap);
            }
            default -> doc;
        };
    }

    private static Document badType(ShapeType from, ShapeType to) {
        throw new RuntimeException("Cannot convert from " + from + " to " + to);
    }

    public static McpServerBuilder builder() {
        return new McpServerBuilder();
    }
}
