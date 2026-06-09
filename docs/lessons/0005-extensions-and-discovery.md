# Lesson 5 — Extensions & Discovery

> **smithy-java · protocol track · lesson 5**
> **Goal:** Precompute wire data and register a protocol so the framework finds it.
> **Time:** ~13 min · **Builds on:** Lessons 3–4 (serde & codecs).

Two SPIs make a protocol real. **Schema extensions** precompute your wire data (Lesson 3's promise,
kept). **`ServiceLoader`** is how the framework *finds* your protocol at all. Miss either and your
protocol is silently absent.

---

## 1. Schema extensions: precompute once, index forever

The mechanism is a pair of types. A **key** (a unique integer ID → array slot):

```java
public final class SchemaExtensionKey<T> {
    private static final AtomicInteger NEXT_ID = new AtomicInteger();
    final int id;
    public SchemaExtensionKey() { this.id = NEXT_ID.getAndIncrement(); }  // O(1) lookup later
}
```

And a **provider** that computes the value for each schema (discovered via `ServiceLoader`):

```java
public interface SchemaExtensionProvider<T> {
    SchemaExtensionKey<T> key();        // which slot
    T provide(Schema schema);           // compute wire data from the schema's traits
}
```

> Source: [`SchemaExtensionProvider.java`](https://github.com/smithy-lang/smithy-java/blob/main/core/src/main/java/software/amazon/smithy/java/core/schema/SchemaExtensionProvider.java) · [`SchemaExtensionKey.java`](https://github.com/smithy-lang/smithy-java/blob/main/core/src/main/java/software/amazon/smithy/java/core/schema/SchemaExtensionKey.java)

> **The contract that bites:** `provide()`'s result is published across threads via a *plain* array
> store (no lock, benign race). The Javadoc requires the returned object's fields to be **all
> `final`** — which is why these are **records**. The JMM guarantees final-field visibility once
> the constructor returns (JLS 17.5). Return a mutable object and you have a data race.

At serialization, the codec does `schema.getExtension(KEY)` — an array index, no hashing — to get
its precomputed bytes/formatters. This is the "no trait lookups on the hot path" claim made concrete.

---

## 2. Registering it: `META-INF/services`

A provider that isn't registered never runs. Registration is a plain-text file named after the
*fully-qualified interface*, containing the implementation class name(s):

```
File: codecs/json-codec/src/main/resources/META-INF/services/
      software.amazon.smithy.java.core.schema.SchemaExtensionProvider

software.amazon.smithy.java.json.JsonSchemaExtensions
software.amazon.smithy.java.json.smithy.SmithyJsonSchemaExtensions
```

Real file, verbatim — note JSON registers *two* providers. CBOR and HTTP-binding each register
their own.

---

## 3. The other SPI: finding the protocol itself

Same mechanism, different interface. Your `ClientProtocolFactory` (the `Factory` inner class from
Lesson 2) is registered under *its* interface name:

```
File: client/client-rpcv2-cbor/src/main/resources/META-INF/services/
      software.amazon.smithy.java.client.core.ClientProtocolFactory

software.amazon.smithy.java.client.rpcv2.RpcV2CborProtocol$Factory
```

(The `$Factory` is the nested class.) The framework consumes it in `DetectProtocolPlugin`:

```java
static {
    for (var impl : ServiceLoader.load(ClientProtocolFactory.class, ...)) {
        PROTOCOL_FACTORIES.add(impl);     // every registered factory, loaded once
    }
}
// later, given a service's protocol traits:
for (var impl : PROTOCOL_FACTORIES) {
    if (protocols.containsKey(impl.id())) {       // trait ShapeId == factory.id()
        return impl.createProtocol(settings, protocols.get(impl.id()));
    }
}
```

> Source: [`DetectProtocolPlugin.java`](https://github.com/smithy-lang/smithy-java/blob/main/client/dynamic-client/src/main/java/software/amazon/smithy/java/dynamicclient/plugins/DetectProtocolPlugin.java)

This closes the loop from Lesson 1: `id()` returns the protocol trait's `ShapeId` precisely so this
matching works. The service model says "I speak `smithy.protocols#rpcv2Cbor`"; the framework finds
the factory whose `id()` equals that ShapeId and builds it with `ProtocolSettings`. (When multiple
match, the first found wins.)

> ⚠️ **The #1 "my protocol doesn't work" bug:** the class is correct but the `META-INF/services`
> line is missing, misspelled, or points to the wrong class (forgetting `$Factory` for a nested
> class). `ServiceLoader` fails *silently* — no error, the protocol is just never found. Check this
> file first.

---

## 4. The protocol trait, briefly

The join key — the `ShapeId` — comes from a Smithy *protocol trait* applied to the service, e.g.
`@rpcv2Cbor` or `@restJson1`. These traits are marked `@protocolDefinition` in their Smithy model
and ship as `*Trait` Java classes (`Rpcv2CborTrait.ID`, `RestJson1Trait.ID`) you reference directly.
For an existing standard protocol the trait already exists; for a brand-new one you'd define the
trait in a Smithy model first. Either way, `factory.id()` returns `YourTrait.ID`.

---

## 5. Quick check

**Q1.** Why must a `SchemaExtensionProvider` return a record (or all-final object)?
- a) Records are shorter to write.
- b) The result is published across threads via a plain array store with no lock; final fields guarantee safe visibility (JLS 17.5).
- c) `ServiceLoader` only instantiates records.

**Q2.** Perfect `ClientProtocol` + `Factory`, but the client says "no matching protocol." First check?
- a) Recompile the generated shapes.
- b) Whether the codec supports your timestamp format.
- c) The `META-INF/services/...ClientProtocolFactory` file — present? correct class? `$Factory` suffix?

**Q3.** How does the framework match a service to your protocol implementation?
- a) It compares each registered factory's `id()` (a `ShapeId`) against the protocol traits on the service.
- b) By the Java package name of the protocol class.
- c) Alphabetically by class name.

<details><summary><strong>Answers</strong></summary>

- **Q1 → b.** The lock-free, benign-race publication is safe *only* because final fields are visible after construction. A mutable extension would be a data race.
- **Q2 → c.** Silent `ServiceLoader` failure from a missing/typo'd registration is the classic cause. Verify that file before anything else.
- **Q3 → a.** `factory.id() == YourTrait.ID`, and the service's `@yourProtocol` trait carries that same ShapeId. That's why Lesson 1 stressed what `id()` returns.

</details>

---

## 6. What you now know

You can wire the two SPIs that make a protocol exist: a `SchemaExtensionProvider` (record-typed,
registered) to precompute wire data, and a `ClientProtocolFactory` (registered under its interface)
so `ServiceLoader` discovery matches your `id()` to a service's protocol trait. You know the
silent-failure trap.

**Next — Lesson 6:** compare the two base-class families head-to-head, so you can pick the right
parent for a new protocol.

## Reference
- [Glossary](../reference/glossary.html)
- [`SchemaExtensionProvider.java`](https://github.com/smithy-lang/smithy-java/blob/main/core/src/main/java/software/amazon/smithy/java/core/schema/SchemaExtensionProvider.java) · [`DetectProtocolPlugin.java`](https://github.com/smithy-lang/smithy-java/blob/main/client/dynamic-client/src/main/java/software/amazon/smithy/java/dynamicclient/plugins/DetectProtocolPlugin.java)
