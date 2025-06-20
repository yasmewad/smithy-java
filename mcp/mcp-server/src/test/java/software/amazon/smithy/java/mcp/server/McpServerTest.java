/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.mcp.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.java.client.core.interceptors.ClientInterceptor;
import software.amazon.smithy.java.client.core.interceptors.InputHook;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.dynamicschemas.StructDocument;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.java.json.JsonSettings;
import software.amazon.smithy.java.mcp.model.JsonRpcRequest;
import software.amazon.smithy.java.mcp.model.JsonRpcResponse;
import software.amazon.smithy.java.server.ProxyService;
import software.amazon.smithy.java.server.Server;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;

public class McpServerTest {
    private static final JsonCodec CODEC = JsonCodec.builder()
            .settings(JsonSettings.builder()
                    .serializeTypeInDocuments(false)
                    .useJsonName(true)
                    .build())
            .build();

    private TestInputStream input;
    private TestOutputStream output;
    private Server server;
    private int id;

    @BeforeEach
    public void beforeEach() {
        input = new TestInputStream();
        output = new TestOutputStream();
    }

    @AfterEach
    public void afterEach() {
        if (server != null) {
            server.shutdown().join();
        }
    }

    @Test
    public void validateToolStructure() {
        server = McpServer.builder()
                .input(input)
                .output(output)
                .addService(ProxyService.builder()
                        .service(ShapeId.from("smithy.test#TestService"))
                        .proxyEndpoint("http://localhost")
                        .model(MODEL)
                        .build())
                .build();

        server.start();

        write("tools/list", Document.of(Map.of()));
        var response = read();
        var tools = response.getResult().asStringMap().get("tools").asList();

        var tool = tools.get(0).asStringMap();
        var inputSchema = tool.get("inputSchema").asStringMap();
        var properties = inputSchema.get("properties").asStringMap();

        assertEquals("TestOperation", tool.get("name").asString());
        assertEquals("This tool invokes TestOperation API of TestService.A TestOperation",
                tool.get("description").asString());
        assertEquals("object", inputSchema.get("type").asString());
        assertEquals("An input for TestOperation with a nested member",
                inputSchema.get("description").asString());

        var str = properties.get("str").asStringMap();
        assertEquals("string", str.get("type").asString());
        assertEquals("It's a string", str.get("description").asString());

        var list = properties.get("list").asStringMap();
        assertEquals("array", list.get("type").asString());

        var bigDecimal = properties.get("bigDecimalField").asStringMap();
        assertEquals("string", bigDecimal.get("type").asString());

        var listItems = list.get("items").asStringMap();
        assertEquals("object", listItems.get("type").asString());
        var listItemProperties = listItems.get("properties").asStringMap();
        validateNestedStructure(listItemProperties);

        var nested = properties.get("nested").asStringMap();
        assertEquals("object", nested.get("type").asString());
        var nestedProperties = nested.get("properties").asStringMap();
        validateNestedStructure(nestedProperties);

        var doubleNestedList = properties.get("doubleNestedList").asStringMap();
        assertEquals("array", doubleNestedList.get("type").asString());

        var doubleNestedListItems = doubleNestedList.get("items").asStringMap();
        assertEquals("array", doubleNestedListItems.get("type").asString());

        var doubleNestedListItemsItems = doubleNestedListItems.get("items").asStringMap();
        assertEquals("object", doubleNestedListItemsItems.get("type").asString());
        var doubleNestedProperties = doubleNestedListItemsItems.get("properties").asStringMap();
        validateNestedStructure(doubleNestedProperties);
    }

