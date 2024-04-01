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

final class FloatAny implements Any {

    private final float value;
    private final SdkSchema schema;

    FloatAny(float value, SdkSchema schema) {
        if (!(schema.type() == ShapeType.DOCUMENT || schema.type() == ShapeType.FLOAT)) {
            throw new SdkSerdeException("Expected float Any to have a float or document schema, but found " + schema);
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
        return ShapeType.FLOAT;
    }

    @Override
    public float asFloat() {
        return value;
    }

    @Override
    public double asDouble() {
        // Allow a widening to double.
        return value;
    }

    @Override
    public void serialize(ShapeSerializer encoder) {
        encoder.writeFloat(schema, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FloatAny floatAny = (FloatAny) o;
        return Float.compare(value, floatAny.value) == 0 && Objects.equals(schema, floatAny.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, schema);
    }

    @Override
    public String toString() {
        return "FloatAny{value=" + value + ", schema=" + schema + '}';
    }
}
