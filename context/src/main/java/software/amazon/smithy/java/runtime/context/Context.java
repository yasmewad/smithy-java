/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.context;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A read/write typed context map.
 */
public interface Context extends ReadableContext {
    /**
     * Set a Property.
     *
     * @param key   Property key.
     * @param value Value to set.
     * @param <T>   Returns the previously set value, or null if not present.
     */
    <T> void setProperty(Constant<T> key, T value);

    /**
     * Creates a context context map.
     *
     * @return Returns the created context.
     */
    static Context create() {
        return new Context() {
            private final ConcurrentMap<Constant<?>, Object> attributes = new ConcurrentHashMap<>();

            @Override
            public <T> void setProperty(Constant<T> key, T value) {
                attributes.put(key, value);
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> T getProperty(Constant<T> key) {
                return (T) attributes.get(key);
            }

            @Override
            public void forEachProperty(PropertyConsumer consumer) {
                attributes.forEach((k, v) -> consumeProperty(k, consumer));
            }

            private <T> void consumeProperty(Constant<T> property, PropertyConsumer consumer) {
                consumer.accept(property, getProperty(property));
            }
        };
    }
}
