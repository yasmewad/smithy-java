/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.core.serde;

import java.util.function.Consumer;

/**
 * Serializes a map shape.
 */
public interface MapSerializer {
    /**
     * Writes a string key for the map, and the valueSerializer is called and required to serialize the value.
     *
     * @param key             Key to write.
     * @param valueSerializer Serializer used to serialize the map value. A value must be serialized.
     */
    void entry(String key, Consumer<ShapeSerializer> valueSerializer);

    /**
     * Writes an integer key for the map, and the valueSerializer is called and required to serialize the value.
     *
     * @param key             Key to write.
     * @param valueSerializer Serializer used to serialize the map value. A value must be serialized.
     */
    void entry(int key, Consumer<ShapeSerializer> valueSerializer);

    /**
     * Writes a long key for the map, and the valueSerializer is called and required to serialize the value.
     *
     * @param key             Key to write.
     * @param valueSerializer Serializer used to serialize the map value. A value must be serialized.
     */
    void entry(long key, Consumer<ShapeSerializer> valueSerializer);
}