    @Test
    void testNumberAndStringIds() {
        server = McpServer.builder()
                .input(input)
                .output(output)
                .addService(ProxyService.builder()
                        .service(ShapeId.from("smithy.test#TestService"))
                        .proxyEndpoint("http://localhost")
                        .model(MODEL)
                        .build())
                .build();

        server.start();

        // Test with numeric ID
        write("tools/list", Document.of(Map.of()), Document.of(42));
        var response = read();
        assertEquals(42, response.getId().asNumber().intValue());
        assertNotNull(response.getResult());

        // Test with string ID
        write("tools/list", Document.of(Map.of()), Document.of("test-id-1"));
        response = read();
        assertEquals("test-id-1", response.getId().asString());
        assertNotNull(response.getResult());

        // Test sequence: number -> string -> number -> string
        write("tools/list", Document.of(Map.of()), Document.of(1));
        response = read();
        assertEquals(1, response.getId().asNumber().intValue());

        write("tools/list", Document.of(Map.of()), Document.of("mixed-test"));
        response = read();
        assertEquals("mixed-test", response.getId().asString());

        write("tools/list", Document.of(Map.of()), Document.of(999));
        response = read();
        assertEquals(999, response.getId().asNumber().intValue());

        write("tools/list", Document.of(Map.of()), Document.of("final-string-id"));
        response = read();
        assertEquals("final-string-id", response.getId().asString());
    }

    @Test
    void testInvalidIds() {
        server = McpServer.builder()
                .input(input)
                .output(output)
                .addService(ProxyService.builder()
                        .service(ShapeId.from("smithy.test#TestService"))
                        .proxyEndpoint("http://localhost")
                        .model(MODEL)
                        .build())
                .build();

        server.start();

        // Test with boolean ID (should fail)
        write("tools/list", Document.of(Map.of()), Document.of(true));
        var response = read();
        assertNotNull(response.getError());
        assertTrue(response.getError().getMessage().contains("Request id is of invalid type"));

        // Test with double ID (should fail)
        write("tools/list", Document.of(Map.of()), Document.of(3.14));
        response = read();
        assertNotNull(response.getError());
        assertTrue(response.getError().getMessage().contains("Request id is of invalid type"));

        // Test with array ID (should fail)
        write("tools/list",
                Document.of(Map.of()),
                Document.of(List.of(Document.of(1), Document.of(2), Document.of(3))));
        response = read();
        assertNotNull(response.getError());
        assertTrue(response.getError().getMessage().contains("Request id is of invalid type"));

        // Test with object ID (should fail)
        write("tools/list", Document.of(Map.of()), Document.of(Map.of("key", Document.of("value"))));
        response = read();
        assertNotNull(response.getError());
        assertTrue(response.getError().getMessage().contains("Request id is of invalid type"));
    }

    @Test
    void testRequestsRequireIds() {
        server = McpServer.builder()
                .input(input)
                .output(output)
                .addService(ProxyService.builder()
                        .service(ShapeId.from("smithy.test#TestService"))
                        .proxyEndpoint("http://localhost")
                        .model(MODEL)
                        .build())
                .build();

        server.start();

        // Test regular request without ID (should fail with specific message)
        write("tools/list", Document.of(Map.of()), null);
        var response = read();
        assertNotNull(response.getError());
        assertTrue(response.getError().getMessage().contains("Requests are expected to have ids"));
    }

    @Test
    void testInputAdaptation() {
        AtomicReference<StructDocument> capturedInput = new AtomicReference<>();
        server = McpServer.builder()
                .input(input)
                .output(output)
                .addService(ProxyService.builder()
                        .service(ShapeId.from("smithy.test#TestService"))
                        .proxyEndpoint("http://localhost")
                        .clientConfigurator(
                                clientConfigurator -> clientConfigurator.addInterceptor(new ClientInterceptor() {
                                    @Override
                                    public void readBeforeSerialization(InputHook<?, ?> hook) {
                                        capturedInput.set((StructDocument) hook.input());
                                    }
                                }))
                        .model(MODEL)
                        .build())
                .build();

        server.start();

        var bigDecimalValue = BigDecimal.valueOf(Integer.MAX_VALUE).add(BigDecimal.TEN);
        var bigIntegerValue = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.valueOf(100));
        var blobValue = "Hello, World!";
        var nestedBigDecimalValue = new BigDecimal("123.456");
        var nestedBigIntegerValue = new BigInteger("9876543210");
        var nestedBlobValue = "Nested blob content";

