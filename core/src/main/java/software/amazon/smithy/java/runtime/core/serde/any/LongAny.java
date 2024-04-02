/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.core.serde.any;

import java.util.Objects;
import software.amazon.smithy.java.runtime.core.schema.SdkSchema;
import software.amazon.smithy.java.runtime.core.serde.SdkSerdeException;
import software.amazon.smithy.java.runtime.core.serde.ShapeSerializer;
import software.amazon.smithy.model.shapes.ShapeType;

final class LongAny implements Any {

    private final SdkSchema schema;
    private final long value;

    LongAny(long value, SdkSchema schema) {
        if (!(schema.type() == ShapeType.DOCUMENT || schema.type() == ShapeType.LONG)) {
            throw new SdkSerdeException("Expected long Any to have a long or document schema, but found " + schema);
        }

        this.value = value;
        this.schema = schema;
    }

    @Override
    public SdkSchema schema() {
        return schema;
    }

    @Override
    public ShapeType type() {
        return ShapeType.LONG;
    }

    @Override
    public long asLong() {
        return value;
    }

    @Override
    public void serialize(ShapeSerializer encoder) {
        encoder.writeLong(schema, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LongAny longAny = (LongAny) o;
        return value == longAny.value && Objects.equals(schema, longAny.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schema, value);
    }

    @Override
    public String toString() {
        return "LongAny{value=" + value + ", schema=" + schema + '}';
    }
}
