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

final class ByteAny implements Any {

    private final SdkSchema schema;
    private final byte value;

    ByteAny(byte value, SdkSchema schema) {
        if (!(schema.type() == ShapeType.DOCUMENT || schema.type() == ShapeType.BYTE)) {
            throw new SdkSerdeException("Expected byte Anyt to have a byte or document schema, but found " + schema);
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
        return ShapeType.BYTE;
    }

    @Override
    public byte asByte() {
        return value;
    }

    @Override
    public short asShort() {
        return value;
    }

    @Override
    public int asInteger() {
        return value;
    }

    @Override
    public long asLong() {
        return value;
    }

    @Override
    public float asFloat() {
        return value;
    }

    @Override
    public double asDouble() {
        return value;
    }

    @Override
    public void serialize(ShapeSerializer encoder) {
        encoder.writeByte(schema, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ByteAny byteAny = (ByteAny) o;
        return value == byteAny.value && Objects.equals(schema, byteAny.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schema, value);
    }

    @Override
    public String toString() {
        return "ByteAny{value=" + value + ", schema=" + schema + '}';
    }
}
