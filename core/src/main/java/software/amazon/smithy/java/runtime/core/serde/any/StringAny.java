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

final class StringAny implements Any {

    private final String value;
    private final SdkSchema schema;

    StringAny(String value, SdkSchema schema) {
        if (!(schema.type() == ShapeType.DOCUMENT || schema.type() == ShapeType.STRING)) {
            throw new SdkSerdeException("Expected string Any to have a string or document schema, but found " + schema);
        }

        this.schema = schema;
        this.value = value;
    }

    @Override
    public SdkSchema schema() {
        return schema;
    }

    @Override
    public ShapeType type() {
        return ShapeType.STRING;
    }

    @Override
    public String asString() {
        return value;
    }

    @Override
    public void serialize(ShapeSerializer encoder) {
        encoder.writeString(schema, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StringAny stringAny = (StringAny) o;
        return Objects.equals(value, stringAny.value) && Objects.equals(schema, stringAny.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, schema);
    }

    @Override
    public String toString() {
        return "StringAny{value='" + value + "', schema=" + schema + '}';
    }
}
