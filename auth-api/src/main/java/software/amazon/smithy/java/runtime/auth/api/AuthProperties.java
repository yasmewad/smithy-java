/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.auth.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * An immutable collection of auth-related properties used for signing, identity resolution, etc.
 */
public final class AuthProperties {

    /**
     * A {@code Key} provides an identity-based, immutable token.
     *
     * <p>The token also contains a name used to describe the value.
     */
    public static final class Key<T> {
        private final String name;

        /**
         * @param name Name of the value.
         */
        private Key(String name) {
            this.name = Objects.requireNonNull(name);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final Map<Key<?>, Object> attributes;

    private AuthProperties(Map<Key<?>, Object> attributes) {
        this.attributes = new HashMap<>(attributes);
    }

    /**
     * Creates a builder used to build {@link AuthProperties}.
     *
     * @return the builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a new identity-based key to store in the context.
     *
     * @param name Name of the key.
     * @return the created key.
     * @param <T> value type associated with the key.
     */
    public static <T> Key<T> key(String name) {
        return new Key<>(name);
    }

    /**
     * Get a property.
     *
     * @param key   property key to get by exact reference identity.
     * @param <T>   Returns the value, or null if not present.
     * @return Returns the nullable property value.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Key<T> key) {
        return (T) attributes.get(key);
    }

    /**
     * Get a property and throw if it isn't present.
     *
     * @param key property key to get by exact reference identity.
     * @param <T> Returns the value.
     * @throws IllegalArgumentException if the property isn't found.
     */
    public <T> T expect(Key<T> key) {
        T value = get(key);
        if (value == null) {
            throw new IllegalArgumentException("Unknown property: " + key);
        }
        return value;
    }

    /**
     * Get the keys added to the context.
     *
     * @return the keys.
     */
    public Set<Key<?>> keys() {
        return attributes.keySet();
    }

    /**
     * Get each key-value pair.
     *
     * @param consumer Receives each key value pair.
     */
    public void forEach(PropertyConsumer consumer) {
        keys().forEach(k -> consume(k, consumer));
    }

    private <T> void consume(Key<T> key, PropertyConsumer consumer) {
        consumer.accept(key, expect(key));
    }

    /**
     * Interface for receiving all {@link AuthProperties} entries.
     */
    @FunctionalInterface
    public interface PropertyConsumer {
        /**
         * A method to operate on a {@link AuthProperties} and it's values.
         *
         * @param key   The identity-based property key.
         * @param value The property value.
         * @param <T> The value type.
         */
        <T> void accept(Key<T> key, T value);
    }

    /**
     * Creates an {@link AuthProperties}.
     */
    public static final class Builder {
        private final Map<Key<?>, Object> attributes = new HashMap<>();

        private Builder() {}

        /**
         * Create the {@code AuthProperties} object.
         *
         * @return the built AuthProperties object.
         */
        public AuthProperties build() {
            return new AuthProperties(attributes);
        }

        /**
         * Put a strongly typed property on the builder.
         *
         * @param key   Key of the property, accessed by identity.
         * @param value Value associated with the property.
         * @return the builder.
         * @param <T> value type.
         */
        public <T> Builder put(Key<T> key, T value) {
            attributes.put(key, value);
            return this;
        }
    }
}
