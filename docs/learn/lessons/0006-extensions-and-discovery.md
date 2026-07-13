# Lesson 6 of 8 · Wiring it up

# Extensions and discovery: making a protocol *known*

You can write a perfect protocol class and the framework will still act as if
it doesn’t exist. Two small wiring mechanisms are what turn your code from
“compiled” into “found and used.”

*≈ 7 min · SPI wiring · META-INF/services*

By now you’ve met the `ClientProtocol` contract (Lesson 2), the smallest real
protocol (Lesson 3), the schema-directed serde trick (Lesson 4), and the codec
layer (Lesson 5). But there is a gap nobody warns you about: a protocol that
*compiles* is not the same as a protocol the framework can *find and run*. This
lesson is about closing that gap. There are exactly two mechanisms to learn, and
both are small.

## Mechanism 1 — Schema extensions: precompute the wire data once

Recall the key idea from Lesson 4: at write-time, the codec *never* inspects
Smithy traits. As the KT doc puts it, “All protocol decisions are precomputed
into extensions (byte arrays, formatters, lookup tables) at Schema
construction.” The mechanism that attaches that precomputed data to a `Schema`
is a small SPI pair: a **key** and a **provider**.

The key is a typed label. It just hands out a unique integer id so the data can
live in an array slot for O(1) lookup:

File: `core/src/main/java/software/amazon/smithy/java/core/schema/SchemaExtensionKey.java`

```java
public final class SchemaExtensionKey<T> {
    private static final AtomicInteger NEXT_ID = new AtomicInteger();
    final int id;
    public SchemaExtensionKey() {
        this.id = NEXT_ID.getAndIncrement();
    }
}
```

The provider is where a protocol computes what it needs. It declares its key and
fills in a value for a given schema:

File: `core/src/main/java/software/amazon/smithy/java/core/schema/SchemaExtensionProvider.java`

```java
public interface SchemaExtensionProvider<T> {
    SchemaExtensionKey<T> key();
    T provide(Schema schema);
}
```

Here is a real one. The CBOR codec precomputes its field-name byte tables and a
lookup structure so serialization is a byte-copy, not a trait scan:

File: `codecs/cbor-codec/src/main/java/software/amazon/smithy/java/cbor/CborSchemaExtensions.java`

```java
public final class CborSchemaExtensions
        implements SchemaExtensionProvider<CborSchemaExtensions.CborExtension> {

    public static final SchemaExtensionKey<CborExtension> KEY = new SchemaExtensionKey<>();

    public record CborExtension(
            byte[] memberNameBytes,
            CborMemberLookup memberLookup,
            byte[][] fieldNameTable) {}

    @Override public SchemaExtensionKey<CborExtension> key() { return KEY; }
    @Override public CborExtension provide(Schema schema) { /* precompute from the schema */ }
}
```

At runtime, the codec pulls its precomputed value straight off the schema by
key:

```java
CborSchemaExtensions.CborExtension ext = schema.getExtension(CborSchemaExtensions.KEY);
```

> **The three you’ll see**
> `SmithyJsonSchemaExtensions` precomputes UTF-8 field-name byte arrays for the
> native JSON codec. `CborSchemaExtensions` precomputes CBOR-encoded field
> names. `HttpBindingSchemaExtensions` precomputes each member’s binding
> (HEADER / QUERY / LABEL / PAYLOAD / BODY) and wire names. Each declares its own
> `public static final SchemaExtensionKey<…> KEY` and reads it back via
> `schema.getExtension(KEY)`.

> **Why this is a silent trap**
> The KT doc rule is blunt: “Always update schema extensions when adding trait
> behavior. Protocol codecs read precomputed extension data, not traits at
> runtime. Adding a trait without updating its `SchemaExtensionProvider`
> silently has no wire effect.” No crash, no warning — the byte just never
> appears on the wire. If your protocol needs a trait to change the bytes, the
> extension is where that happens.

## Mechanism 2 — ServiceLoader: how the framework *finds* your code

The second mechanism is the one that decides whether your protocol exists at
all. smithy-java never hard-codes a list of protocols. Instead it uses Java’s
built-in **ServiceLoader**: at startup it asks “which classes out there
implement this interface?” and the answer comes from plain text files under
`META-INF/services/`. This is the same pattern the CLAUDE.md architecture notes
call out: “Uses Java ServiceLoader extensively for protocol, transport, and
codec discovery.”

