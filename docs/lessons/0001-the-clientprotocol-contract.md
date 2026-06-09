# Lesson 1 — The `ClientProtocol` Contract

> **smithy-java · protocol track · lesson 1**
> **Goal:** Know the 6 methods of `ClientProtocol` and trace one call through them.
> **Time:** ~10 min · **Mission:** deep architecture mastery, using "add a new protocol" as the lens.

This is the non-interactive companion to the HTML lesson. Before you can *add* a protocol to
smithy-java, you need to know exactly what a protocol *is* in this framework. It's one interface
with six methods. Learn them, and you have the skeleton you'll flesh out for the rest of the track.

---

## 1. The one-sentence definition

A **protocol** is the thing that knows how to turn a typed *input shape* into a transport
*request*, and a transport *response* back into a typed *output shape*. That's it. It is the
translator between "Java objects the user holds" and "bytes on the wire."

In code, that's a single interface, generic over the request and response types it produces:

```java
public interface ClientProtocol<RequestT, ResponseT> {
    ShapeId id();                          // which protocol trait am I?
    Codec payloadCodec();                  // how do I encode bodies?
    MessageExchange<RequestT, ResponseT> messageExchange();

    RequestT createRequest(operation, input, context, endpoint);     // shape → request
    RequestT setServiceEndpoint(request, endpoint);                  // attach where to send
    O        deserializeResponse(operation, ctx, errors, req, resp); // response → shape
}
```

> Source (verbatim signatures):
> [`client/client-core/.../client/core/ClientProtocol.java`](https://github.com/smithy-lang/smithy-java/blob/main/client/client-core/src/main/java/software/amazon/smithy/java/client/core/ClientProtocol.java)

**Why generic over `RequestT`/`ResponseT`?** Because a protocol isn't tied to HTTP. The type
parameters let an HTTP protocol work with `HttpRequest`/`HttpResponse` while leaving the door open
for other transports. The `MessageExchange` marker is how the pipeline checks that a protocol and a
transport actually speak the same request/response types before wiring them together.

---

## 2. The six methods, by job

Split them into three that **describe** the protocol and three that **do the work**:

| Method | Job | Think of it as… |
|---|---|---|
| `id()` | Returns the protocol trait's `ShapeId`, e.g. `smithy.protocols#rpcv2Cbor`. | "My name." The join key to the Smithy model. |
| `payloadCodec()` | Returns the `Codec` used to encode/decode bodies (JSON, CBOR, XML…). | "My encoder." Often a single shared static instance. |
| `messageExchange()` | Declares the request/response pair (e.g. HTTP). | "What transport I'm compatible with." |
| `createRequest(...)` | Build the transport request from the operation + input shape. Serialize the body, set method/path/headers. | **The outbound half.** |
| `setServiceEndpoint(...)` | Merge the resolved endpoint (host/path) into the request. | The "where to send it" step. |
| `deserializeResponse(...)` | Turn the response into the output shape — or throw a modeled/unmodeled error. | **The inbound half.** |

> **The good news for HTTP protocols:** you almost never implement all six yourself.
> `HttpClientProtocol` already gives you `id()`, `messageExchange()`, and `setServiceEndpoint()`.
> You're left writing the two interesting ones — `createRequest` and `deserializeResponse` — plus
> pointing `payloadCodec()` at a codec. We'll prove this in Lesson 2.

---

## 3. Trace one call through the contract

Here's where the six methods sit in the life of a single `client.call(input, operation)`. The
**protocol methods are bold**; the rest is pipeline machinery you get for free.

1. **You call** `client.call(input, operation)` with a generated input shape.
2. The pipeline calls **`protocol.createRequest(operation, input, ctx, endpoint)`** → serializes `input` through `payloadCodec()` into a `RequestT`.
3. Endpoint resolution runs, then **`protocol.setServiceEndpoint(request, endpoint)`** attaches host + base path.
4. Auth resolves & signs the request (separate from the protocol).
5. The **transport** sends the request and returns a `ResponseT`.
6. The pipeline calls **`protocol.deserializeResponse(operation, ctx, errors, req, resp)`** → output shape, or a thrown error.
7. If retryable, the loop repeats from step 3. Otherwise you get your output shape back.

```
client.call(input, operation)
        │
        ▼
  ┌─────────────────────┐
  │ protocol.createRequest │  ◄── shape → request   (PROTOCOL)
  └─────────────────────┘
        │
        ▼   resolve endpoint → protocol.setServiceEndpoint (PROTOCOL)
        ▼   resolve auth → sign
        ▼   transport.send() ──────────► returns ResponseT
        │
        ▼
  ┌──────────────────────────┐
  │ protocol.deserializeResponse │  ◄── response → shape  (PROTOCOL)
  └──────────────────────────┘
        │
        ▼   retryable? ── yes ──► back to setServiceEndpoint
        │
        ▼   output shape
```

This mirrors the call-pipeline sketch in the Knowledge-Transfer doc (§8 "Call Pipeline") — now you
can see which steps are *the protocol's job* and which are the pipeline's.

---

## 4. Quick check

Test yourself. Answers are below — no peeking until you've committed.

**Q1.** You're adding an HTTP-based protocol. Which two methods will you almost always have to write yourself?
- a) `id()` and `payloadCodec()`
- b) `createRequest()` and `deserializeResponse()`
- c) `setServiceEndpoint()` and `messageExchange()`

