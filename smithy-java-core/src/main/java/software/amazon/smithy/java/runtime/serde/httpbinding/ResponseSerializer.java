/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.serde.httpbinding;

import java.util.Objects;
import software.amazon.smithy.java.runtime.net.http.SmithyHttpResponse;
import software.amazon.smithy.java.runtime.serde.Codec;
import software.amazon.smithy.java.runtime.shapes.IOShape;
import software.amazon.smithy.java.runtime.shapes.SdkSchema;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.HttpTrait;

/**
 * Serializes HTTP responses.
 */
public final class ResponseSerializer {
    private Codec payloadCodec;
    private SdkSchema operation;
    private IOShape shapeValue;
    private final BindingMatcher bindingMatcher = BindingMatcher.responseMatcher();

    ResponseSerializer() {}

    /**
     * Schema of the operation response to serialize.
     *
     * @param operation Operation schema.
     * @return Returns the serializer.
     */
    public ResponseSerializer operation(SdkSchema operation) {
        if (operation.type() != ShapeType.OPERATION) {
            throw new IllegalArgumentException("operation must be an operation, but found " + operation);
        }
        this.operation = operation;
        return this;
    }

    /**
     * Codec to use in the payload of the response.
     *
     * @param payloadCodec Payload codec.
     * @return Returns the serializer.
     */
    public ResponseSerializer payloadCodec(Codec payloadCodec) {
        this.payloadCodec = payloadCodec;
        return this;
    }

    /**
     * Set the value of the response shape.
     *
     * @param shapeValue Response shape value to serialize.
     * @return Returns the serializer.
     */
    public ResponseSerializer shapeValue(IOShape shapeValue) {
        this.shapeValue = shapeValue;
        return this;
    }

    /**
     * Finishes setting up the serializer and creates an HTTP response.
     *
     * @return Returns the created response.
     */
    public SmithyHttpResponse serializeResponse() {
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

        return SmithyHttpResponse.builder()
                .statusCode(serializer.getResponseStatus())
                .headers(serializer.getHeaders())
                .body(serializer.getBody())
                .build();
    }
}
