/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.serde.httpbinding;

import java.net.URI;
import java.util.Objects;
import software.amazon.smithy.java.runtime.net.http.SmithyHttpRequest;
import software.amazon.smithy.java.runtime.net.uri.URIBuilder;
import software.amazon.smithy.java.runtime.serde.Codec;
import software.amazon.smithy.java.runtime.shapes.IOShape;
import software.amazon.smithy.java.runtime.shapes.SdkSchema;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.HttpTrait;

/**
 * Serializes an HTTP request from an input shape that uses HTTP binding traits.
 */
public final class RequestSerializer {

    private Codec payloadCodec;
    private SdkSchema operation;
    private URI endpoint;
    private IOShape shapeValue;
    private final BindingMatcher bindingMatcher = new BindingMatcher(true);

    RequestSerializer() {}

    /**
     * Schema of the operation to serialize.
     *
     * @param operation Operation schema.
     * @return Returns the serializer.
     */
    public RequestSerializer operation(SdkSchema operation) {
        if (operation.type() != ShapeType.OPERATION) {
            throw new IllegalArgumentException("operation must be an operation, but found " + operation);
        }
        this.operation = operation;
        return this;
    }

    /**
     * Codec to use in the payload of requests.
     *
     * @param payloadCodec Payload codec.
     * @return Returns the serializer.
     */
    public RequestSerializer payloadCodec(Codec payloadCodec) {
        this.payloadCodec = payloadCodec;
        return this;
    }

    /**
     * Set the endpoint of the request.
     *
     * @param endpoint Request endpoint.
     * @return Returns the serializer.
     */
    public RequestSerializer endpoint(URI endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    /**
     * Set the value of the request shape.
     *
     * @param shapeValue Request shape value to serialize.
     * @return Returns the serializer.
     */
    public RequestSerializer shapeValue(IOShape shapeValue) {
        this.shapeValue = shapeValue;
        return this;
    }

    /**
     * Finishes setting up the serializer and creates an HTTP request.
     *
     * @return Returns the created request.
     */
    public SmithyHttpRequest serializeRequest() {
        Objects.requireNonNull(shapeValue, "shapeValue is not set");
        Objects.requireNonNull(operation, "operation is not set");
        Objects.requireNonNull(payloadCodec, "payloadCodec is not set");
        Objects.requireNonNull(payloadCodec, "endpoint is not set");
        Objects.requireNonNull(payloadCodec, "value is not set");

        var httpTrait = operation.expectTrait(HttpTrait.class);
        var serializer = new HttpBindingSerializer(httpTrait, payloadCodec, bindingMatcher);
        shapeValue.serialize(serializer);
        shapeValue.serializeStream(serializer);
        serializer.flush();

        var uriBuilder = URIBuilder.of(endpoint);

        // Append the path using simple concatenation, not using RFC 3986 resolution.
        uriBuilder.concatPath(serializer.getPath());

        if (serializer.hasQueryString()) {
            uriBuilder.query(serializer.getQueryString());
        }

        var targetEndpoint = uriBuilder.build();

        return SmithyHttpRequest.builder()
                .method(httpTrait.getMethod())
                .uri(targetEndpoint)
                .headers(serializer.getHeaders())
                .body(serializer.getBody())
                .build();
    }
}
