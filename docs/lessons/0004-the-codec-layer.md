# Lesson 4 — The Codec Layer

> **smithy-java · protocol track · lesson 4**
> **Goal:** Know what a `Codec` is and when to write one vs reuse one.
> **Time:** ~11 min · **Builds on:** Lesson 3 (the serde interfaces).

Your protocol returns a `Codec` from `payloadCodec()`. That object is the bridge between
schema-carrying shapes and raw bytes. This lesson shows what the interface actually is — and the key
decision: *reuse* a codec vs *write* one.

---

## 1. The interface is tiny

A `Codec` is just a factory for the two visitor implementations from Lesson 3, plus two convenience
methods:

```java
public interface Codec extends AutoCloseable {
    ShapeSerializer   createSerializer(OutputStream sink);     // struct → bytes
    ShapeDeserializer createDeserializer(ByteBuffer source);   // bytes → struct

    // Convenience, built on the two above:
    default ByteBuffer serialize(SerializableShape shape) { ... }
    default <T> T deserializeShape(ByteBuffer src, ShapeBuilder<T> builder) { ... }
}
```

> Source: [`core/.../core/serde/Codec.java`](https://github.com/smithy-lang/smithy-java/blob/main/core/src/main/java/software/amazon/smithy/java/core/serde/Codec.java)

When `AbstractRpcV2ClientProtocol` called `codec().serialize(input)` in Lesson 2, it used the
`default serialize` here: it spins up a `ShapeSerializer`, lets the shape push its members into it,
and collects the bytes.

---

## 2. The three codecs that ship today

| Codec | Format | Notes |
|---|---|---|
| `JsonCodec` | JSON | Pluggable `JsonSerdeProvider`; options for `useJsonName`, timestamp format, pretty-print. Hand-written parser on `byte[]` — no Jackson on the hot path. |
| `Rpcv2CborCodec` | CBOR | Pluggable `CborSerdeProvider`; binary, compact. `VarHandle` for big-endian reads. |
| `XmlCodec` | XML | StAX-based; `wrapperElements` for AWS Query; honors XML traits. |

Protocols **configure**, not reimplement, these. `RestJsonClientProtocol` builds its codec with
options tuned for REST JSON:

```java
this.codec = JsonCodec.builder()
    .useJsonName(true)            // respect @jsonName
    .useTimestampFormat(true)     // respect @timestampFormat
    .defaultNamespace(service.getNamespace())
    .build();
```

> Source: [`RestJsonClientProtocol.java`](https://github.com/smithy-lang/smithy-java/blob/main/aws/client/aws-client-restjson/src/main/java/software/amazon/smithy/java/aws/client/restjson/RestJsonClientProtocol.java)

---

## 3. The decision: reuse or write?

> **Default to reuse.** If your new protocol's body is JSON, CBOR, or XML — even with quirks — you
> configure an existing codec via its builder. You write a *new* codec only when the **byte format
> itself** is genuinely new (MessagePack, Protobuf, a bespoke binary layout). That's a much rarer
> and bigger task than "add a protocol."

Two distinct axes, often confused:

| Axis | What changes | Example |
|---|---|---|
| **Protocol** | Framing: URI, method, headers, error signaling, where members bind | restJson1 vs AWS JSON 1.0 — *both use JSON* |
| **Codec** | The payload byte encoding itself | JSON vs CBOR vs XML |

restJson1 and AWS JSON 1.0 share `JsonCodec` but are different *protocols*. rpcv2Cbor and a
hypothetical rpcv2Json share framing but differ by *codec*. Knowing which axis you're moving on
tells you how much work you're signing up for.

If you *do* write a codec, the steps (from the KT doc's "Adding a new codec"):

```
1. Implement Codec, ShapeSerializer, ShapeDeserializer in a new module
2. Add a SchemaExtensionProvider to precompute trait-derived wire data   // Lesson 5
3. Register the codec's extension via ServiceLoader in META-INF/services/
4. Add protocol compliance tests                                          // Lesson 7
```

---

## 4. Quick check

**Q1.** The two methods that *define* a `Codec` (everything else is a default)?
- a) `encode()` and `decode()`
- b) `createSerializer(OutputStream)` and `createDeserializer(ByteBuffer)`
- c) `serialize()` and `deserializeShape()`

**Q2.** Your new protocol sends JSON bodies but with AWS-JSON-style framing (`X-Amz-Target`, full-body payload). Do you write a new codec?
- a) Yes — different protocol means different codec.
- b) Yes — the header changes the byte format.
- c) No — the byte format is still JSON. Reuse `JsonCodec`; put framing in your `ClientProtocol`.

**Q3.** How does `RestJsonClientProtocol` make its codec honor `@jsonName`?
- a) `JsonCodec.builder().useJsonName(true)`.
- b) It subclasses `JsonCodec`.
- c) It reads `@jsonName` at request time in `createRequest`.

<details><summary><strong>Answers</strong></summary>

- **Q1 → b.** A codec is a *factory* for a `ShapeSerializer` and `ShapeDeserializer`. `serialize`/`deserializeShape` are defaults built on top.
- **Q2 → c.** Protocol ≠ codec. Framing lives in the protocol; the codec only encodes the body. restJson1 and AWS JSON are different protocols sharing one JSON codec.
- **Q3 → a.** Codecs are configured through their builders — a flag flips precomputed-field-name behavior. No subclassing, no per-request trait reads (Lesson 3).

</details>

---

## 5. What you now know

You can describe the `Codec` interface, name the three shipping codecs, and — crucially — separate
the **protocol axis** (framing) from the **codec axis** (byte format). That distinction is what
makes "add a protocol" usually a small job.

**Next — Lesson 5:** the schema-extension SPI and `ServiceLoader` discovery — how wire data is
precomputed and how the framework *finds* your protocol.

## Reference
- [Glossary](../reference/glossary.html)
- [`Codec.java`](https://github.com/smithy-lang/smithy-java/blob/main/core/src/main/java/software/amazon/smithy/java/core/serde/Codec.java)
