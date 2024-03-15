/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.serde.httpbinding;

import java.io.InputStream;
import java.net.http.HttpHeaders;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import software.amazon.smithy.java.runtime.net.StoppableInputStream;
import software.amazon.smithy.java.runtime.serde.Codec;
import software.amazon.smithy.java.runtime.serde.ShapeDeserializer;
import software.amazon.smithy.java.runtime.serde.SpecificShapeDeserializer;
import software.amazon.smithy.java.runtime.shapes.IOShape;
import software.amazon.smithy.java.runtime.shapes.SdkSchema;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * Generic HTTP binding deserializer that delegates to another ShapeDeserializer when members are encountered that
 * form a protocol-specific body.
 *
 * <p>This deserializer requires that a top-level structure shape is deserialized and will throw an
 * UnsupportedOperationException if any other kind of shape is first read from it.
 */
final class HttpBindingDeserializer extends SpecificShapeDeserializer implements IOShape.Deserializer {

    private final Codec payloadCodec;
    private final HttpHeaders headers;
    private final String requestRawQueryString;
    private final InputStream body;
    private final int responseStatus;
    private final String requestPath;
    private final Map<String, String> requestPathLabels;
    private final BindingMatcher bindingMatcher;

    private HttpBindingDeserializer(Builder builder) {
        this.payloadCodec = Objects.requireNonNull(builder.payloadCodec, "payloadSerializer not set");
        this.headers = Objects.requireNonNull(builder.headers, "headers not set");
        this.bindingMatcher = builder.isRequest ? BindingMatcher.requestMatcher() : BindingMatcher.responseMatcher();
        this.body = builder.body == null ? InputStream.nullInputStream() : builder.body;
        this.requestPath = builder.requestPath;
        this.requestRawQueryString = builder.requestRawQueryString;
        this.responseStatus = builder.responseStatus;
        this.requestPathLabels = builder.requestPathLabels;
    }

    static Builder builder() {
        return new Builder();
    }

    @Override
    protected RuntimeException throwForInvalidState(SdkSchema schema) {
        throw new IllegalStateException("Expected to parse a structure for HTTP bindings, but found " + schema);
    }

    @Override
    public void readStruct(SdkSchema schema, BiConsumer<SdkSchema, ShapeDeserializer> eachEntry) {
        List<String> bodyMembers = new ArrayList<>();

        // First parse members in the framing.
        for (SdkSchema member : schema.members()) {
            switch (bindingMatcher.match(member)) {
                case LABEL -> throw new UnsupportedOperationException("httpLabel binding not supported yet");
                case QUERY -> throw new UnsupportedOperationException("httpQuery binding not supported yet");
                case HEADER -> headers.firstValue(bindingMatcher.header())
                        .ifPresent(headerValue -> eachEntry.accept(member, new HttpHeaderDeserializer(headerValue)));
                case BODY -> bodyMembers.add(member.id().getName());
                case PAYLOAD -> {
                    if (member.memberTarget().type() == ShapeType.STRUCTURE) {
                        eachEntry.accept(member, payloadCodec.createDeserializer(body));
                    }
                }
            }
        }

        // Now parse members in the payload of body.
        if (!bodyMembers.isEmpty()) {
            // Extract from the payload codec and exclude members from other locations.
            SdkSchema payloadOnly = schema.withFilteredMembers(member -> !bodyMembers.contains(member.id().getName()));
            payloadCodec.createDeserializer(body).readStruct(payloadOnly, eachEntry);
        }
    }

    @Override
    public StoppableInputStream readStream(SdkSchema schema) {
        return StoppableInputStream.of(body);
    }

    @Override
    public Object readEventStream(SdkSchema schema) {
        throw new UnsupportedOperationException("Event streams are not yet implemented");
    }

    static final class Builder implements SmithyBuilder<HttpBindingDeserializer> {
        private Map<String, String> requestPathLabels;
        private Codec payloadCodec;
        private boolean isRequest;
        private HttpHeaders headers;
        private String requestRawQueryString;
        private InputStream body;
        private String requestPath;
        private int responseStatus;

        private Builder() {}

        @Override
        public HttpBindingDeserializer build() {
            return new HttpBindingDeserializer(this);
        }

        /**
         * Set the captured, already percent-decoded, labels for the operation.
         *
         * <p>This builder assumes an operation has already been matched by a framework, which means HTTP label
         * bindings have already been extracted.
         *
         * @param requestPathLabels Captured request labels.
         * @return Returns the builder.
         */
        Builder requestPathLabels(Map<String, String> requestPathLabels) {
            this.requestPathLabels = requestPathLabels;
            return this;
        }

        /**
         * Set the codec used to deserialize the body if necessary.
         *
         * @param payloadCodec Payload codec.
         * @return Returns the builder.
         */
        Builder payloadCodec(Codec payloadCodec) {
            this.payloadCodec = payloadCodec;
            return this;
        }

        /**
         * Set HTTP headers to use when deserializing the message.
         *
         * @param headers HTTP headers to set.
         * @return Returns the builder.
         */
        Builder headers(HttpHeaders headers) {
            this.headers = Objects.requireNonNull(headers);
            return this;
        }

        /**
         * Set the raw query string of the request.
         *
         * <p>The query string should be percent-encoded and include any relevant parameters.
         * For example, "foo=bar&baz=bam%20boo".
         *
         * @param requestRawQueryString Query string.
         * @return Returns the builder.
         */
        Builder requestRawQueryString(String requestRawQueryString) {
            if (!isRequest) {
                throw new IllegalStateException("Cannot set rawQueryString for a response");
            }
            this.requestRawQueryString = requestRawQueryString;
            return this;
        }

        /**
         * Set the body of the message, if any.
         *
         * @param body Payload to deserialize.
         * @return Returns the builder.
         */
        Builder body(InputStream body) {
            this.body = body;
            return this;
        }

        /**
         * Set the raw path of a request as received on the wire.
         *
         * @param requestPath Path of the request.
         * @return Returns the path.
         */
        Builder requestPath(String requestPath) {
            if (!isRequest) {
                throw new IllegalStateException("Cannot set path for a response");
            }
            this.requestPath = requestPath;
            return this;
        }

        /**
         * Set the HTTP status code of a response.
         *
         * @param responseStatus Status to set.
         * @return Returns the builder.
         */
        Builder responseStatus(int responseStatus) {
            if (isRequest) {
                throw new IllegalStateException("Cannot set status for a request");
            }
            this.responseStatus = responseStatus;
            return this;
        }

        /**
         * Set to true to honor request bindings.
         *
         * @param isRequest Set to true to use request bindings.
         * @return Returns the builder.
         */
        Builder request(boolean isRequest) {
            this.isRequest = isRequest;
            return this;
        }
    }
}
