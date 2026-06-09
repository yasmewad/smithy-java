# Lesson 7 — Proving It: Compliance Tests (Finale)

> **smithy-java · protocol track · lesson 7 · finale**
> **Goal:** Prove a protocol with compliance tests, and recap the whole flow.
> **Time:** ~12 min · **Builds on:** the whole track.

A protocol you can't prove correct is a liability. smithy-java's answer is *protocol compliance
tests*: Smithy-defined request/response cases run against your real code. This is the authoritative
correctness bar — and the last piece of adding a protocol.

---

## 1. Why compliance tests, not just unit tests

The Smithy spec ships `@httpRequestTests` and `@httpResponseTests` — declarative cases that say
"given this input, the wire bytes must be exactly this" (and vice-versa for responses). The
`protocol-test-harness` runs them against your actual protocol + codec.

> **A passing unit test that fails a compliance test is a bug.** Compliance tests are the source of
> truth because they test the contract the wire format actually promises — not your interpretation
> of it. Run them before *and* after any serde/codec/binding change.

---

## 2. What a compliance test class looks like

Strikingly little code — the harness generates the cases; you declare the service under test and how
to compare. This is the entire CBOR client test:

```java
@ProtocolTest(
    service = "smithy.protocoltests.rpcv2Cbor#RpcV2Protocol",
    testType = TestType.CLIENT)
public class RpcV2CborProtocolTests {

    @HttpClientRequestTests
    @ProtocolTestFilter(skipTests = { "...known-broken-case..." })
    public void requestTest(DataStream expected, DataStream actual) {
        CborComparator.assertEquals(expected.asByteBuffer(), actual.asByteBuffer());
    }

    @HttpClientResponseTests
    public void responseTest(Runnable test) { test.run(); }
}
```

> Source: [`RpcV2CborProtocolTests.java`](https://github.com/smithy-lang/smithy-java/blob/main/client/client-rpcv2-cbor/src/it/java/software/amazon/smithy/java/client/rpcv2/RpcV2CborProtocolTests.java) · `@ProtocolTest` is a JUnit 5 extension ([`ProtocolTest.java`](https://github.com/smithy-lang/smithy-java/blob/main/protocol-test-harness/src/main/java/software/amazon/smithy/java/protocoltests/harness/ProtocolTest.java)).

Three things make this work; you supply all three in the module's `build.gradle.kts`:

```kotlin
plugins { id("smithy-java.protocol-testing-conventions") }   // wires the harness + integ tests

testImplementation(libs.smithy.protocol.tests)               // the Smithy-defined test models

// generate test sources from the protocol's test service:
addGenerateSrcsTask(generator, "rpcv2Cbor", "smithy.protocoltests.rpcv2Cbor#RpcV2Protocol")
```

> Source: [`client-rpcv2-cbor/build.gradle.kts`](https://github.com/smithy-lang/smithy-java/blob/main/client/client-rpcv2-cbor/build.gradle.kts)

---

## 3. Running them

```bash
# the authoritative correctness check across all protocols
./gradlew :protocol-test-harness:test

# your protocol's own compliance suite (it = integration tests)
./gradlew :client:client-rpcv2-cbor:integ
```

The `protocol-testing-conventions` plugin even wires `test` to be finalized by `integ`, so
compliance runs as part of the normal suite. A protocol without a green compliance run is not done.

---

## 4. The whole flow, end to end

Everything from the track, as the checklist to add an HTTP protocol:

1. **Get the protocol trait.** Reference an existing `*Trait.ID`, or define a new
   `@protocolDefinition` trait in a Smithy model. *(Lesson 5 — the join key)*
2. **Pick your base class.** `@http*` member bindings → `HttpBindingClientProtocol` (REST);
   operation-name routing + whole-body → `HttpClientProtocol` (RPC). *(Lesson 6)*
3. **Choose a codec.** Reuse `JsonCodec`/`Rpcv2CborCodec`/`XmlCodec` via its builder; write a new
   `Codec` only if the byte format is genuinely new. *(Lesson 4)*
4. **Implement the class.** Override the handful of abstract methods (codec, media type, error
   deserializer). Inherit `createRequest`/`deserializeResponse` where possible. *(Lessons 1–2)*
5. **Add a schema extension if needed.** Precompute trait-derived wire data as a record; needed when
   your wire format reads new traits. *(Lessons 3 & 5)*
6. **Register both SPIs.** `ClientProtocolFactory` (and any `SchemaExtensionProvider`) in
   `META-INF/services/`. The silent-failure trap. *(Lesson 5)*
7. **Prove it.** Add a `@ProtocolTest` class, wire the gradle conventions, run the compliance suite
   green. *(this lesson)*

---

## 5. Final check

**Q1.** Unit tests pass but a `@httpRequestTests` case fails. Verdict?
- a) The compliance test is too strict; ship it.
- b) It's a bug in your protocol — compliance tests are the authoritative correctness bar.
- c) Unit and compliance tests are independent; ignore the conflict.

**Q2.** How many wire-format assertion cases do you hand-write in a `@ProtocolTest` class?
- a) One per operation.
- b) One per member.
- c) None — the harness generates cases from the Smithy test model; you declare the service and a comparison method.

**Q3.** A teammate adds "rpcv2Json": reuses `JsonCodec`, extends `AbstractRpcV2ClientProtocol`, registers the factory — but the client can't find it. Most likely cause?
- a) The `META-INF/services/...ClientProtocolFactory` entry is missing or names the wrong class (forgot `$Factory`).
- b) `JsonCodec` can't be used by an RPC protocol.
- c) They need to regenerate every shape for the new protocol.

<details><summary><strong>Answers</strong></summary>

- **Q1 → b.** "A passing unit test that fails a compliance test is a bug." The wire format is the contract; the compliance suite is the source of truth.
- **Q2 → c.** You wire the gradle conventions + point at the test service; the harness supplies the cases. Your job is the comparator (e.g. `CborComparator.assertEquals`).
- **Q3 → a.** Discovery is ShapeId-matched over `ServiceLoader`-registered factories; a bad registration fails silently. Codecs are orthogonal to RPC/REST, and shapes are protocol-agnostic — neither (b) nor (c) applies.

</details>

---

## 🏁 Track complete

You can now trace a call through the contract, read any existing protocol and name its base class,
explain why protocols and generated code are decoupled, wire both SPIs, pick the right parent, and
prove the result with compliance tests. That's deep architectural mastery — earned through the lens
of one concrete task.

### Where to go next (beyond this track)
- **Server side:** `ServerProtocol` / `ServerProtocolProvider` / `ProtocolResolver` — the mirror image, for accepting requests.
- **Build a toy protocol end-to-end** in a scratch module and watch the compliance suite go green.
- **Read `AwsQueryClientProtocol`** — the most "different" protocol (form-encoded body, XML response), to test your model.

## Reference
- [Glossary](../reference/glossary.html)
- [`RpcV2CborProtocolTests.java`](https://github.com/smithy-lang/smithy-java/blob/main/client/client-rpcv2-cbor/src/it/java/software/amazon/smithy/java/client/rpcv2/RpcV2CborProtocolTests.java) · [`ProtocolTest.java`](https://github.com/smithy-lang/smithy-java/blob/main/protocol-test-harness/src/main/java/software/amazon/smithy/java/protocoltests/harness/ProtocolTest.java)
