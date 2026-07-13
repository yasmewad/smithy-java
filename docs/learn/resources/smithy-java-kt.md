# Smithy Java, Knowledge Transfer

> Repository: https://github.com/smithy-lang/smithy-java
> Source: internal Pippin artifact smithy_musings/Y84gPFVTqbKJ (authors: sugmanue, yefrig).
> Stashed verbatim for offline grounding of the protocol learning track.

## Getting Started

### Prerequisites
- **JDK 21+** — virtual threads, sealed classes, and records are used pervasively; earlier versions will not compile.
- **Gradle wrapper** — no local Gradle installation needed; use `./gradlew`.

### Build
```bash
./gradlew build           # compile + test everything
./gradlew assemble        # compile only, skip tests
./gradlew :context:build  # build a single module
```

### Run Tests
```bash
./gradlew test                          # all unit tests across all modules
./gradlew :protocol-test-harness:test   # protocol compliance tests (restJson1, restXml, awsJson, awsQuery, rpcv2-cbor)
./gradlew :codecs:test                  # tests for a specific module
./gradlew :module:test --tests "*Foo*"  # single test class by name pattern
```
**Protocol compliance tests are the authoritative correctness check.** Run them before and after any change touching serde, codecs, HTTP binding, or protocol integrations. A passing unit test that fails a compliance test is a bug.

### Local SNAPSHOT
```bash
./gradlew publishToMavenLocal
# Artifacts published as software.amazon.smithy.java:<module>:1.0.0-SNAPSHOT to ~/.m2/repository
```

## Suggested Reading Order (tiers by dependency)

### Tier 1, Foundations (no inter-dependencies)
1. Context System — used everywhere; understand first
2. Schemas — the runtime data model; foundational for serde
3. Documents — untyped data representation
4. Logging — auto-adapting logger abstraction

### Tier 2, I/O & Serialization (depends on Tier 1)
5. I/O and Streaming — binary/event stream abstractions
6. Codecs — JSON, CBOR, XML serialization engines

### Tier 3, Client Infrastructure (depends on Tiers 1–2)
7. HTTP Layer — transport and HTTP binding
8. Auth System — identity, signing, scheme resolution
9. Client Core Architecture — plugin/interceptor/pipeline
10. Retries and Waiters — resilience patterns

### Tier 4, Protocols & Specialized Clients (depends on Tier 3)
11. AWS Protocol Integrations — restJson, restXml, awsJson, awsQuery
12. Rules Engine — endpoint resolution via BDD + VM
13. Dynamic Client — runtime client without codegen

### Tier 5, Server & Generation (depends on all above)
14. Server — handler pipeline, Netty, protocol SPI
15. MCP — Model Context Protocol for AI/LLM tools
16. Code Generation — plugin architecture, writers, integrations

## Repository Structure (multi-module Gradle; each module is a published JAR)

| Module | Description | Key packages |
|-|-|-|
| `context/` | Typed key-value pipeline context | `software.amazon.smithy.java.context` |
| `core/` | Schemas, serde interfaces, documents, traits | `...core.schema`, `...core.serde` |
| `logging/` | Framework-agnostic internal logger | `software.amazon.smithy.java.logging` |
| `io/` | `DataStream`, `EventStream`, event codec pipeline | `software.amazon.smithy.java.io` |
| `codecs/` | JSON, CBOR, XML codec implementations | `software.amazon.smithy.java.codecs` |
| `http/` | `HttpRequest`, `HttpResponse`, `HttpHeaders` | `software.amazon.smithy.java.http` |
| `client/client-http/` | HTTP client transport | `...client.http` |
| `client/client-http-binding/` | HTTP trait → request/response mapping | `...client.http.binding` |
| `auth-api/` | `Identity`, `Signer`, `AuthScheme` interfaces | `software.amazon.smithy.java.auth.api` |
| `client/client-auth-api/` | Client-side auth resolution | `...client.auth` |
| `aws/aws-sigv4/` | SigV4 signing implementation | `software.amazon.smithy.java.aws.sigv4` |
| `client/client-core/` | Plugin system, interceptors, `ClientPipeline` | `...client.core` |
| `retries-api/` | `RetryStrategy`, `RetryToken` interfaces | `software.amazon.smithy.java.retries.api` |
| `retries/` | Standard and Adaptive retry strategies | `software.amazon.smithy.java.retries` |
| `client/client-waiters/` | `Waiter`, `Acceptor`, `Matcher` | `...client.waiters` |
| `aws/client/` | AWS protocol implementations (restJson1, restXml, awsJson, awsQuery) | `...aws.client` |
| `rulesengine/` | BDD + VM endpoint resolver | `software.amazon.smithy.java.rulesengine` |
| `client/dynamic-client/` | Runtime client from Smithy `Model` | `...client.dynamic` |
| `dynamic-schemas/` | `SchemaConverter`, `StructDocument` | `...dynamic.schemas` |
| `server/` | Server handler pipeline, Netty integration | `software.amazon.smithy.java.server` |
| `mcp/` | MCP server transports, interceptor pipeline | `software.amazon.smithy.java.mcp` |
| `smithy-ai-traits/` | AI annotation traits | `...smithy.ai.traits` |
| `codegen/` | `JavaCodegenPlugin`, writers, integrations | `software.amazon.smithy.java.codegen` |
| `protocol-test-harness/` | Smithy compliance test runner (not published) | `...protocol.tests` |

