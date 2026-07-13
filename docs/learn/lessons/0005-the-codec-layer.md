# Lesson 5 of 8 · The codec layer

> Section: Reuse vs write · ≈ 7 min
>
> A codec turns schema-carrying structures into bytes and back. Most new
> protocols reuse one — here is how to tell when you must write one instead.

Last lesson you saw the trick that makes protocols pluggable: generated code
*pushes* its fields into a `ShapeSerializer`, and a protocol-specific
implementation *interprets* them into wire bytes. This lesson names the thing
that produces those serializers and deserializers — the **Codec** — and answers
the first real decision you face when adding a protocol: *do I reuse an existing
codec, or write a new one?*

## What a codec is

The KT doc puts it in one sentence: *"Codecs are the bridge between structures
(which carry schemas) and bytes."* A structure knows *what* it holds (its
schema); the codec knows how to spell that out in a particular wire format. It
does not do this itself — it hands you the two visitors you met last lesson.

From `core/src/main/java/software/amazon/smithy/java/core/serde/Codec.java`:

```java
public interface Codec extends AutoCloseable {

    ShapeSerializer createSerializer(OutputStream sink);

    ShapeDeserializer createDeserializer(ByteBuffer source);

    default ByteBuffer serialize(SerializableShape shape) { ... }

    default <T extends SerializableShape> T deserializeShape(
            ByteBuffer source, ShapeBuilder<T> builder) { ... }
}
```

Read it top to bottom. The two abstract methods are the whole contract: give me
an `OutputStream` and I hand back a `ShapeSerializer` that writes your format
into it; give me a `ByteBuffer` and I hand back a `ShapeDeserializer` that reads
your format out of it. The `default` methods (`serialize`, `deserializeShape`)
are convenience wrappers built on those two — you get them for free.

> **The one idea to hold onto.** A codec is the piece that knows a *wire format*
> — JSON, CBOR, XML — and nothing else. It does not know about HTTP, auth, or
> which operation you called. That narrowness is exactly why it is reusable
> across protocols.

## The codecs that already ship

smithy-java already implements three. Each is a `final class ... implements
Codec`, and each owns one format:

| Codec | Wire format | Module |
|---|---|---|
| `JsonCodec` | JSON (text) | `codecs/json-codec` |
| `Rpcv2CborCodec` | CBOR (compact binary) | `codecs/cbor-codec` |
| `XmlCodec` | XML (text) | `codecs/xml-codec` |

## Two different axes

Here is the mental model that keeps this from getting confusing. There is a
**protocol axis** and a **codec axis**, and they move independently.

- The **protocol** decides *routing and envelope*: which URL, which HTTP method,
  which header names the operation goes in.
- The **codec** decides *the shape bytes*: how a structure and its fields are
  spelled out in the body.

Because these are separate, a new protocol very often keeps the body format of
an old one and only changes the routing. When that happens, you *reuse* the
existing codec — you write no serialization code at all.

> **Reuse in the real code.** `RpcV2CborProtocol` is a whole protocol, yet it
> writes no CBOR itself. It reaches for the shipped codec:
> `private static final Codec CBOR_CODEC = Rpcv2CborCodec.builder().build();`
> and its `codec()` method just returns it. `RestJsonClientProtocol` does the
> same with `JsonCodec`. Two different protocols, zero new codecs.

From `client/client-rpcv2-cbor/src/main/java/software/amazon/smithy/java/client/rpcv2/RpcV2CborProtocol.java`:

```java
public final class RpcV2CborProtocol extends AbstractRpcV2ClientProtocol {
    private static final Codec CBOR_CODEC = Rpcv2CborCodec.builder().build();

    @Override
    protected Codec codec() {
        return CBOR_CODEC;
    }
}
```

## When *do* you write a new codec?

Only for a genuinely new wire format — a body encoding none of the three shipped
codecs produces. If your protocol speaks JSON, CBOR, or XML, you are reusing. If
it speaks some other encoding no existing codec can emit, then and only then do
you implement `Codec`, `ShapeSerializer`, and `ShapeDeserializer` in a new
module. The KT doc lists this as its own multi-step workflow — a bigger job than
adding a protocol that reuses a codec.

> **Don't reach for a new codec too early.** Writing a codec means
> re-implementing the entire serialize/deserialize visitor for a format. It is
> the framework's tenet of *simplicity* — "fewer moving parts" — that says:
> reuse first. A new protocol over an existing format should touch a `codec()`
> method, not a new module.

## Where you're headed

You now know the reuse-vs-write decision and can spot it in the code: a protocol
either returns a shipped codec or, rarely, brings its own. Next lesson covers the
other half of the wiring — *schema extensions* (the precomputed per-format data a
codec reads) and how the framework *discovers* your protocol at runtime through
SPI.

## Self-check

**In Smithy Java, you are adding a protocol whose body is CBOR. What should you
do about the codec?**

- [x] Reuse the CBOR codec the framework ships **← correct**
- [ ] Write a new codec for CBOR yourself
- [ ] Change the JSON codec to emit CBOR
- [ ] Remove every codec and hand-roll bytes

CBOR is already a shipped format, so you reuse `Rpcv2CborCodec` exactly as
`RpcV2CborProtocol` does — return it from `codec()`. You write a new codec only
for a format none of the three can emit.

## Primary source

Verified in
`core/src/main/java/software/amazon/smithy/java/core/serde/Codec.java` and
`client/client-rpcv2-cbor/src/main/java/software/amazon/smithy/java/client/rpcv2/RpcV2CborProtocol.java`.
Codec overview: KT doc §5 (Codecs). Framework guidance:
<https://smithy.io/2.0/>.

---

- [← Prev: Schema-directed serde](0004-schema-directed-serde.md)
- [Track overview](index.md)
- [Next: Extensions & discovery →](0006-extensions-and-discovery.md)
