/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.http.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * An immutable collection of HTTP-related properties.
 *
 * <p>Keys in the property bag are identity-based and associated with a strongly typed value.
 */
public final class HttpProperties {

    private final Map<HttpProperty<?>, Object> attributes;

    private HttpProperties(Map<HttpProperty<?>, Object> attributes) {
        this.attributes = attributes;
    }

    /**
     * Creates a builder used to build {@link HttpProperties}.
     *
     * @return the builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get a property.
     *
     * @param key   property key to get by exact reference identity.
     * @param <T>   Returns the value, or null if not present.
     * @return Returns the nullable property value.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(HttpProperty<T> key) {
        return (T) attributes.get(key);
    }

    /**
     * Get the set of property keys that are set on the HttpProperties object.
     *
     * @return the set property keys.
     */
    public Set<HttpProperty<?>> httpProperties() {
        return attributes.keySet();
    }

    /**
     * Create a new builder using the same properties as this object.
     *
     * @return the created builder.
     */
    public Builder toBuilder() {
        var builder = builder();
        builder.attributes.putAll(attributes);
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
        HttpProperties that = (HttpProperties) o;
        return Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributes);
    }

    /**
     * Creates an {@link HttpProperties}.
     */
    public static final class Builder {

        private Map<HttpProperty<?>, Object> attributes = new HashMap<>();

        private Builder() {}

        /**
         * Create the {@code HttpProperties} object.
         *
         * @return the built HttpProperties object.
         */
        public HttpProperties build() {
            var copy = attributes;
            this.attributes = new HashMap<>();
            return new HttpProperties(copy);
        }

        /**
         * Put a strongly typed property on the builder.
         *
         * @param key   HttpProperty of the property, accessed by identity.
         * @param value Value associated with the property.
         * @return the builder.
         * @param <T> value type.
         */
        public <T> Builder put(HttpProperty<T> key, T value) {
            attributes.put(key, value);
            return this;
        }
    }
}