**Dependency direction rule:** dependencies flow upward through the tier list. `core` must not depend on `codecs`; `codecs` must not depend on `client-core`. Violations break the SPI composability model.

## Non-Obvious Design Decisions

### 1. Correctness First
- **Protocol compliance tests** (`protocol-test-harness/`) run Smithy-defined `@httpRequestTests` / `@httpResponseTests` against real client/server code. 14+ test suites cover rpcv2-cbor, restJson1, restXml, awsJson, awsQuery for both client and server.
- **Schema-driven validation** — `Validator` reuses the same `serializeMembers()` path as serialization, guaranteeing consistency between what gets validated and what hits the wire.
- **`PresenceTracker`** enforces `@required` at build time (generated builders call `tracker.validate()` before `build()` returns).
- **Constraint traits** (`@range`, `@length`, `@pattern`, `@uniqueItems`) are precomputed into the `Schema` and checked at runtime. Extensible via `CustomConstraint` SPI.
- **Client error correction** follows the spec; generated `errorCorrection()` methods provide safe defaults for missing required members during deserialization.

### 2. Performance by Design
Nothing off-the-shelf on the hot path by default. Custom JSON parser on `byte[]`, SWAR string scanning (8 bytes/iteration), custom Schubfach float→decimal, CBOR uses `VarHandle`. Speculative field-name lookup (schema-order fast path via `Arrays.equals` ~2ns, FNV-1a hash fallback). Lock-free striped object pools (`AtomicReferenceArray`, virtual-thread-aware). O(1) identity-based lookup everywhere (`Context.Key`→int ID→array index, `Schema.memberIndex()`→dense int for array dispatch). Rules Engine compiles endpoint rules to BDD + bytecode VM.

### 3. Built for Modern Java (21+)
Virtual threads first-class: client HTTP transport defaults to `Executors.newVirtualThreadPerTaskExecutor()`; codec pooling detects `Thread.currentThread().isVirtual()` and skips VT pooling; almost no `synchronized` (uses `ReentrantLock`/`StampedLock` to avoid pinning VTs). Sealed classes/interfaces (~49), records (~245), pattern matching with guards, `instanceof` patterns (283), switch expressions (~135), SequencedCollections.

### 4. Strict Boundaries
| Mechanism | Purpose | Example |
|-|-|-|
| Package-private classes | Hard compile-time enforcement | `ChunkedArrayStorageContext`, `ClientPipeline`, `SmithyJsonSerializer`, all `Schema` subclasses |
| Sealed types | Restrict type hierarchy extension | `Context` (2 permits), `Schema` (5 permits), `PresenceTracker` (3 permits) |
| `@SmithyInternalApi` | "Public but don't use" signal (68+ classes) | `Client.Builder`, `InjectIdempotencyTokenPlugin`, `CborSchemaExtensions` |
| `@SmithyUnstableApi` | "API may change" for preview features | All server/MCP packages |
| Gradle `internal` source set | Build-time code excluded from published JAR | Codegen internal plugins |
| Shadow/relocation | Hide transitive deps | Jackson → `smithy.java.internal.shaded.tools.jackson.core` |

