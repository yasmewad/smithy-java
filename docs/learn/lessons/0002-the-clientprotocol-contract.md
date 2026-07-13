# Lesson 2 of 8 · The contract — The `ClientProtocol` contract

> Prefer the interactive version? [Read the HTML lesson](0002-the-clientprotocol-contract.html).
> Track overview: [index.md](index.md)

There is exactly one interface a client-side protocol must satisfy. This lesson
meets its six members, in plain language, and shows where each one fires when a
client makes a call.

In Lesson 1 you learned that a *protocol* is the set of rules for turning an
operation call into bytes and turning response bytes back into an object.
smithy-java gathers *all* of those rules behind a single Java interface. Satisfy
it, and the client runtime knows how to drive your protocol. The interface is
`ClientProtocol<RequestT, ResponseT>`, and it is short enough to read in one
sitting.

## What the two type parameters mean

`RequestT` and `ResponseT` are the *transport* request and response types — for
an HTTP protocol they would be an HTTP request and an HTTP response. The
interface is generic so the same contract works whether the wire is HTTP, or
something else entirely.

## The interface, verbatim

File: `client/client-core/src/main/java/software/amazon/smithy/java/client/core/ClientProtocol.java`

```java
public interface ClientProtocol<RequestT, ResponseT> {
    ShapeId id();

    Codec payloadCodec();

    MessageExchange<RequestT, ResponseT> messageExchange();

    <I extends SerializableStruct, O extends SerializableStruct> RequestT createRequest(
            ApiOperation<I, O> operation, I input, Context context, SmithyUri endpoint);

    RequestT setServiceEndpoint(RequestT request, Endpoint endpoint);

    <I extends SerializableStruct, O extends SerializableStruct> O deserializeResponse(
            ApiOperation<I, O> operation, Context context, TypeRegistry errorRegistry,
            RequestT request, ResponseT response);
}
```

The interface's own Javadoc sums up its job in one line: *"Handles request and
response serialization."* Six members do that job.

## The three describe-yourself members

The first three members ask the protocol to *describe itself*. They take no
work-in-progress data; they just report facts about the protocol.

### 1 · `ShapeId id()`

Returns the protocol's identity — its name in the Smithy world. The Javadoc gives
the example `aws.protocols#restJson1`. A `ShapeId` is Smithy's type for such a
fully-qualified name. This is how the runtime knows *which* protocol you are.

### 2 · `Codec payloadCodec()`

Returns the **codec** — the thing that reads and writes the message *body* (JSON,
CBOR, XML). A whole later lesson covers codecs; for now, note one detail straight
from the Javadoc: it may be `null`. The comment reads *"the payload codec of the
protocol, or null if the protocol has no payload codec."* Some protocols put
everything in headers or the URI and have no body to encode, so returning `null`
is legal.

> **Don't assume a codec exists.** Because `payloadCodec()` is allowed to return
> `null`, code that consumes a protocol must not blindly dereference it. The "may
> be null" is part of the contract, not an accident.

### 3 · `MessageExchange<RequestT, ResponseT> messageExchange()`

Returns a **marker** that says what request/response pair this protocol speaks.
The interface itself is tiny — its entire body is empty:

File: `client/client-core/src/main/java/software/amazon/smithy/java/client/core/MessageExchange.java`

```java
public interface MessageExchange<RequestT, ResponseT> {}
```

**Why an empty interface earns its keep.** The KT doc calls this *"protocol
agnosticism via `MessageExchange`"*: protocols and transports "declare a
`MessageExchange<Req, Resp>` and the pipeline validates compatibility at
construction." In plain terms: it lets the runtime confirm that a protocol and a
transport agree on the same request/response types *before* any call is made,
rather than crashing mid-flight.

## The three do-the-work members

The last three members do the actual serialization work, and they line up with
three moments in a single client call.

### 4 · `createRequest(operation, input, context, endpoint)`

