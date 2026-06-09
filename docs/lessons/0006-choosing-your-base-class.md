# Lesson 6 — Choosing Your Base Class

> **smithy-java · protocol track · lesson 6**
> **Goal:** Pick RPC (`HttpClientProtocol`) vs REST (`HttpBindingClientProtocol`) correctly.
> **Time:** ~12 min · **Builds on:** Lessons 2 & 5.

The biggest decision when adding an HTTP protocol is which parent to extend. Pick right and you
inherit the correct half of the work.

---

## 1. The fork in the road

Both bases implement `ClientProtocol<HttpRequest, HttpResponse>` and both give you `id()`,
`messageExchange()`, `setServiceEndpoint()` for free. They differ in *how the request body and
members are arranged* — which determines how much of `createRequest`/`deserializeResponse` you write.

| | `HttpClientProtocol` → **RPC** | `HttpBindingClientProtocol<F>` → **REST** |
|---|---|---|
| **Body shape** | Whole input is one serialized payload | Members scatter across method/path/query/headers/body via `@http*` traits |
| **Routing** | By operation name (path or header) | URI templates + bindings |
| **You write** | `createRequest`/`deserializeResponse` (or extend a family base that did) | Almost nothing — codec, media type, error deserializer |
| **Examples** | rpcv2Cbor, AWS JSON 1.0/1.1, AWS Query, EC2 Query | restJson1, restXml |

---

## 2. What REST gives you for free

This is the entire abstract surface of `HttpBindingClientProtocol` — the binding layer already
implemented `createRequest` and `deserializeResponse` generically:

```java
public abstract class HttpBindingClientProtocol<F extends Frame<?>> extends HttpClientProtocol {
    abstract protected String payloadMediaType();                  // you supply
    abstract protected HttpErrorDeserializer getErrorDeserializer(Context ctx);  // you supply
    protected boolean omitEmptyPayload() { return false; }         // you may override

    // createRequest + deserializeResponse: ALREADY IMPLEMENTED via HttpBinding
}
```

So a REST protocol like `RestJsonClientProtocol` mostly configures a `JsonCodec` and an error
deserializer. The member-to-HTTP-location mapping (`@httpHeader`, `@httpLabel`, …) is handled by
`HttpBindingSchemaExtensions` — the extension from Lesson 5.

> Source: [`HttpBindingClientProtocol.java`](https://github.com/smithy-lang/smithy-java/blob/main/client/client-http-binding/src/main/java/software/amazon/smithy/java/client/http/binding/HttpBindingClientProtocol.java)

---

## 3. What the real protocols extend (verified)

| Protocol | Extends | Family |
|---|---|---|
| `RestJsonClientProtocol` | `HttpBindingClientProtocol<AwsEventFrame>` | REST |
| `RestXmlClientProtocol` | `HttpBindingClientProtocol<AwsEventFrame>` | REST |
| `RpcV2CborProtocol` | `AbstractRpcV2ClientProtocol` → `HttpClientProtocol` | RPC |
| `AwsJsonProtocol` (sealed) | `HttpClientProtocol` (permits 1.0 & 1.1) | RPC |
| `AwsQueryClientProtocol` | `HttpClientProtocol` | RPC |
| `Ec2QueryClientProtocol` | `HttpClientProtocol` | RPC |

These are the literal `class … extends …` lines in the repo. Pattern: anything "REST-ish" (URI
templates + scattered bindings) extends the binding base; anything "call this operation with this
body" extends the plain HTTP base.

> **Decision rule.** Ask: *"Does the Smithy model use `@http`, `@httpLabel`, `@httpQuery` on
> operations/members?"* — Yes → REST → `HttpBindingClientProtocol`. No; the operation name routes
> and the whole input is the body → RPC → `HttpClientProtocol` (and check whether a family base like
> `AbstractRpcV2ClientProtocol` already fits, so you write ~45 lines instead of two full methods).

---

## 4. The non-HTTP escape hatch

Not all protocols are HTTP. `ClientProtocol` is generic over `<RequestT, ResponseT>` precisely so
you *could* implement it directly for a different transport — your `messageExchange()` would return
a non-HTTP exchange, and the pipeline would only pair you with a matching transport (recall
`findProtocolMatchingTransport` from Lesson 5). That's a much larger undertaking; for the
overwhelming majority of new protocols, one of the two HTTP bases is the right parent.

---

## 5. Quick check

**Q1.** A model puts `@httpLabel` on path members and `@httpQuery` on others. Which base?
- a) `HttpClientProtocol` — it's still HTTP.
- b) `HttpBindingClientProtocol` — `@http*` traits mean members scatter; the binding layer handles it.
- c) Implement `ClientProtocol` directly.

**Q2.** What do you typically NOT write in a `HttpBindingClientProtocol` subclass?
- a) `payloadMediaType()`
- b) `getErrorDeserializer()`
- c) `createRequest()` and `deserializeResponse()` — the binding layer implements them.

**Q3.** AWS JSON 1.0 routes by `X-Amz-Target` and sends the full input as the JSON body. Which family?
- a) RPC (`HttpClientProtocol`) — operation-name routing + whole-body payload, no `@http*` member bindings.
- b) REST — it uses HTTP headers, so it's binding-based.
- c) Neither — it needs a custom transport.

<details><summary><strong>Answers</strong></summary>

- **Q1 → b.** HTTP binding traits ⇒ REST ⇒ `HttpBindingClientProtocol`. You inherit createRequest/deserializeResponse and supply codec + media type + error deserializer.
- **Q2 → c.** That's the whole point of the binding base: the two big methods are done.
- **Q3 → a.** Using *a* header for routing ≠ `@httpHeader` member bindings. The input is one JSON body ⇒ RPC. Indeed `AwsJsonProtocol extends HttpClientProtocol`.

</details>

---

## 6. What you now know

You can look at a protocol's Smithy model and pick its base class in one question (does it use
`@http*` member bindings?), and you know what each base hands you for free.

**Next — Lesson 7 (finale):** prove the result with protocol compliance tests, and recap the whole
add-a-protocol flow as a checklist.

## Reference
- [Glossary](../reference/glossary.html)
- [`HttpBindingClientProtocol.java`](https://github.com/smithy-lang/smithy-java/blob/main/client/client-http-binding/src/main/java/software/amazon/smithy/java/client/http/binding/HttpBindingClientProtocol.java)