### 5. Additional Principles
- **SPI composability** — `ServiceLoader` is the universal extension mechanism. Protocols, transports, codecs, auth schemes, schema extensions, codegen integrations, and auto-plugins are all discovered at runtime via SPI. Adding behavior never requires modifying existing code.
- **Push-based serialization** — No reflection. Generated types implement `serializeMembers(ShapeSerializer)` and push their own fields into the serializer. Protocols maintain wire-format state naturally while the shape controls traversal order.
- **Lazy evaluation** — `LazyStructure` defers document materialization; `DeferredRootSchema` defers recursive member resolution; schema extensions compute lazily with a benign-race pattern.
- **Protocol agnosticism via `MessageExchange`** — Protocols and transports are independently pluggable. They declare a `MessageExchange<Req, Resp>` and the pipeline validates compatibility at construction. Switching protocols at runtime requires only a config change.

## Contributing

### Engineering Tenets (higher-numbered yield to lower-numbered)
1. **Correctness.** No silent data loss, no wrong serialization, no violated contracts. If unsure, fail loudly.
2. **Safety.** Do not break existing users. Public API backward compatibility is non-negotiable. Additive changes only.
3. **Performance.** Minimize allocations, avoid unnecessary work, prefer O(1) over O(n). Library on the hot path.
4. **Simplicity.** Fewer concepts, fewer abstractions, fewer moving parts. No indirection without a concrete problem today.
5. **Explicitness.** Behavior obvious from the code. No magic, no implicit registration, no action-at-a-distance.
6. **Readability.** Code read more than written, but never trumps correctness or performance in library code.

### Code Conventions
| Rule | Rationale |
|-|-|
| No Java streams in production code | Lambda capture + iterator allocation overhead; uniform to avoid "is this hot?" debates. (Tenet 3 > 6) |
| No `synchronized` — use `ReentrantLock`/`StampedLock` | `synchronized` pins virtual threads. Only ~5 legacy blocks remain; add no new ones. (Tenet 3) |
| Pre-size collections when size known | Avoids rehash allocation. `new ArrayList<>(schema.members().size())`. (Tenet 3) |
| No `Optional` as field or parameter — return types only | Allocation + obscures intent as a parameter. (Tenet 5) |
| `@SmithyInternalApi` for public-but-internal classes | Signals "don't use" without package-private (which breaks inter-module use). (Tenet 2) |
| Never add dependencies from `core` toward protocol modules | Breaks SPI composability; protocols must be independently pluggable. |
| Always update schema extensions when adding trait behavior | Protocol codecs read precomputed extension data, not traits at runtime. Adding a trait without updating its `SchemaExtensionProvider` silently has no wire effect. |
| Register new SPIs in `META-INF/services/` | `ServiceLoader` is the discovery mechanism. Missing registration = feature silently absent. |

### Common Workflows

**Adding a protocol compliance test:** Drop a Smithy model file and a matching expected-output file in `protocol-test-harness/`. No Java code needed. `./gradlew :protocol-test-harness:test` runs them.

**Adding a new HTTP-bound trait:**
1. Add the trait to the relevant `Schema` extension in `HttpBindingSchemaExtensions`
2. Map it to a `MemberBinding` location (HEADER / QUERY / LABEL / PAYLOAD / BODY)
3. Add a protocol compliance test to verify the wire format
4. Run `./gradlew :protocol-test-harness:test`

**Adding a new codec:**
1. Implement `Codec`, `ShapeSerializer`, `ShapeDeserializer` in a new module
2. Add a `SchemaExtensionProvider` to precompute any trait-derived wire data
3. Register the codec via `ServiceLoader` in `META-INF/services/`
4. Add protocol compliance tests

**Debugging a serialization bug:** Attach a logging interceptor; the `readBeforeSigning` hook exposes the raw `HttpRequest` — fastest way to see what hits the wire without a proxy.

