/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates endpoint provider request parameters.
 */
public final class EndpointProviderRequest {

    private final Map<EndpointKey<?>, Object> immutableMap;

    private EndpointProviderRequest(Map<EndpointKey<?>, Object> map) {
        this.immutableMap = new HashMap<>(map);
    }

    /**
     * Create a new EndpointProviderRequest.
     *
     * @return the created request.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get an auth scheme-specific property using a strongly typed key, or {@code null}.
     *
     * @param key Key of the property to get.
     * @return Returns the value or null of not found.
     */
    @SuppressWarnings("unchecked")
    public <T> T attribute(EndpointKey<T> key) {
        return (T) immutableMap.get(key);
    }

    /**
     * Get all the keys registered with the request.
     *
     * @return the keys of the request.
     */
    public Set<EndpointKey<?>> attributeKeys() {
        return immutableMap.keySet();
    }

    /**
     * Create a builder from this request.
     *
     * @return the builder.
     */
    public Builder toBuilder() {
        Builder builder = builder();
        builder.map.putAll(immutableMap);
        return builder;
    }

    /**
     * Builder used to create and EndpointProviderRequest.
     */
    public static final class Builder {

        private final Map<EndpointKey<?>, Object> map = new HashMap<>();

        /**
         * Build the request.
         * @return the built request.
         */
        public EndpointProviderRequest build() {
            return new EndpointProviderRequest(map);
        }

        /**
         * Put an attribute on the request.
         *
         * @param key   Key to set.
         * @param value Value to set.
         * @return the builder.
         * @param <T> value type stored in the key.
         */
        public <T> Builder putAttribute(EndpointKey<T> key, T value) {
            map.put(key, value);
            return this;
        }
    }
}
