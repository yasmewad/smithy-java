/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.http.api;

import java.util.Objects;

/**
 * Contains the context necessary to send an HTTP request.
 */
public final class HttpClientCall {

    private final SmithyHttpRequest request;
    private final HttpProperties properties;

    private HttpClientCall(Builder builder) {
        this.request = Objects.requireNonNull(builder.request, "request is null");
        this.properties = Objects.requireNonNullElseGet(builder.properties, () -> HttpProperties.builder().build());
    }

    /**
     * Creates a builder used to build an {@link HttpClientCall}.
     *
     * @return the new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get the HTTP request to send.
     *
     * @return the HTTP request.
     */
    public SmithyHttpRequest request() {
        return request;
    }

    /**
     * Get the properties that can be used by the underlying client.
     *
     * @return the properties.
     */
    public HttpProperties properties() {
        return properties;
    }

    /**
     * Create a builder based on this HttpClientCall.
     *
     * @return the created builder.
     */
    public Builder toBuilder() {
        var builder = builder();
        builder.request(request);
        builder.properties(properties);
        return builder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HttpClientCall that = (HttpClientCall) o;
        return Objects.equals(request, that.request) && Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(request, properties);
    }

    /**
     * Builder used to create instances of {@link HttpClientCall}.
     */
    public static final class Builder {

        private SmithyHttpRequest request;
        private HttpProperties properties;

        private Builder() {}

        /**
         * Creates an {@link HttpClientCall} from the builder.
         *
         * @return the created {@link HttpClientCall}.
         */
        public HttpClientCall build() {
            return new HttpClientCall(this);
        }

        /**
         * Set the required HTTP request to send.
         *
         * @param request Request to send.
         * @return the builder.
         */
        public Builder request(SmithyHttpRequest request) {
            this.request = request;
            return this;
        }

        /**
         * Set HTTP-specific properties that are used by the underlying client.
         *
         * @param properties Properties to set.
         * @return the builder.
         */
        public Builder properties(HttpProperties properties) {
            this.properties = properties;
            return this;
        }
    }
}
