//This ensures composite builds also have the repositories configured.
pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "smithy-java"

// Common modules
include(":context")
include(":core")
include(":dynamic-schemas")
include(":io")
include(":logging")

// CLI
include(":cli")

// Common components
include(":auth-api")
include(":framework-errors")
include(":http:http-api")
include(":http:http-binding")
include(":retries-api")
include(":tracing-api")


// Codecs
include(":codecs:cbor-codec")
include(":codecs:json-codec")
include(":codecs:xml-codec")

// Client
include(":client:client-core")
include(":client:client-auth-api")
include(":client:client-http")
include(":client:client-http-binding")
include(":client:client-rpcv2-cbor")
include(":client:dynamic-client")
include(":client:client-mock-plugin")
include(":client:client-waiters")

// Server
include(":server:server-api")
include(":server:server-core")
include(":server:server-netty")
include(":server:server-rpcv2-cbor")
include(":server:server-proxy")

// Codegen
include(":codegen:codegen-core")
include(":codegen:integrations:waiters-codegen")
include(":codegen:plugins")
include(":codegen:plugins:client-codegen")
include(":codegen:plugins:server-codegen")
include(":codegen:plugins:types-codegen")

// Utilities
include(":jmespath")
include(":protocol-test-harness")

// AWS specific
include(":aws:aws-event-streams")
include(":aws:aws-sigv4")
include(":aws:client:aws-client-awsjson")
include(":aws:client:aws-client-core")
include(":aws:client:aws-client-http")
include(":aws:client:aws-client-restjson")
include(":aws:client:aws-client-restxml")
include(":aws:integrations:aws-lambda-endpoint")
include(":aws:server:aws-server-restjson")
include(":aws:aws-auth-api")

// AWS service bundling code
include(":aws:aws-service-bundle")
include(":aws:aws-service-bundler")
include(":aws:aws-mcp-provider")
include(":aws:aws-mcp-types")
include(":aws:aws-mcp-cli-commands")

// AWS SDK V2 shims
include(":aws:sdkv2:aws-sdkv2-retries")
include(":aws:sdkv2:aws-sdkv2-shapes")
include(":aws:sdkv2:aws-sdkv2-auth")

// Examples
include(":examples")
include(":examples:basic-server")
include(":examples:dynamodb-client")
include(":examples:end-to-end")
include(":examples:event-streaming-client")
include(":examples:lambda")
include(":examples:restjson-client")
include(":examples:standalone-types")
include(":examples:mcp-server")
include(":examples:mcp-traits-example")

//MCP
include(":mcp")
include(":mcp:mcp-schemas")
include(":mcp:mcp-server")
include(":mcp:mcp-cli")
include(":mcp:mcp-cli-api")
include(":mcp:mcp-bundle-api")
include(":mcp:mcp-traits")

include(":model-bundle")
include(":model-bundle:model-bundle-api")