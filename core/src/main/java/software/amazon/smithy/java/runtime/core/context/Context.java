/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.core.context;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A typed context map.
 */
public interface Context {

    /**
     * Creates a context context map.
     *
     * @return Returns the created context.
     */
    static Context create() {
        return new Context() {
            private final ConcurrentMap<Constant<?>, Object> attributes = new ConcurrentHashMap<>();

            @Override
            public final <T> void setAttribute(Constant<T> key, T value) {
                attributes.put(key, value);
            }

            @Override
            @SuppressWarnings("unchecked")
            public final <T> T getAttribute(Constant<T> key) {
                return (T) attributes.get(key);
            }
        };
    }

    /**
     * Set an attribute.
     *
     * @param key   Attribute key.
     * @param value Value to set.
     * @param <T>   Returns the previously set value, or null if not present.
     */
    <T> void setAttribute(Constant<T> key, T value);

    /**
     * Get an attribute.
     *
     * @param key   Attribute key to get by exact reference identity.
     * @param <T>   Returns the value, or null if not present.
     * @return Returns the nullable attribute value.
     */
    <T> T getAttribute(Constant<T> key);

    /**
     * Get an attribute or return a default if not found.
     *
     * @param key          Attribute key to get by exact reference identity.
     * @param defaultValue Value to return if the attribute is null or non-existent.
     * @param <T>          Returns the value, or null if not present.
     * @return Returns the attribute value.
     */
    default <T> T getAttribute(Constant<T> key, T defaultValue) {
        return Objects.requireNonNullElse(getAttribute(key), defaultValue);
    }

    /**
     * Get an attribute and throw if it isn't present.
     *
     * @param key Attribute key to get by exact reference identity.
     * @param <T> Returns the value.
     * @throws IllegalArgumentException if the attribute isn't found.
     */
    default <T> T expectAttribute(Constant<T> key) {
        T value = getAttribute(key);
        if (value == null) {
            throw new IllegalArgumentException("Unknown attribute: " + key);
        }
        return value;
    }
}
