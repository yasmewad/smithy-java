/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.context;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A thread safe, mutable, typed context map.
 */
public interface Context {
    /**
     * A {@code Key} provides an identity-based, immutable token.
     *
     * <p>The token also contains a name used to describe the value.
     */
    final class Key<T> {
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

    /**
     * Create a new identity-based key to store in the context.
     *
     * @param name Name of the key.
     * @return the created key.
     * @param <T> value type associated with the key.
     */
    static <T> Key<T> key(String name) {
        return new Key<>(name);
    }

    /**
     * Set a Property.
     *
     * @param key   Property key.
     * @param value Value to set.
     * @param <T>   Returns the previously set value, or null if not present.
     */
    <T> void put(Key<T> key, T value);

    /**
     * Get a property.
     *
     * @param key   property key to get by exact reference identity.
     * @param <T>   Returns the value, or null if not present.
     * @return Returns the nullable property value.
     */
    <T> T get(Key<T> key);

    /**
     * Get a property and throw if it isn't present.
     *
     * @param key property key to get by exact reference identity.
     * @param <T> Returns the value.
     * @throws IllegalArgumentException if the property isn't found.
     */
    default <T> T expect(Key<T> key) {
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
    Set<Key<?>> keys();

    /**
     * Get each key-value pair of the context object.
     *
     * @param consumer Receives each context key value pair.
     */
    default void forEach(PropertyConsumer consumer) {
        keys().forEach(k -> consume(k, consumer));
    }

    private <T> void consume(Key<T> key, PropertyConsumer consumer) {
        consumer.accept(key, expect(key));
    }

    /**
     * Interface for receiving all {@link Context} entries.
     */
    @FunctionalInterface
    interface PropertyConsumer {
        /**
         * A method to operate on a {@link Context} and it's values.
         *
         * @param key   The context key.
         * @param value The context value.
         * @param <T> The value type.
         */
        <T> void accept(Key<T> key, T value);
    }

    /**
     * Creates a thread-safe, mutable context map.
     *
     * @return Returns the created context.
     */
    static Context create() {
        return new Context() {
            private final ConcurrentMap<Key<?>, Object> attributes = new ConcurrentHashMap<>();

            @Override
            public <T> void put(Key<T> key, T value) {
                attributes.put(key, value);
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> T get(Key<T> key) {
                return (T) attributes.get(key);
            }

            @Override
            public Set<Key<?>> keys() {
                return attributes.keySet();
            }
        };
    }
}
