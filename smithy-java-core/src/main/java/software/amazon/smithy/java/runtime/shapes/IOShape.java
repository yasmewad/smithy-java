/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.shapes;

import software.amazon.smithy.java.runtime.net.StoppableInputStream;
import software.amazon.smithy.java.runtime.serde.ShapeSerializer;

/**
 * Represents an input or output shape allowed to contain a stream.
 *
 * <p>Top-level input and output shapes are the only shapes in the model allowed to contain a stream.
 * Adding streams to the generic Smithy data model would complicate the data model and requires throwing many
 * UnsupportedOperationExceptions due to codecs not supporting streaming. Instead, streaming is handled by protocol
 * handlers that can rely on the fact that they are given input and output shapes and can directly ask the shapes for
 * streams or set streams on builders.
 */
public interface IOShape extends SerializableShape {
    /**
     * Serializes any streams attached to the shape to the given serializer.
     *
     * @param encoder Where to serialize the stream.
     */
    default void serializeStream(Serializer encoder) {
        // no streams are assumed by default.
    }

    /**
     * A builder used to create input or output shapes that can potentially contain streams and event streams.
     *
     * @param <T> Shape to build
     */
    interface Builder<T extends IOShape> extends SdkShapeBuilder<T> {
        /**
         * Deserializes any streams attached to the shape from the given deserializer.
         *
         * @param decoder Where to get the stream from.
         */
        default Builder<T> deserializeStream(Deserializer decoder) {
            return this;
        }
    }

    /**
     * Handles the deserialization of values only allowed on input or output shapes.
     */
    interface Deserializer {
        /**
         * Reads a data stream for the given member schema.
         *
         * @param schema A member schema that targets a streaming blob or string.
         * @return Returns the stream or an empty stream.
         */
        StoppableInputStream readStream(SdkSchema schema);

        /**
         * Reads an event stream for the given member schema.
         * <p>
         * TODO: Implement
         *
         * @param schema A member schema that targets a string.
         * @return Returns the stream or an empty stream.
         */
        Object readEventStream(SdkSchema schema);
    }

    /**
     * Handles the serialization of values only allowed on input or output shapes.
     */
    interface Serializer {
        /**
         * Writes a streaming blob.
         *
         * @param schema Schema of the member that targets a streaming blob.
         * @param value  Value to set.
         */
        void writeStreamingBlob(SdkSchema schema, StoppableInputStream value);

        /**
         * Writes a streaming string.
         *
         * @param schema Schema of the member that targets a streaming string.
         * @param value  Value to set.
         */
        void writeStreamingString(SdkSchema schema, StoppableInputStream value);

        /**
         * Writes an event stream.
         *
         * @param schema Schema of the member that targets an event stream.
         * @param value  Value to set.
         */
        void writeEventStream(SdkSchema schema, StoppableInputStream value);
    }
}
