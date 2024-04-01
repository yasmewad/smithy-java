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

final class DoubleAny implements Any {

    private final double value;
    private final SdkSchema schema;

    DoubleAny(double value, SdkSchema schema) {
        if (!(schema.type() == ShapeType.DOCUMENT || schema.type() == ShapeType.DOUBLE)) {
            throw new SdkSerdeException("Expected double Any to have a double or document schema, but found " + schema);
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
        return ShapeType.DOUBLE;
    }

    @Override
    public double asDouble() {
        return value;
    }

    @Override
    public void serialize(ShapeSerializer encoder) {
        encoder.writeDouble(schema, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DoubleAny doubleAny = (DoubleAny) o;
        return Double.compare(value, doubleAny.value) == 0 && Objects.equals(schema, doubleAny.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, schema);
    }

    @Override
    public String toString() {
        return "DoubleAny{value=" + value + ", schema=" + schema + '}';
    }
}
