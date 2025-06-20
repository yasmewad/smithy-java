$version: "2"

namespace smithy.mcp

structure JsonRpcRequest {
    @required
    jsonrpc: String

    @required
    method: String

    id: Document

    params: Document
}

structure JsonRpcResponse {
    @required
    jsonrpc: String

    result: Document

    error: JsonRpcErrorResponse

    id: Document
}

structure JsonRpcErrorResponse {
    @required
    code: Integer = 0

    message: String

    data: Document
}

@trait(selector: "service")
structure jsonRpc2 {}

@trait(selector: "operation")
structure jsonRpc2Method {
    @required
    method: NonEmptyString
}

@private
@length(min: 1)
string NonEmptyString

@mixin
structure BaseResult {
    @default("2024-11-05")
    protocolVersion: String
}

structure InitializeResult with [BaseResult] {
    @required
    capabilities: Capabilities

    @required
    serverInfo: ServerInfo
}

structure Capabilities {
    logging: Document
    prompts: Prompts
    tools: Tools
}

structure Prompts {
    @default(false)
    listChanged: Boolean
}

structure Resources {
    @default(false)
    subscribe: Boolean
}

structure Tools {
    @default(false)
    listChanged: Boolean
}

structure ServerInfo {
    @required
    name: String

    @required
    version: String
}

structure ListToolsResult {
    tools: ToolInfoList
}

structure ToolInfo {
    @required
    name: String

    description: String

    inputSchema: JsonObjectSchema
}

list ToolInfoList {
    member: ToolInfo
}

structure PromptsResult {
    prompts: PromptList
}

list PromptList {
    member: PromptInfo
}

structure PromptArgument {
    @required
    name: String

    description: String

    required: Boolean
}

list PromptArgumentList {
    member: PromptArgument
}

structure PromptInfo {
    @required
    name: String

    description: String

    arguments: PromptArgumentList
}

structure JsonObjectSchema {
    @required
    type: String = "object"

    properties: PropertiesMap

    required: StringList

    additionalProperties: Boolean

    description: String

    @jsonName("$schema")
    schema: String = "http://json-schema.org/draft-07/schema#"
}

structure JsonArraySchema {
    @required
    type: String = "array"

    /// one of JsonObjectSchema | JsonArraySchema | JsonPrimitiveSchema
    @required
    items: Document

    uniqueItems: PrimitiveBoolean = false

    description: String

    default: Document
}

structure JsonPrimitiveSchema {
    @required
    type: JsonPrimitiveType

    description: String
}

enum JsonPrimitiveType {
    NUMBER = "number"
    STRING = "string"
    BOOLEAN = "boolean"
    NULL = "null"
}

map PropertiesMap {
    key: String

    /// one of JsonObjectSchema | JsonArraySchema | JsonPrimitiveSchema
    value: Document
}

list StringList {
    member: String
}

structure CallToolResult {
    // TODO add others
    content: TextContentList

    @default(false)
    isError: Boolean
}

list TextContentList {
    member: TextContent
}

structure TextContent {
    @required
    @default("text")
    type: String

    text: String
}

structure McpResource {
    uri: String
    text: String
    mimeType: String
}

// Updated MessageContent structure to match the MCP specification
structure MessageContent {
    @required
    type: String = "text"

    text: String

    resource: McpResource
}

structure Message {
    @required
    role: String

    @required
    content: MessageContent
}

list MessageList {
    member: Message
}

structure PromptGetResult {
    description: String
    messages: MessageList
}