        write("tools/call",
                Document.of(
                        Map.of("name",
                                Document.of("TestOperation"),
                                "arguments",
                                Document.of(Map.of(
                                        "bigDecimalField",
                                        Document.of(bigDecimalValue.toString()),
                                        "bigIntegerField",
                                        Document.of(bigIntegerValue.toString()),
                                        "blobField",
                                        Document.of(blobValue),
                                        "nestedWithBigNumbers",
                                        Document.of(Map.of(
                                                "nestedBigDecimal",
                                                Document.of(nestedBigDecimalValue.toString()),
                                                "nestedBigInteger",
                                                Document.of(nestedBigIntegerValue.toString()),
                                                "nestedBlob",
                                                Document.of(nestedBlobValue),
                                                "bigDecimalList",
                                                Document.of(List.of(
                                                        Document.of("100.25"),
                                                        Document.of("200.75"))))))))));
        assertNotNull(read());
        var inputDocument = capturedInput.get();

        var bigDecimalField = inputDocument.getMember("bigDecimalField");
        assertNotNull(bigDecimalField);
        assertEquals(ShapeType.BIG_DECIMAL, bigDecimalField.type());
        assertEquals(bigDecimalValue, bigDecimalField.asBigDecimal());

        var bigIntegerField = inputDocument.getMember("bigIntegerField");
        assertNotNull(bigIntegerField);
        assertEquals(ShapeType.BIG_INTEGER, bigIntegerField.type());
        assertEquals(bigIntegerValue, bigIntegerField.asBigInteger());

        var blobField = inputDocument.getMember("blobField");
        assertNotNull(blobField);
        assertEquals(ShapeType.BLOB, blobField.type());
        assertEquals(blobValue, new String(blobField.asBlob().array(), StandardCharsets.UTF_8));

        var nestedWithBigNumbers = inputDocument.getMember("nestedWithBigNumbers");
        assertNotNull(nestedWithBigNumbers);
        assertEquals(ShapeType.STRUCTURE, nestedWithBigNumbers.type());

        var nestedStruct = (StructDocument) nestedWithBigNumbers;

        var nestedBigDecimalField = nestedStruct.getMember("nestedBigDecimal");
        assertNotNull(nestedBigDecimalField);
        assertEquals(ShapeType.BIG_DECIMAL, nestedBigDecimalField.type());
        assertEquals(nestedBigDecimalValue, nestedBigDecimalField.asBigDecimal());

        var nestedBigIntegerField = nestedStruct.getMember("nestedBigInteger");
        assertNotNull(nestedBigIntegerField);
        assertEquals(ShapeType.BIG_INTEGER, nestedBigIntegerField.type());
        assertEquals(nestedBigIntegerValue, nestedBigIntegerField.asBigInteger());

        var nestedBlobField = nestedStruct.getMember("nestedBlob");
        assertNotNull(nestedBlobField);
        assertEquals(ShapeType.BLOB, nestedBlobField.type());
        assertEquals(nestedBlobValue, new String(nestedBlobField.asBlob().array(), StandardCharsets.UTF_8));

        var bigDecimalListField = nestedStruct.getMember("bigDecimalList");
        assertNotNull(bigDecimalListField);
        assertEquals(ShapeType.LIST, bigDecimalListField.type());
        var bigDecimalList = bigDecimalListField.asList();
        assertEquals(2, bigDecimalList.size());
        assertEquals(ShapeType.BIG_DECIMAL, bigDecimalList.get(0).type());
        assertEquals(ShapeType.BIG_DECIMAL, bigDecimalList.get(1).type());
        assertEquals(new BigDecimal("100.25"), bigDecimalList.get(0).asBigDecimal());
        assertEquals(new BigDecimal("200.75"), bigDecimalList.get(1).asBigDecimal());

