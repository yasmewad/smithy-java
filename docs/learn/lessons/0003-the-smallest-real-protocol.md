# Lesson 3 of 8 · The smallest real protocol

*Section: A concrete example · ≈ 7 min · real code · inheritance*

Lesson 2 gave you the contract. Now watch a real, shipping protocol satisfy it in about 45 lines — because a base class already did the hard parts.

In the last lesson you read `ClientProtocol` and saw the jobs a protocol must do: hand back an `id()`, hand back a `payloadCodec()`, build a request with `createRequest(...)`, and turn a response back into an object with `deserializeResponse(...)`. That is a lot of surface area. If you had to write all of it by hand for every protocol, "add a protocol" would still feel like a wall.

It isn't — because most of that work is *shared* across a family of protocols and lives in a base class. The concrete protocol on top is tiny. Let's read the smallest real one in the codebase, top to bottom.

## The whole file

File: `client/client-rpcv2-cbor/src/main/java/software/amazon/smithy/java/client/rpcv2/RpcV2CborProtocol.java`

```java
public final class RpcV2CborProtocol extends AbstractRpcV2ClientProtocol {
    private static final String PAYLOAD_MEDIA_TYPE = "application/cbor";
    private static final Codec CBOR_CODEC = Rpcv2CborCodec.builder().build();

    public RpcV2CborProtocol(ShapeId service) {
        super(Rpcv2CborTrait.ID, service, PAYLOAD_MEDIA_TYPE);
    }

    @Override
    protected Codec codec() {
        return CBOR_CODEC;
    }

    public static final class Factory implements ClientProtocolFactory<Rpcv2CborTrait> {
        @Override
        public ShapeId id() {
            return Rpcv2CborTrait.ID;
        }

        @Override
        public ClientProtocol<?, ?> createProtocol(ProtocolSettings settings, Rpcv2CborTrait trait) {
            return new RpcV2CborProtocol(
                    Objects.requireNonNull(settings.service(), "service is a required protocol setting"));
        }
    }
}
```

That is the real class, verbatim (minus the license header and imports). Count the lines that do actual work and there are only a handful. Notice what is *missing*: there is no `createRequest` here, no `deserializeResponse`, no error handling. So where did the contract go?

## The base class did the hard parts

The first line is the whole trick: `extends AbstractRpcV2ClientProtocol`. The base class is where request building and response reading actually live. Its own doc comment says so:

File: `client/client-rpcv2/src/main/java/software/amazon/smithy/java/client/rpcv2/AbstractRpcV2ClientProtocol.java`

```java
/**
 * Shared base for RPC v2 client protocol implementations.
 *
 * <p>Subclasses provide the codec and media type that distinguish the concrete wire
 * format (CBOR vs JSON). All request construction, response deserialization, error
 * extraction, and event-streaming plumbing is handled here.
 */
@SmithyInternalApi
public abstract class AbstractRpcV2ClientProtocol extends HttpClientProtocol {
```

Read that comment slowly: "*All request construction, response deserialization, error extraction, and event-streaming plumbing is handled here.*" Those are exactly the heavy `ClientProtocol` methods from Lesson 2. The base class implements `createRequest(...)` and `deserializeResponse(...)` once, for every RPC v2 protocol. The subclass inherits them for free.

The base class doesn't know *which* wire format it is serializing to — CBOR? JSON? — so it leaves exactly one hole for the subclass to fill:

```java
/** Returns the codec used for serialization and deserialization. */
protected abstract Codec codec();
```

> **The one idea to hold onto:** A base class carries the *behavior* that is common to a family of protocols — how to build a request, how to read a response. A concrete protocol supplies only its *identity* (which protocol am I, for which service, with what media type) and its *codec* (which bytes format do I speak). That's the entire division of labor in this file.

## Identity: the `super(...)` call

The constructor passes three things up to the base class:

```java
public RpcV2CborProtocol(ShapeId service) {
    super(Rpcv2CborTrait.ID, service, PAYLOAD_MEDIA_TYPE);
}
```

- `Rpcv2CborTrait.ID` — the protocol's shape ID, the answer to `id()`. This is how the model says "this service speaks rpcv2Cbor."
- `service` — which service this protocol instance is bound to.
- `PAYLOAD_MEDIA_TYPE` — the string `"application/cbor"`. That is the only thing that makes this the CBOR flavor rather than the JSON flavor.

## Codec: the one override

The single method the subclass is forced to implement just returns a cached codec:

```java
private static final Codec CBOR_CODEC = Rpcv2CborCodec.builder().build();

@Override
protected Codec codec() {
    return CBOR_CODEC;
}
```

The codec (`Rpcv2CborCodec`) is the thing that actually turns objects into CBOR bytes and back. It is built once, stored in a `static final` field, and handed back on every call. You will meet the codec layer properly in Lesson 5 — for now, it is simply "the object that knows CBOR."

> **Why cache it in a field?** The KT doc lists "*Performance by Design*" as a core value — "*library on the hot path*." Building the codec once and reusing it avoids allocating a fresh one on every request. Small file, but the same discipline as the rest of the framework.

## The nested `Factory`: how the protocol gets created

The rest of the file is a small nested class:

```java
public static final class Factory implements ClientProtocolFactory<Rpcv2CborTrait> {
```

A `ClientProtocol` handles requests; a `ClientProtocolFactory` *creates* a `ClientProtocol`. The factory interface asks for two things — an `id()` and a `createProtocol(...)` method — and this one satisfies both: it reports `Rpcv2CborTrait.ID`, and it builds a `RpcV2CborProtocol` from the service found in `ProtocolSettings`. The factory is the seam the framework calls through when it wires up a client. How the framework *finds* this factory (a file under `META-INF/services/`) is the subject of a later lesson — here, just notice that a protocol ships with its own little factory.

## What you just learned

The smallest real protocol is small on purpose. It picks a base class that already implements the hard `ClientProtocol` methods, then supplies only its identity (via `super(...)`) and its codec (via one override), plus a factory so it can be constructed. That is the pattern you will copy when you add your own. The next lesson opens up the machinery that makes this possible: *schema-directed serde*, the reason one generated class can serialize to CBOR or JSON without change.

## Self-check

**Beyond what it inherits, what does `RpcV2CborProtocol` itself supply?**

- [x] Its protocol trait ID, service, and CBOR codec **← correct**
- [ ] The full request builder plus the response reader
- [ ] The HTTP transport, socket pool, and worker threads
- [ ] The generated shape classes, builders, and field names

It passes its identity (trait `ID`, service, and `"application/cbor"`) to `super(...)` and overrides `codec()` to return the CBOR codec. Everything else — request construction, response deserialization, error extraction — is inherited from `AbstractRpcV2ClientProtocol`.

## Primary source

- `client/client-rpcv2-cbor/src/main/java/software/amazon/smithy/java/client/rpcv2/RpcV2CborProtocol.java`
- `client/client-rpcv2/src/main/java/software/amazon/smithy/java/client/rpcv2/AbstractRpcV2ClientProtocol.java`
- Protocol-family reference: https://smithy.io/2.0/

---

[← Prev: The ClientProtocol contract](0002-the-clientprotocol-contract.md) · [Track overview](index.md) · [Next: Schema-directed serde →](0004-schema-directed-serde.md)
