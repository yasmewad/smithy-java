/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.core;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;

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
     * A binding of a key to a value.
     *
     * @param key   Key of the binding.
     * @param value Value bound to the key.
     * @param <T>   Value type.
     */
    record Value<T>(Key<T> key, T value) {
        /**
         * Checks if the value uses the same key as {@code key}, ensuring that {@code T} and {@code U} are the same,
         * and if so, supplies the given mapper with a type {@code U} and returns an updated {@code U}. If the entry
         * does not use the same key, the original value is returned as-is.
         *
         * @param key Key to check for identity equality.
         * @param mapper Mapper that accepts the value and returns an updated value. It is only invoked if compatible.
         * @return the mapped result, or the original result if the entry does not use the given {@code key}.
         * @param <U> Value type to supply to the mapper.
         */
        @SuppressWarnings("unchecked")
        public <U> Value<T> mapIf(Key<U> key, Function<U, U> mapper) {
            if (key == this.key) {
                var mapped = (U) value;
                return (Value<T>) new Value<>(key, mapper.apply(mapped));
            } else {
                return this;
            }
        }

        /**
         * Get the value from the entry as {@code U} if it uses the given key, ensuring that {@code U} and {@code T}
         * are the same.
         *
         * @param key Key to check for identity equality.
         * @param consumer Consumer that accepts the value of the entry as a {@code U}.
         * @param <U> Value type to supply to the consumer.
         */
        @SuppressWarnings("unchecked")
        public <U> void getIf(Key<U> key, Consumer<U> consumer) {
            if (key == this.key) {
                consumer.accept((U) value);
            }
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
     * Create a new typed entry that combines a key and value.
     *
     * @param key   Key to set.
     * @param value Value to set.
     * @return the created entry.
     * @param <T> Value type.
     */
    static <T> Value<T> value(Key<T> key, T value) {
        return new Value<>(key, value);
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
     * @throws NullPointerException if the property isn't found.
     */
    default <T> T expect(Key<T> key) {
        T value = get(key);
        if (value == null) {
            throw new NullPointerException("Unknown property: " + key);
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