        server.shutdown().join();
    }

    @Test
    void testNotificationsDoNotRequireRequestId() {
        server = McpServer.builder()
                .input(input)
                .output(output)
                .addService(ProxyService.builder()
                        .service(ShapeId.from("smithy.test#TestService"))
                        .proxyEndpoint("http://localhost")
                        .model(MODEL)
                        .build())
                .build();

        server.start();

        // Test notifications/initialized without ID (should not produce any error response)
        writeNotification("notifications/initialized", Document.of(Map.of()));
        output.assertNoOutput();

        // Test another notification to ensure it's consistently handled
        writeNotification("notifications/progress", Document.of(Map.of("progressToken", Document.of("test"))));
        output.assertNoOutput();

        // Send a regular request to verify the server is still functioning
        write("tools/list", Document.of(Map.of()));
        var response = read();
        assertNotNull(response.getResult());
    }

    private void validateNestedStructure(Map<String, Document> properties) {
        var nestedStr = properties.get("nestedStr").asStringMap();
        assertEquals("string", nestedStr.get("type").asString());
        assertEquals("A string that's nested", nestedStr.get("description").asString());

        var nestedDocument = properties.get("nestedDocument").asStringMap();
        assertTrue(nestedDocument.get("additionalProperties").asBoolean());
        assertTrue(nestedDocument.get("properties").asStringMap().isEmpty());
        assertTrue(nestedDocument.get("required").asList().isEmpty());

        var recursive = properties.get("recursive").asStringMap();
        assertEquals("object", recursive.get("type").asString());
        assertEquals("A structure that references itself recursively",
                recursive.get("description").asString());
        var recursiveProperties = recursive.get("properties").asStringMap();
        assertTrue(recursiveProperties.containsKey("nested"));
    }

    private void write(String method, Document document) {
        write(method, document, Document.of(id++));
    }

    private void write(String method, Document document, Document requestId) {
        var request = JsonRpcRequest.builder()
                .id(requestId)
                .method(method)
                .params(document)
                .jsonrpc("2.0")
                .build();
        input.write(CODEC.serializeToString(request));
        input.write("\n");
    }

    private JsonRpcResponse read() {
        var line = output.read();
        System.out.println(line);
        return CODEC.deserializeShape(line, JsonRpcResponse.builder());
    }

    private void writeNotification(String method, Document params) {
        var request = JsonRpcRequest.builder()
                .method(method)
                .params(params)
                .jsonrpc("2.0")
                .build();
        input.write(CODEC.serializeToString(request));
        input.write("\n");
    }

    private static final String MODEL_STR = """
            $version: "2"

            namespace smithy.test

            /// A TestService
            @aws.protocols#awsJson1_0
            service TestService {
                operations: [TestOperation]
            }

            /// A TestOperation
            operation TestOperation {
                input: TestInput
                output: TestInput
            }

            /// An input for TestOperation with a nested member
            structure TestInput {
                /// It's a string
                str: String

                /// The nested member
                nested: Nested

                list: NestedList

                doubleNestedList: DoubleNestedList

                bigDecimalField: BigDecimal

                bigIntegerField: BigInteger

                blobField: Blob

                nestedWithBigNumbers: NestedWithBigNumbers
            }

            list NestedList {
                member: Nested
            }

            list DoubleNestedList {
                member: NestedList
            }

            /// A structure that can be nested
            structure Nested {
                /// A string that's nested
                nestedStr: String

                /// A document that's nested
                nestedDocument: Document

                /// A field that recurses back into us
                recursive: Recursive
            }

            /// A structure that references itself recursively
            structure Recursive {
                /// the nested field that points back to us
                nested: Nested
            }

            /// A structure containing big number types
            structure NestedWithBigNumbers {
                /// A nested BigDecimal
                nestedBigDecimal: BigDecimal

                /// A nested BigInteger
                nestedBigInteger: BigInteger

                /// A nested Blob
                nestedBlob: Blob

                /// A list of BigDecimals
                bigDecimalList: BigDecimalList
            }

            list BigDecimalList {
                member: BigDecimal
            }""";

    private static final Model MODEL = Model.assembler()
            .addUnparsedModel("test.smithy", MODEL_STR)
            .discoverModels()
            .assemble()
            .unwrap();
}