### Known Rough Edges
| Area | Notes |
|-|-|
| Server (`@SmithyUnstableApi`) | Public API may change. Do not treat server interfaces as stable. |
| MCP (`@SmithyUnstableApi`) | Actively evolving. Transport and interceptor APIs likely to change. |
| XML codec | Less battle-tested than JSON/CBOR. Thinner compliance coverage. |
| `synchronized` blocks | ~5 remain (key registration, output stream writes). Candidates for `ReentrantLock` migration. |
| AWS auth | Upcoming refactor of credential resolution; SEP-compliant authorization for AWS services. |

---

## 1. Context System
**Source:** `context/`
A typed key-value store threaded through the entire pipeline. Keys are `static final` constants with globally-assigned integer IDs → O(1) array-indexed access (no hashing).
```java
static final Context.Key<String> REGION = Context.key("Region");
static final Context.Key<Set<String>> FLAGS = Context.key("Flags", HashSet::new); // mutable: needs copy fn
var ctx = Context.create();
ctx.put(REGION, "us-west-2");
String r = ctx.expect(REGION); // throws NPE if absent
```
**Storage:** `ChunkedArrayStorageContext` uses `Object[][]` chunks of 32. Key ID → chunk index via bit-shift. ~2× faster than HashMap for gets, ~4.5× for copies. Sealed interface (2 impls). Copy semantics per-key. Not thread-safe.

## 2. Schemas & Schema-Directed Serde
**Source:** `core/.../schema/`
A Schema is a trimmed-down runtime representation of a Smithy shape — just enough to drive serialization/deserialization. This decouples protocols from generated code, enabling runtime protocol switching.
```java
var schema = Schema.structureBuilder(ShapeId.from("com.example#Foo"))
    .putMember("name", Schema.createString(ShapeId.from("com.example#Foo$name")), RequiredTrait.INSTANCE)
    .putMember("age", Schema.createInteger(ShapeId.from("com.example#Foo$age")))
    .build();
```
**Sealed hierarchy:** `RootSchema`, `MemberSchema`, `DeferredRootSchema`, `DeferredMemberSchema`, `ResolvedRootSchema`. Lists/Maps enforce member names (`"member"`; `"key"`+`"value"`). Recursive schemas pass `SchemaBuilder` → `DeferredRootSchema` resolving lazily. `PresenceTracker` validates `@required` via bitfield (a `long` ≤64 members, else `BitSet`).

### Schema-Directed Serialization
Core innovation: **Schema is the first argument to every serde method.** Generated code passes member schemas; protocol implementations read them to decide *how* to encode/decode — without the two knowing about each other.
```java
public interface ShapeSerializer {
    void writeString(Schema schema, String value);
    void writeTimestamp(Schema schema, Instant value);
    void writeStruct(Schema schema, SerializableStruct struct);
    <T> void writeList(Schema schema, T state, int size, BiConsumer<T, ShapeSerializer> consumer);
}
```
Generated code (the "push" side) pushes members with their schemas via `serializeMembers(ShapeSerializer)`. Protocol codec (the "interpret" side) reads the schema extension to decide wire format (JSON field-name bytes, timestamp format, etc.).
**Critical:** Schema traits are **never inspected at write-time**. All protocol decisions are precomputed into extensions (byte arrays, formatters, lookup tables) at Schema construction. At serialization: `currentFieldNameTable[memberIndex]` (O(1)) then `System.arraycopy`. No hashing, no trait lookups on the hot path.

### Schema-Directed Deserialization (Callback Pattern)
```java
<T> void readStruct(Schema schema, T state, StructMemberConsumer<T> consumer);
@FunctionalInterface interface StructMemberConsumer<T> { void accept(T state, Schema memberSchema, ShapeDeserializer memberDeserializer); }
```
Generated deserialization uses `switch (member.memberIndex())` compiled to a JVM tableswitch (no string comparisons). Field-name resolution: speculative fast path (schema-order `Arrays.equals` ~2ns) → FNV-1a hash fallback.

