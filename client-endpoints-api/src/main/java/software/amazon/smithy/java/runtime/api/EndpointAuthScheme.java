/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.api;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * An authentication scheme supported for the endpoint.
 */
public interface EndpointAuthScheme {
    /**
     * The ID of the auth scheme (e.g., "aws.auth#sigv4").
     *
     * @return the auth scheme ID.
     */
    String authSchemeId();

    /**
     * Get an auth scheme-specific property using a strongly typed key, or {@code null}.
     *
     * @param key Key of the property to get.
     * @return Returns the value or null of not found.
     */
    <T> T attribute(EndpointKey<T> key);

    /**
     * Get the attribute keys of the auth scheme.
     *
     * @return the attribute keys.
     */
    Iterator<EndpointKey<?>> attributeKeys();

    /**
     * Convert the EndpointAuthScheme to a builder.
     *
     * @return the created builder.
     */
    default Builder toBuilder() {
        Builder builder = builder().authSchemeId(authSchemeId());
        attributeKeys().forEachRemaining(k -> builder.attributes.put(k, attribute(k)));
        return builder;
    }

    /**
     * Create a builder for {@link EndpointAuthScheme}.
     *
     * @return the builder.
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to create an {@link EndpointAuthScheme}.
     */
    final class Builder {

        private String authSchemeId;
        private final Map<EndpointKey<?>, Object> attributes = new HashMap<>();

        /**
         * Set the auth scheme ID.
         *
         * @param authSchemeId Auth scheme ID to set.
         * @return the builder.
         */
        public Builder authSchemeId(String authSchemeId) {
            this.authSchemeId = authSchemeId;
            return this;
        }

        /**
         * Add an attribute to the EndpointAuthScheme.
         *
         * @param key   Ket to set.
         * @param value Value to set.
         * @return the builder.
         * @param <T> the value type.
         */
        public <T> Builder putAttribute(EndpointKey<T> key, T value) {
            attributes.put(key, value);
            return this;
        }

        /**
         * Create the {@link EndpointAuthScheme}.
         *
         * @return the created EndpointAuthScheme.
         */
        EndpointAuthScheme build() {
            return new Impl(this);
        }

        private static final class Impl implements EndpointAuthScheme {
            private final String authSchemeId;
            private final Map<EndpointKey<?>, Object> attributes;

            private Impl(Builder builder) {
                this.authSchemeId = Objects.requireNonNull(builder.authSchemeId, "authSchemeId is null");
                this.attributes = Map.copyOf(builder.attributes);
            }

            @Override
            public String authSchemeId() {
                return authSchemeId;
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> T attribute(EndpointKey<T> key) {
                return (T) attributes.get(key);
            }

            @Override
            public Iterator<EndpointKey<?>> attributeKeys() {
                return attributes.keySet().iterator();
            }
        }
    }
}