**Q2.** What does `id()` return, and why does it matter?
- a) A random UUID, so two protocols never collide at runtime.
- b) The Java class name, used for logging only.
- c) The protocol trait's `ShapeId` — the join key between the Smithy model and the factory.

**Q3.** In the call lifecycle, what happens *between* `createRequest` and `deserializeResponse`?
- a) Endpoint resolution, signing, and the transport actually sending the request.
- b) Nothing — `createRequest` sends the bytes itself.
- c) The codec is chosen for the first time.

<details>
<summary><strong>Show answers</strong></summary>

- **Q1 → b.** The two "do the work" halves — outbound (`createRequest`) and inbound
  (`deserializeResponse`) — carry the protocol's real logic. `id()` returns a constant and
  `payloadCodec()` usually just returns a shared codec instance; the base class handles
  `setServiceEndpoint()` and `messageExchange()`.
- **Q2 → c.** `id()` returns something like `smithy.protocols#rpcv2Cbor`. That's how the framework
  matches a service's protocol trait to *your* protocol. Used in the discovery lesson.
- **Q3 → a.** The protocol builds the request; the pipeline then resolves the endpoint, signs, and
  hands it to the transport to send. The protocol re-enters only to deserialize the response.
  Keeping "build" and "send" separate is what lets you swap transports.

</details>

---

## 5. What you now know

You can state what a protocol is, name its six methods and their jobs, and point to where each one
sits in a single call. That's the scaffold. In **Lesson 2** we'll read the smallest real protocol
in the repo — `RpcV2CborProtocol`, which is literally ~45 lines — and watch this contract get
satisfied by inheritance, so you see how little code a new protocol actually needs.

---

## Reference

- **Glossary:** [`reference/glossary.html`](../reference/glossary.html) — canonical vocabulary for this track.
- **The interface:** [`ClientProtocol.java`](https://github.com/smithy-lang/smithy-java/blob/main/client/client-core/src/main/java/software/amazon/smithy/java/client/core/ClientProtocol.java)
- **HTTP base class:** [`HttpClientProtocol.java`](https://github.com/smithy-lang/smithy-java/blob/main/client/client-http/src/main/java/software/amazon/smithy/java/client/http/HttpClientProtocol.java)
- **Smallest real protocol (Lesson 2 preview):** [`RpcV2CborProtocol.java`](https://github.com/smithy-lang/smithy-java/blob/main/client/client-rpcv2-cbor/src/main/java/software/amazon/smithy/java/client/rpcv2/RpcV2CborProtocol.java)

*This is a teaching lesson generated for architecture onboarding. Signatures are quoted from the
smithy-java `main` branch.*
