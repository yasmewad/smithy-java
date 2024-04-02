/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.core.serde.any;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.java.runtime.core.schema.SdkSchema;
import software.amazon.smithy.java.runtime.core.serde.SdkSerdeException;
import software.amazon.smithy.java.runtime.core.serde.ShapeSerializer;
import software.amazon.smithy.model.shapes.ShapeType;

final class StructAny implements Any {

    private final SdkSchema schema;
    private final Map<String, Any> value;
    private final ShapeType resolvedType;

    StructAny(Map<String, Any> value, SdkSchema schema) {
        // Validate the schema and resolve a type to return from #type(). The returned type will be an assumed
        // STRUCTURE when given a DOCUMENT (remember, type() can differ from schema().type()).
        this.resolvedType = switch (schema.type()) {
            case DOCUMENT, STRUCTURE -> ShapeType.STRUCTURE;
            case UNION -> ShapeType.UNION;
            default -> throw new SdkSerdeException(
                "Expected struct Any to have a structure or document schema, but found "
                    + schema
            );
        };

        // Every member must use a document schema or a valid member schema.
        for (var entry : value.entrySet()) {
            if (entry.getValue().schema().isMember()) {
                if (!entry.getKey().equals(entry.getValue().schema().memberName())) {
                    throw new SdkSerdeException(
                        "Expected Any struct member for key '" + entry.getKey() + "' to have a matching member "
                            + "name, but found " + entry.getValue().schema()
                    );
                }
            } else if (entry.getValue().schema().type() != ShapeType.DOCUMENT) {
                throw new SdkSerdeException(
                    "Each member of a structure or union Any must have a document schema or "
                        + "a member schema, but found " + entry.getValue()
                        + " at key '" + entry.getKey() + "'"
                );
            }
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
        return resolvedType;
    }

    @Override
    public Any getStructMember(String memberName) {
        return value.get(memberName);
    }

    @Override
    public Map<Any, Any> asMap() {
        Map<Any, Any> result = new LinkedHashMap<>();
        for (var entry : value.entrySet()) {
            result.put(Any.of(entry.getKey()), entry.getValue());
        }
        return result;
    }

    @Override
    public void serialize(ShapeSerializer encoder) {
        encoder.beginStruct(schema, structSerializer -> {
            for (var value : value.values()) {
                structSerializer.member(value.schema(), value::serialize);
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
        StructAny structAny = (StructAny) o;
        return Objects.equals(schema, structAny.schema) && Objects.equals(value, structAny.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schema, value);
    }

    @Override
    public String toString() {
        return "StructAny{value=" + value + ", schema=" + schema + '}';
    }
}
