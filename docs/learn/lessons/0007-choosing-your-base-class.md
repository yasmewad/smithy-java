# Lesson 7 of 8 · Choosing your base class

**Section:** Picking your path · ≈ 7 min

There are exactly two base classes a client protocol can extend. Which one you pick is decided by a single question about your wire format — and getting it right saves you hundreds of lines.

You now know what a protocol must do: build a request from an operation, and turn a response back into an object. Before you write a line, smithy-java asks you to make one decision that shapes everything after it — *which base class do I extend?* There are only two candidates, and the answer is not a matter of taste. It falls out of how your wire format decides **where each field goes**.

## The two base classes

Every client protocol in the repo extends one of these. Both live under `client/`, and you verified in earlier lessons that a protocol is just a class satisfying the `ClientProtocol` contract. These two abstract classes each satisfy most of that contract for you — differently.

`client/client-http/src/main/java/software/amazon/smithy/java/client/http/HttpClientProtocol.java`

```java
public abstract class HttpClientProtocol implements ClientProtocol<HttpRequest, HttpResponse> {

    public HttpClientProtocol(ShapeId id) {
        this.id = id;
    }

    @Override
    public HttpRequest setServiceEndpoint(HttpRequest request, Endpoint endpoint) { ... }
}
```

`HttpClientProtocol` is the **bare** base. It handles the HTTP identity — an id, the `HttpMessageExchange`, and gluing the endpoint onto the request. It does *not* know anything about how to place your fields. That is left entirely to you: you override `createRequest` and `deserializeResponse` and serialize the whole body yourself.

`client/client-http-binding/src/main/java/software/amazon/smithy/java/client/http/binding/HttpBindingClientProtocol.java`

```java
public abstract class HttpBindingClientProtocol<F extends Frame<?>> extends HttpClientProtocol {

    private final HttpBinding httpBinding = new HttpBinding();

    @Override
    public <I extends SerializableStruct, O extends SerializableStruct> HttpRequest createRequest(...) {
        RequestSerializer serializer = httpBinding.requestSerializer()
                .operation(operation)
                .payloadCodec(payloadCodec())
                .payloadMediaType(payloadMediaType())
                .shapeValue(input)
                .endpoint(endpoint)
                .omitEmptyPayload(omitEmptyPayload());
        ...
        return serializer.serializeRequest();
    }
}
```

`HttpBindingClientProtocol` **extends** the bare base and does the field placement for you. Notice it already implements `createRequest` — it hands your input to an `HttpBinding` that reads the operation's `@http*` traits and routes each field to the right place on the wire. You only tell it the codec and media type.

> **The one idea to hold onto**
> `HttpBindingClientProtocol` *is a* `HttpClientProtocol` (it extends it) that has already written the tedious field-placement logic. Extending the binding base means opting in to automatic `@http*` trait mapping. Extending the bare base means you serialize the whole body yourself.

## What "automatic trait mapping" actually means

Lesson 4 introduced the HTTP binding traits. A REST-style protocol lets the model author annotate fields, and the binding layer places them for you:

| Trait | Where the field lands on the wire |
|---|---|
| `@httpHeader` | an HTTP header |
| `@httpQuery` | the query string |
| `@httpLabel` | a slot in the URI path |
| `@httpPayload` | the entire request body |

If your protocol honors these traits, you want the binding base — because it already knows how to read them. If your protocol ignores them and just serializes the whole input into one body, that machinery is dead weight, so you extend the bare base.

## The fork, drawn from the KT doc

Section 11 of the Knowledge Transfer doc lays out every shipping AWS protocol against its base class. This is the table to memorize:

| Protocol | Base class | Routing |
|---|---|---|
| restJson1 | `HttpBindingClientProtocol` | URI + method |
| restXml | `HttpBindingClientProtocol` | URI + method |
| AWS JSON 1.0/1.1 | `HttpClientProtocol` (direct) | `X-Amz-Target` header |
| AWS Query | `HttpClientProtocol` (direct) | `Action` form param |

The KT doc states the split plainly: *"REST protocols delegate to `HttpBinding` for automatic trait-to-HTTP mapping; RPC protocols handle serialization directly (no HTTP binding traits)."* The two columns on the right are the tell. REST protocols route by **URI + method** — each operation gets its own path, exactly what path/label/query bindings are for. RPC protocols route by a **header or a form parameter** — the whole call goes to one endpoint and a discriminator names the operation.