The schema-extension providers you just saw are themselves discovered this way.
The core module loads them once at class-load:

File: `core/src/main/java/software/amazon/smithy/java/core/schema/Schema.java`

```java
var loader = ServiceLoader
        .load(SchemaExtensionProvider.class, SchemaExtensionProvider.class.getClassLoader());
```

For that loader to see the CBOR extension, the cbor-codec module ships a file
whose *name* is the interface and whose *contents* are the implementing classes:

File: `codecs/cbor-codec/src/main/resources/META-INF/services/software.amazon.smithy.java.core.schema.SchemaExtensionProvider`

```
software.amazon.smithy.java.cbor.CborSchemaExtensions
```

A protocol is registered the same way, against a different interface —
`ClientProtocolFactory`, the little factory that knows a protocol’s id and how
to build it:

File: `client/client-core/.../ClientProtocolFactory.java`

```java
public interface ClientProtocolFactory<T extends Trait> {
    ShapeId id();
    ClientProtocol<?, ?> createProtocol(ProtocolSettings settings, T trait);
}
```

And here is a real registration — the file that makes the CBOR protocol
discoverable. This is the exact path you would copy for a new protocol:

File: `client/client-rpcv2-cbor/src/main/resources/META-INF/services/software.amazon.smithy.java.client.core.ClientProtocolFactory`

```
software.amazon.smithy.java.client.rpcv2.RpcV2CborProtocol$Factory
```

The AWS restJson1 protocol looks identical, just pointing at its own factory:

File: `aws/client/aws-client-restjson/src/main/resources/META-INF/services/software.amazon.smithy.java.client.core.ClientProtocolFactory`

```
software.amazon.smithy.java.aws.client.restjson.RestJsonClientProtocol$Factory
```

> **Reading that filename**
> The `$Factory` at the end is Java’s notation for a nested class:
> `RpcV2CborProtocol.Factory`. In the source it is exactly
> `public static final class Factory implements ClientProtocolFactory<Rpcv2CborTrait>`.
> The file name is the SPI interface; the single line inside is the class
> ServiceLoader should instantiate.

> **Forget this file and the feature is silently absent**
> The KT doc states the rule directly: “Register new SPIs in
> `META-INF/services/`. ServiceLoader is the discovery mechanism. Missing
> registration = feature silently absent.” Your protocol class compiles, your
> tests that instantiate it directly pass, but the client builder can’t find it.
> There is no error to grep for — the framework simply behaves as though your
> protocol was never written.

## Putting the two together

These two mechanisms answer two different “known” questions. The **schema
extension** makes your protocol’s *wire data* known to the schema — precomputed
once, read on the hot path with no trait lookups. The **ServiceLoader
registration** makes your protocol *itself* known to the framework — discovered
at startup with no code changes anywhere else. Miss the first and the bytes are
wrong or missing; miss the second and nothing runs at all. Both failures are
silent, which is exactly why they’re on this checklist.

## Self-check

**You wrote and compiled a `ClientProtocolFactory` but never added it to a
`META-INF/services/` file. What happens?**

- The client build fails and stops before finishing
- **The protocol is silently absent and never discovered** ✓ (correct)
- The runtime throws a clear and descriptive error
- Other bundled protocols quietly fill the empty slot

ServiceLoader reads those files to discover implementations; with no line, your
factory is never found. The KT doc: “Missing registration = feature silently
absent.” No crash, no warning.

## Primary source

Verified in-repo:
`core/.../schema/SchemaExtensionProvider.java`,
`SchemaExtensionKey.java`, and the service file
`client/client-rpcv2-cbor/src/main/resources/META-INF/services/software.amazon.smithy.java.client.core.ClientProtocolFactory`.
Rules quoted from the internal *Smithy Java Knowledge Transfer* doc (“Register
new SPIs in `META-INF/services/`”). ServiceLoader background:
https://smithy.io/2.0/guides/building-codegen/index.html

---

- [← Prev: The codec layer](0005-the-codec-layer.md)
- [Next: Choosing your base class →](0007-choosing-your-base-class.md)
- [Track overview](index.md)
