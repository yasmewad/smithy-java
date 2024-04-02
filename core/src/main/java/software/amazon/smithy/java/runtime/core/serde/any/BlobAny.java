/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.core.serde.any;

import java.util.Arrays;
import java.util.Objects;
import software.amazon.smithy.java.runtime.core.schema.SdkSchema;
import software.amazon.smithy.java.runtime.core.serde.SdkSerdeException;
import software.amazon.smithy.java.runtime.core.serde.ShapeSerializer;
import software.amazon.smithy.model.shapes.ShapeType;

final class BlobAny implements Any {

    private final SdkSchema schema;
    private final byte[] value;

    BlobAny(byte[] value, SdkSchema schema) {
        if (!(schema.type() == ShapeType.DOCUMENT || schema.type() == ShapeType.BLOB)) {
            throw new SdkSerdeException("Expected blob Any to have a blob or document schema, but found " + schema);
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
        return ShapeType.BLOB;
    }

    @Override
    public byte[] asBlob() {
        return value;
    }

    @Override
    public void serialize(ShapeSerializer encoder) {
        encoder.writeBlob(schema, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BlobAny blobAny = (BlobAny) o;
        return Objects.equals(schema, blobAny.schema) && Arrays.equals(value, blobAny.value);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(schema);
        result = 31 * result + Arrays.hashCode(value);
        return result;
    }

    @Override
    public String toString() {
        return "BlobAny{value=" + Arrays.toString(value) + "schema=" + schema + '}';
    }
}