### Extension System (Protocol-Specific Precomputation)
Attaches precomputed data to schemas via SPI — each protocol computes what it needs once at class-load.
```java
public static final SchemaExtensionKey<SmithyJsonSchemaExt> KEY = new SchemaExtensionKey<>();
public class SmithyJsonSchemaExtensions implements SchemaExtensionProvider<SmithyJsonSchemaExt> {
    public SmithyJsonSchemaExt provide(Schema schema) { /* precompute UTF-8 field-name bytes, lookup tables, formatters from traits */ }
}
SmithyJsonSchemaExt ext = schema.getExtension(KEY); // lazy, lock-free, benign-race
```
| Extension | Precomputes | Used By |
|-|-|-|
| `SmithyJsonSchemaExtensions` | `byte[][]` field name tables (jsonName + memberName), `SmithyMemberLookup` | Native JSON codec |
| `CborSchemaExtensions` | `byte[][]` CBOR-encoded field names, `CborMemberLookup` | CBOR codec |
| `HttpBindingSchemaExtensions` | `MemberBinding` (HEADER/QUERY/LABEL/BODY), wire names, timestamp formats | HTTP binding layer |
**The result:** The same generated `serializeMembers()` works unchanged with JSON, CBOR, XML, or any future protocol. Switching protocols at runtime requires only a config/dependency change — no regeneration.

