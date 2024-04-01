/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.core.serde.any;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import software.amazon.smithy.java.runtime.core.schema.SdkSchema;
import software.amazon.smithy.java.runtime.core.serde.SdkSerdeException;
import software.amazon.smithy.java.runtime.core.serde.ShapeSerializer;
import software.amazon.smithy.model.shapes.ShapeType;

final class MapAny implements Any {

    private final SdkSchema schema;
    private final Map<Any, Any> value;

    MapAny(Map<Any, Any> value, SdkSchema schema) {
        if (!(schema.type() == ShapeType.DOCUMENT || schema.type() == ShapeType.MAP)) {
            throw new SdkSerdeException("Expected map Any to have a map or document schema, but found " + schema);
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
        return ShapeType.MAP;
    }

    @Override
    public Map<Any, Any> asMap() {
        return value;
    }

    @Override
    public void serialize(ShapeSerializer encoder) {
        encoder.beginMap(schema, s -> {
            for (var entry : value.entrySet()) {
                Any key = entry.getKey();
                Consumer<ShapeSerializer> valueSer = ser -> entry.getValue().serialize(ser);
                switch (key.type()) {
                    case STRING -> s.entry(entry.getKey().asString(), valueSer);
                    case INTEGER -> s.entry(entry.getKey().asInteger(), valueSer);
                    case LONG -> s.entry(entry.getKey().asLong(), valueSer);
                    default -> throw new UnsupportedOperationException("Unexpected document map key: " + key.type());
                }
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MapAny mapAny = (MapAny) o;
        return Objects.equals(schema, mapAny.schema) && Objects.equals(value, mapAny.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schema, value);
    }

    @Override
    public String toString() {
        return "MapAny{schema=" + schema + ", value=" + value + '}';
    }
}