The outbound half. Given the `operation` being called, the caller's `input`
shape, a `Context` (a typed key-value bag threaded through the call), and an
`endpoint`, it *builds the transport request*. Its Javadoc: *"Creates the
underlying transport request."* This is where your input object first becomes
something shaped like a real request.

### 5 · `setServiceEndpoint(request, endpoint)`

A small but important second step. The runtime first builds a request against a
placeholder endpoint, then — once the *real* service endpoint is resolved — hands
the request back to the protocol to be re-pointed at it. The Javadoc explains the
combining rule: *"any existing endpoint information on the request should be
combined with the service endpoint … with HTTP protocols, any path found on the
request should be concatenated to the end of the service endpoint's path."*

### 6 · `deserializeResponse(operation, context, errorRegistry, request, response)`

The inbound half. It takes the transport `response` and returns the typed output
shape `O` — *or throws*. Its Javadoc: *"Deserializes the output from the transport
response or throws a modeled or unmodeled exception."* The `errorRegistry` (a
`TypeRegistry`) lets it build a *modeled* error when the service returns one; if
the registry is `null` or doesn't know the shape, the protocol falls back to
*"protocol hints (e.g., HTTP status codes)."*

## Where each member fires in `Client.call(...)`

The KT doc sketches the "Call Pipeline." Three of the six members are called
directly along it, and the codebase shows the exact spots:

| Step in the call | Member invoked | Verified at |
|---|---|---|
| Serialize the input into a request | `createRequest(...)` | `ClientPipeline.java:123` |
| After the real endpoint resolves | `setServiceEndpoint(...)` | `ClientPipeline.java:188` |
| Turn the response back into output | `deserializeResponse(...)` | `ClientPipeline.java:410` |

Reading the pipeline top to bottom, from the KT doc's sketch:

```
Client.call(input, operation, overrideConfig)
 → apply plugins/overrides → create pipeline
 → protocol.createRequest(operation, input)
 → [RETRY LOOP]: resolveAuthScheme → resolveEndpoint → sign → transport.send()
     → protocol.deserializeResponse() → if retryable: refreshToken → backoff → retry
 → interceptor.readAfterExecution
```

So `createRequest` runs once, up front. Then inside the retry loop the endpoint
gets resolved, `setServiceEndpoint` re-points the request at it, the transport
sends, and `deserializeResponse` turns the reply into your output — or throws so
the loop can decide whether to retry. The other three members (`id`,
`payloadCodec`, `messageExchange`) are read at wiring time, not on every call.

## A factory sits right beside it

In the same folder lives `ClientProtocolFactory.java`. Its Javadoc: *"Creates a
`ClientProtocol`."* It has an `id()` and a `createProtocol(settings, trait)`
method. That is the seam the client builder uses to *construct* your protocol
from a Smithy trait — we return to it when we wire a protocol up for discovery.

## Self-check

**Q: Which member turns the transport response back into a typed output shape (or throws)?**

- createRequest assembles the outbound transport request to send
- setServiceEndpoint merges the request with the service endpoint
- **deserializeResponse turns the response bytes into typed output** ← correct
- payloadCodec identifies the codec serializing request payloads

`deserializeResponse` is the inbound half — its Javadoc says it "deserializes the
output from the transport response or throws a modeled or unmodeled exception."

## Primary source

- Interface read verbatim: `client/client-core/src/main/java/software/amazon/smithy/java/client/core/ClientProtocol.java`
- Marker interface: `client/client-core/src/main/java/software/amazon/smithy/java/client/core/MessageExchange.java`
- Pipeline call sites: `client/client-core/src/main/java/software/amazon/smithy/java/client/core/ClientPipeline.java` lines 123, 188, 410
- Call-pipeline sketch and "protocol agnosticism via `MessageExchange`": internal *Smithy Java Knowledge Transfer* doc (`resources/smithy-java-kt.md`)

---

← Prev: [What a protocol even is](0001-what-is-a-protocol.md) ·
Next: [The smallest real protocol](0003-the-smallest-real-protocol.md) →