## See the RPC side in the code

The RPC protocols back up the table. `AwsJson1Protocol` and `AwsJson11Protocol` both extend a shared sealed base, which extends the *bare* class directly:

`aws/client/aws-client-awsjson/.../awsjson/AwsJsonProtocol.java`

```java
abstract sealed class AwsJsonProtocol extends HttpClientProtocol
        permits AwsJson1Protocol, AwsJson11Protocol {

    @Override
    public <I extends SerializableStruct, O extends SerializableStruct> HttpRequest createRequest(...) {
        var target = service.getName() + "." + operation.schema().id().getName();
        var builder = HttpRequest.create();
        builder.setMethod("POST");
        builder.setUri(endpoint);
        ...
        builder.addHeader(HeaderName.X_AMZ_TARGET, target)
    }
}
```

Because it extends the bare base, `AwsJsonProtocol` has to write its own `createRequest` — and you can see it doing exactly what the table promised: fixed `POST`, and the operation named in the `X_AMZ_TARGET` header rather than in the path. `AwsQueryClientProtocol` does the same, extending `HttpClientProtocol` directly and building a URL-encoded form body by hand.

## See the REST side in the code

`RestXmlClientProtocol` extends the binding base, so it never writes `createRequest` at all — it inherits the version you saw above. It only supplies the format-specific pieces, like the codec and media type:

`aws/client/aws-client-restxml/.../restxml/RestXmlClientProtocol.java`

```java
public final class RestXmlClientProtocol extends HttpBindingClientProtocol<AwsEventFrame> {

    @Override
    public Codec payloadCodec() { ... }

    @Override
    protected String payloadMediaType() { ... }
}
```

That short class is the payoff of picking the right base: the field-placement work is inherited, and only the format-specific pieces remain. `RestJsonClientProtocol` sits on the same binding base with JSON.

> **The decision rule**
> Ask one question: *does my wire format use HTTP binding traits to decide where each field goes?* If yes — fields scatter across path, query, headers, and payload — extend `HttpBindingClientProtocol` and let it map them. If no — you route by a header or action name and serialize one full body — extend `HttpClientProtocol` and do the serialization yourself.

> **Don't pick by format**
> It is tempting to think "JSON means the binding base." Not so: restJson1 *and* AWS JSON both use JSON, yet restJson1 extends the binding base and AWS JSON extends the bare one. The format is not the deciding factor — *field placement* is. Routing by `X-Amz-Target` is the giveaway that AWS JSON is RPC, not REST.

## Where you're headed

You have made the structural choice. In the final lesson you will prove your protocol is correct the way the framework does — with the compliance test harness the KT doc calls the authoritative correctness check, so a passing build actually means a working protocol.

## Self-check

**Q: Your new protocol routes every operation to one endpoint and names the operation in a header, serializing the whole input as one JSON body. Which base class fits, and why?**

- Binding base, since the wire format is textual JSON data
- Binding base, since it routes each request over plain HTTP
- **Bare base, since it ignores bindings and writes one body** ✅
- Bare base, since JSON payloads cannot carry any HTTP headers

The format (JSON) is a distractor — restJson1 and AWS JSON are both JSON. What decides it is field placement: routing by a header and serializing one full body is the RPC signature (no `@http*` placement), so you extend `HttpClientProtocol`, exactly like `AwsJsonProtocol` does.

## Primary source

KT doc §11 "AWS Protocol Integrations" (base-class table) and §6 "HTTP Layer" (binding traits). Verified in repo: `client/client-http/src/main/java/software/amazon/smithy/java/client/http/HttpClientProtocol.java`, `client/client-http-binding/src/main/java/software/amazon/smithy/java/client/http/binding/HttpBindingClientProtocol.java`, and the family members `AwsJsonProtocol.java`, `AwsQueryClientProtocol.java`, `RestJsonClientProtocol.java`, `RestXmlClientProtocol.java` under `aws/client/`. See also the [smithy.io HTTP binding traits](https://smithy.io/2.0/spec/http-bindings.html).

---

← Prev: [Extensions & discovery](0006-extensions-and-discovery.md) · [Track overview](index.md) · Next: [Proving it: compliance tests](0008-proving-it-compliance-tests.md) →
