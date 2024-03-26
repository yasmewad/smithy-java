/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.api;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A resolved endpoint.
 */
public interface Endpoint {
    /**
     * The endpoint URI.
     *
     * @return URI of the endpoint.
     */
    URI uri();

    /**
     * Get an endpoint-specific property using a strongly typed key, or {@code null}.
     *
     * <p>For example, in some AWS use cases, this might contain HTTP headers to add to each request.
     *
     * @param key Endpoint key to get.
     * @return Returns the value or null of not found.
     */
    <T> T endpointAttribute(EndpointKey<T> key);

    /**
     * Get the attribute keys of the endpoint.
     *
     * @return the attribute keys.
     */
    Iterator<EndpointKey<?>> endpointAttributeKeys();

    /**
     * Get the list of auth scheme overrides for the endpoint.
     *
     * @return the auth schemes overrides.
     */
    List<EndpointAuthScheme> authSchemes();

    /**
     * Convert the endpoint to a builder.
     *
     * @return the builder.
     */
    default Builder toBuilder() {
        var builder = builder().uri(uri());
        endpointAttributeKeys().forEachRemaining(k -> builder.attributes.put(k, endpointAttribute(k)));
        for (EndpointAuthScheme authScheme : authSchemes()) {
            builder.addAuthScheme(authScheme);
        }
        return builder;
    }

    /**
     * Create a builder used to build an {@link Endpoint}.
     *
     * @return the created builder.
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to create an {@link Endpoint}.
     */
    final class Builder {

        private URI uri;
        private final List<EndpointAuthScheme> authSchemes = new ArrayList<>();
        private final Map<EndpointKey<?>, Object> attributes = new HashMap<>();

        private Builder() {}

        /**
         * Set the URI of the endpoint.
         *
         * @param uri URI to set.
         * @return the builder.
         */
        public Builder uri(URI uri) {
            this.uri = uri;
            return this;
        }

        /**
         * Set the URI of the endpoint.
         *
         * @param uri URI to set.
         * @return the builder.
         */
        public Builder uri(String uri) {
            try {
                return uri(new URI(uri));
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Add an auth scheme override to the endpoint.
         *
         * @param authScheme Auth scheme override to add.
         * @return the builder.
         */
        public Builder addAuthScheme(EndpointAuthScheme authScheme) {
            this.authSchemes.add(authScheme);
            return this;
        }

        /**
         * Put a typed attribute on the endpoint.
         *
         * @param key   Key to set.
         * @param value Value to associate with the key.
         * @return the builder.
         * @param <T> Value type.
         */
        public <T> Builder putAttribute(EndpointKey<T> key, T value) {
            attributes.put(key, value);
            return this;
        }

        /**
         * Create the endpoint.
         *
         * @return the created endpoint.
         */
        public Endpoint build() {
            return new Impl(this);
        }

        private static final class Impl implements Endpoint {

            private final URI uri;
            private final List<EndpointAuthScheme> authSchemes;
            private final Map<EndpointKey<?>, Object> attributes;

            private Impl(Builder builder) {
                this.uri = Objects.requireNonNull(builder.uri);
                this.authSchemes = List.copyOf(builder.authSchemes);
                this.attributes = Map.copyOf(builder.attributes);
            }

            @Override
            public URI uri() {
                return uri;
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> T endpointAttribute(EndpointKey<T> key) {
                return (T) attributes.get(key);
            }

            @Override
            public Iterator<EndpointKey<?>> endpointAttributeKeys() {
                return attributes.keySet().iterator();
            }

            @Override
            public List<EndpointAuthScheme> authSchemes() {
                return authSchemes;
            }
        }
    }
}
