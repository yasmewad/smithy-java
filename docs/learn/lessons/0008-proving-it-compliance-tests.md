# Lesson 8 of 8 · Proving it — compliance tests

> smithy.io/learn · adding a protocol · Lesson 8 of 8
> Estimated time: ≈ 7 min · the payoff · checklist inside

Seven lessons in, you can read a protocol, name what each method does, pick a
base class, and wire it up for discovery. One question remains, and it is the
only one that matters at the end: *how do you know your protocol is actually
correct?* Not "compiles," not "my unit test passes" — actually produces the
exact bytes the protocol specification demands.

## The authoritative correctness bar

smithy-java answers this with **protocol compliance tests**. The KT doc states
the rule plainly:

> **The rule.** "*Protocol compliance tests are the authoritative correctness
> check.* Run them before and after any change touching serde, codecs, HTTP
> binding, or protocol integrations. A passing unit test that fails a compliance
> test is a bug."

Here is the beautiful part: the tests are not Java you write. They live *inside
the Smithy model* as traits — `@httpRequestTests` and `@httpResponseTests` —
each one a concrete example that says "given this input, the wire bytes must look
exactly like *this*." A module named `protocol-test-harness/` reads those traits,
generates a real client (and server), runs your protocol against every example,
and compares the actual bytes to the expected bytes.

> **The one idea to hold onto.** Compliance tests are the protocol specification,
> made executable. You do not grade your own homework — the Smithy-defined test
> suite does. Per the KT doc, **14+ test suites** cover rpcv2-cbor, restJson1,
> restXml, awsJson, and awsQuery, for both client and server.

## What running it looks like

Two commands carry you across the finish line. The first runs the harness on its
own; the second runs everything, which is what a reviewer will expect to be green:

```bash
# from the repository root
./gradlew :protocol-test-harness:test   # just the compliance suites
./gradlew build                          # compile + all tests, everywhere
```

The `:protocol-test-harness:test` command is the one from the KT doc's "Run
Tests" section, annotated there as "*protocol compliance tests (restJson1,
restXml, awsJson, awsQuery, rpcv2-cbor)*." When it is green, your bytes match the
spec.

## How a suite is declared

A compliance suite is a tiny Java class. It names the service that carries the
test traits and the side under test — then hands each test to a comparator. This
is the *entire* client suite for rpcv2Cbor:

File: `client/client-rpcv2-cbor/src/it/java/software/amazon/smithy/java/client/rpcv2/RpcV2CborProtocolTests.java`

```java
@ProtocolTest(
        service = "smithy.protocoltests.rpcv2Cbor#RpcV2Protocol",
        testType = TestType.CLIENT)
public class RpcV2CborProtocolTests {
    @HttpClientRequestTests
    public void requestTest(DataStream expected, DataStream actual) {
        CborComparator.assertEquals(expected.asByteBuffer(), actual.asByteBuffer());
    }

    @HttpClientResponseTests
    public void responseTest(Runnable test) {
        test.run();
    }
}
```

That is it. The `@ProtocolTest` annotation triggers generation of one test per
trait found in the service closure. `requestTest` receives the `expected` and
`actual` wire bytes and asserts they are equal; `responseTest` just runs the
generated check. No per-test code — you write the comparison once and the harness
fans it out over every example in the model.

> **You will skip a few — on purpose.** The real file uses
> `@ProtocolTestFilter(skipTests = { … })` to exclude a handful of cases with
> recorded reasons (an upstream Smithy bug, a not-yet-supported `clientOptional`
> path). Skipping a known-broken case with a comment is normal and honest;
> silently passing a wrong one is the bug the rule warns about.

## The definitive checklist

Everything this track taught, in the order you would actually do it. Each step
points back to the lesson that explains the *why*.

| Step | What you do | Taught in |
|------|-------------|-----------|
| 1 | **Pick a base class.** REST-style (HTTP-bound traits) → extend `HttpBindingClientProtocol`; RPC-style (routing by header or path) → extend `HttpClientProtocol`, or a shared abstract like `AbstractRpcV2ClientProtocol`. | Lesson 7 |
| 2 | **Reuse or write a codec.** If your wire format already has a `Codec` (JSON, CBOR, XML), reuse it; only write a new one for a genuinely new format. | Lesson 5 |
| 3 | **Implement `ClientProtocol`.** Let the base class do the heavy lifting; you supply the small overrides (like `codec()`) plus a nested `Factory` so config can build your protocol. | Lessons 2 & 3 |
| 4 | **Add a `SchemaExtensionProvider`** — only if your wire needs trait data precomputed onto schemas. Skip it if you reuse a codec that already ships one. | Lesson 6 |
| 5 | **Register the `Factory`** in `META-INF/services/` so `ServiceLoader` can discover it. Missing registration = the protocol silently does not exist. | Lesson 6 |
| 6 | **Add compliance tests and run the harness.** Point a `@ProtocolTest` class at your service and run `:protocol-test-harness:test`. | Lesson 8 |

Step 5 is a single line of text in a file. This is the exact registration for the
rpcv2Cbor client protocol you dissected earlier:

File: `client/client-rpcv2-cbor/src/main/resources/META-INF/services/software.amazon.smithy.java.client.core.ClientProtocolFactory`

```
software.amazon.smithy.java.client.rpcv2.RpcV2CborProtocol$Factory
```

The file *name* is the interface being implemented (`ClientProtocolFactory`); the
file *content* is the fully-qualified name of your `Factory` — the `$Factory`
suffix is Java's notation for the nested class you saw in `RpcV2CborProtocol`.
That one line is what turns your code into a protocol the runtime can find.

> **Two silent-failure traps.** The KT doc flags both: forget the
> `META-INF/services/` entry and the "feature is silently absent"; add trait
> behavior without updating its `SchemaExtensionProvider` and it "silently has no
> wire effect." Both compile. Both pass unit tests. Only the compliance suite
> catches them.

## You made it

You started at "codegen is a locked door" and you finish able to describe, in
order, every step to add a protocol to smithy-java — and, just as importantly,
how to *prove* it works. A protocol is no longer a mystery; it is a base class, a
codec, a factory, maybe an extension, one line of registration, and a compliance
suite that keeps you honest. That is the whole task. Go read a real one end to
end — you now can.

## Self-check

**Why are protocol compliance tests called the authoritative correctness check?**

- **A. They replay Smithy wire tests against your real code** ✅
- B. They compile the module faster than the unit tests
- C. They format the source and apply the license header
- D. They publish the snapshot jar to your local Maven

**Correct answer: A.** The harness reads Smithy-defined `@httpRequestTests` /
`@httpResponseTests` and checks your protocol's actual bytes against the spec's
expected bytes — the spec, made executable. It is not about build speed,
formatting, or publishing.

## Primary source

KT doc *Smithy Java, Knowledge Transfer* — "Run Tests," "Correctness First," and
"Common Workflows: Adding a protocol compliance test." Verified in-repo:
`protocol-test-harness/`,
`client/client-rpcv2-cbor/src/it/.../RpcV2CborProtocolTests.java`, and the
`META-INF/services/...ClientProtocolFactory` registration. Spec traits:
<https://smithy.io/2.0/additional-specs/http-protocol-compliance-tests.html>.

---

← Prev: [Choosing your base class](0007-choosing-your-base-class.md) ·
Finish: [Back to the track overview](index.md)
