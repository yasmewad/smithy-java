/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.httpbinding;

import java.util.function.IntConsumer;
import software.amazon.smithy.java.runtime.core.serde.SpecificShapeSerializer;
import software.amazon.smithy.java.runtime.core.shapes.SdkSchema;

final class ResponseStatusSerializer extends SpecificShapeSerializer {

    private final IntConsumer consumer;

    ResponseStatusSerializer(IntConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    protected RuntimeException throwForInvalidState(SdkSchema schema) {
        throw new UnsupportedOperationException(schema + " is not a value response status code member");
    }

    @Override
    public void writeInteger(SdkSchema schema, int value) {
        consumer.accept(value);
    }
}