### Polymorphic Error Deserialization
Errors are polymorphic. Wire format includes a discriminator (`__type` in JSON, `x-amzn-errortype` header). Flow: detect non-2xx → header check (fast) → payload fallback (`__type`/`code`) → `TypeRegistry` lookup (populated at codegen with each error's builder supplier) → schema-directed deserialization into the matched error builder. AWS JSON 1.0 preserves namespace in `__type`; 1.1 and restJson strip it, but TypeRegistry lookup + schema-directed deserialization are shared.

## 3. Documents
**Source:** `core/.../serde/document/`
Documents represent the Smithy `document` type — any value without a fixed schema (like JSON's `Value`). `Document.of(...)`, `.type()`, `.asInteger()`, `.asStringMap()`, `.getMember("k")`, `.asShape(MyShape.builder())`. `LazyStructure` wraps a `SerializableStruct` and materializes lazily. Two-level serialization: `doc.serialize(serializer)` → `serializer.writeDocument(schema, doc)` → `doc.serializeContents(serializer)`.

## 4. I/O and Streaming
**Source:** `io/`, `core/.../serde/event/`
**DataStream** — a `Flow.Publisher<ByteBuffer>` (`ofBytes`, `ofInputStream`, `ofFile`). **EventStream** — sealed interface for typed bidirectional streaming (`@streaming` unions). Event codec pipeline: `EventEncoder`→`FrameEncoder` (write); `FrameDecoder`→`EventDecoder` (read). Composable via `FrameProcessor` (signing, compression).

## 5. Codecs
**Source:** `codecs/`
Codecs are the bridge between structures (which carry schemas) and bytes. A `Codec` produces `ShapeSerializer`/`ShapeDeserializer` (the protocol-specific implementations of the serde visitor interfaces from `core`).
```java
public interface Codec extends AutoCloseable {
    ShapeSerializer createSerializer(OutputStream sink);
    ShapeDeserializer createDeserializer(ByteBuffer source);
    default ByteBuffer serialize(SerializableShape shape) { ... }
    default <T> T deserializeShape(ByteBuffer source, ShapeBuilder<T> builder) { ... }
}
```
| Codec | Format | Provider Pattern | Notes |
|-|-|-|-|
| `JsonCodec` | JSON | Pluggable `JsonSerdeProvider` | Supports `useJsonName`, timestamps, pretty print |
| `Rpcv2CborCodec` | CBOR | Pluggable `CborSerdeProvider` | Binary, compact |
| `XmlCodec` | XML | StAX directly | `wrapperElements` for AWS Query, honors XML traits |

## 6. HTTP Layer
**Source:** `http/`, `client/client-http/`, `client/client-http-binding/`
Core types: `HttpRequest`, `HttpResponse`, `HttpHeaders` — each with Modifiable/Unmodifiable variants for zero-copy freezing.
**HTTP Binding** maps Smithy `@http*` traits via `HttpBindingSchemaExtensions`:
| Trait | Binding | Wire Location |
|-|-|-|
| `@httpHeader` | HEADER | HTTP header |
| `@httpQuery` | QUERY | Query string |
| `@httpLabel` | LABEL | URI path template |
| `@httpPayload` | PAYLOAD | Entire body |
| `@httpPrefixHeaders` | PREFIX_HEADERS | Header prefix expansion |
| (none) | BODY | Protocol-codec body |
`HttpBindingClientProtocol` ties serialization, deserialization, and protocol together for REST-style protocols.

## 7. Auth System
**Source:** `auth-api/`, `client/client-auth-api/`, `aws/aws-sigv4/`
Resolution flow (in `ClientPipeline`): `AuthSchemeResolver.resolveAuthScheme(params)` → ordered `List<AuthSchemeOption>` → per option find `AuthScheme` → `IdentityResolver` → `Identity` → `Signer.sign(request, identity, properties)` → `SignResult`.
```java
public interface Identity { Instant expirationTime(); }
public interface IdentityResolver<T extends Identity> { IdentityResult<T> resolveIdentity(Context props); }
public interface Signer<RequestT, IdentityT> { SignResult<RequestT> sign(RequestT, IdentityT, Context); }
public interface AuthScheme<RequestT, IdentityT> { Signer<RequestT, IdentityT> signer(); IdentityResolver<IdentityT> identityResolver(IdentityResolvers resolvers); }
```
Concrete example — SigV4: `SigV4AuthScheme` + `SigV4Signer` (canonical request → signing key derivation → HMAC-SHA256 → Authorization header). Signing keys cached (300 entries).

## 8. Client Core Architecture
**Source:** `client/client-core/`
### Plugin System
```java
@FunctionalInterface public interface ClientPlugin { void configureClient(ClientConfig.Builder config); default Phase getPluginPhase() { return Phase.APPLY; } }
```
Phases: `FIRST` → `BEFORE_DEFAULTS` → `DEFAULTS` → `AFTER_DEFAULTS` → `BEFORE_APPLY` → `APPLY` → `AFTER_APPLY` → `LAST`. Plugins dedup by class. `AutoClientPlugin` SPI auto-applied.
### Interceptor System
~20 hooks in execution order: `modifyBeforeCall` → `readBeforeExecution` → `modifyBeforeSerialization` → ... → `readAfterExecution`. Hooks are read (observe) or modify (mutate). Per-attempt hooks repeat in the retry loop. `interceptCall` provides nesting composition for short-circuit/hedging/fallback.
### Call Pipeline
```
Client.call(input, operation, overrideConfig)
 → apply plugins/overrides → create pipeline
 → protocol.createRequest(operation, input)
 → [RETRY LOOP]: resolveAuthScheme → resolveEndpoint → sign → transport.send()
     → protocol.deserializeResponse() → if retryable: refreshToken → backoff → retry
 → interceptor.readAfterExecution
```
### Transport & Protocol Abstractions
`ClientTransport<Req, Resp>` (SPI via `ClientTransportFactory`): sends requests. `ClientProtocol<Req, Resp>`: creates requests from operations and deserializes responses. Bound via `MessageExchange` marker interface. `RequestOverrideConfig`: per-request overrides for protocol, endpoint, auth, interceptors, retry strategy.

## 9. Retries and Waiters
**Source:** `retries-api/`, `retries/`, `client/client-waiters/`
Token-based: `acquireInitialToken()` → `refreshRetryToken()` → `recordSuccess()`. Standard: 3 attempts, exponential backoff (50ms base, 20s max) + token bucket (500 tokens, 14 cost/retry). Adaptive: + CUBIC client-side rate limiting. `Claimable` prevents sharing retry state across clients. Retry decision pushed to `RetryInfo` (`isRetrySafe()`, `isThrottle()`, `retryAfter()`), not the strategy — pipeline checks it; strategy only controls *how* (backoff, cost). Waiters poll via ordered `Acceptor` list (first match wins) → SUCCESS/FAILURE/RETRY, exponential backoff w/ jitter.

## 10. Logging
**Source:** `logging/`
`InternalLogger` sealed interface adapts to whichever logging framework the app uses without imposing a dependency. Detection order (once at class-load): Log4j2 → SLF4J → Commons Logging → JDK `System.Logger` (always-available fallback), via try-catch on `NoClassDefFoundError` (no ServiceLoader). Arity-overloaded log methods (1–5 arg) avoid varargs allocation.

## 11. AWS Protocol Integrations
**Source:** `aws/client/`
| Protocol | Base Class | Routing | Request Format | Response Format |
|-|-|-|-|-|
| restJson1 | `HttpBindingClientProtocol` | URI + method | JSON + HTTP bindings | JSON |
| restXml | `HttpBindingClientProtocol` | URI + method | XML + HTTP bindings | XML |
| AWS JSON 1.0/1.1 | `HttpClientProtocol` (direct) | `X-Amz-Target` header | Full JSON body | JSON |
| AWS Query | `HttpClientProtocol` (direct) | `Action` form param | URL-encoded form | XML (wrapped) |
Key split: REST protocols delegate to `HttpBinding` for automatic trait-to-HTTP mapping; RPC protocols handle serialization directly (no HTTP binding traits). Event streaming: shared `aws-event-streams` module (`AwsEventFrame`, encoder/decoder factories).

## 12. Rules Engine
**Source:** `rulesengine/`
Resolves endpoints using a compiled ruleset transformed into a Binary Decision Diagram (BDD) executed by a stack-based VM. Compilation: `EndpointRuleSet` → CFG → BDD → sifting optimization → node reversal → bytecode. BDD: flat `int[]` (3 ints/node). Trivial conditions inlined in BDD traversal. VM ~30 opcodes; thread-local evaluators, URI caching, fused opcodes.

## 13. Dynamic Client
**Source:** `client/dynamic-client/`, `dynamic-schemas/`
Creates clients at runtime from a Smithy `Model` — no codegen. All interaction via `Document`.
```java
DynamicClient client = DynamicClient.builder().model(model).serviceId(ShapeId.from("com.example#MyService")).build();
Document output = client.call("GetItem", Map.of("id", "abc"));
```
Key components: `SchemaConverter` (Smithy `Shape` → runtime `Schema`, handles cycles), `StructDocument` (implements both `Document` and `SerializableStruct`), `ContentDocument`, `SchemaGuidedDocumentBuilder`.

## 14. Server (`@SmithyUnstableApi`)
**Source:** `server/`
Handler pipeline: `ProtocolHandler` (deserialize) → `ValidationHandler` → `OperationHandler`. Orchestrator hierarchy: `SingleThreadOrchestrator` → `ErrorHandlingOrchestrator` → `OrchestratorGroup`. Netty: `HttpRequestHandler` resolves protocol+operation via `ProtocolResolver` (SPI `ServerProtocolProvider`).

## 15. MCP (`@SmithyUnstableApi`)
**Source:** `mcp/`, `smithy-ai-traits/`
Bridges Smithy services to MCP (JSON-RPC 2.0). Operation name → tool name; input schema → JSON Schema properties; `@readonly`/`@idempotent` → tool annotations. Transports: Stdio, HTTP proxy (POST + SSE). Interceptor pipeline: `readBeforeExecution` → ... → `readAfterExecution`.

## 16. Code Generation
**Source:** `codegen/`. General guidance: https://smithy.io/2.0/guides/building-codegen/index.html
**Entry point:** `JavaCodegenPlugin` (SmithyBuildPlugin, name: `"java-codegen"`). Modes: `CLIENT`, `SERVER`, `TYPES`.
**Writer (`JavaWriter`):** Custom formatters — `$T` (type + import), `$B` (boxed type), `$U` (capitalize), `$N` (@Nullable). `$C` accepts a lambda and inlines its output.
**Types shared between client/server:** All type generators (`StructureGenerator`, `UnionGenerator`, `EnumGenerator`) live in `codegen-core`, mode-agnostic. Only service-level generators differ per mode.
**Dummy service trick:** For TYPES-only mode, `SyntheticServiceTransform` creates a synthetic `ServiceShape` + operation + input struct referencing all types in the closure (directed codegen requires a root service).
**Integration system (SPI):** `JavaCodegenIntegration extends SmithyIntegration`. Extension points: `preprocessModel()`, `decorateSymbolProvider()`, `interceptors()`, `traitInitializers()`. Register in `META-INF/services/software.amazon.smithy.java.codegen.JavaCodegenIntegration`.
