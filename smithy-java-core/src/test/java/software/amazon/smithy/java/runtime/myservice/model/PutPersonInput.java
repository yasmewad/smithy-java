/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.myservice.model;

import java.time.Instant;
import software.amazon.smithy.java.runtime.serde.ShapeDeserializer;
import software.amazon.smithy.java.runtime.serde.ShapeSerializer;
import software.amazon.smithy.java.runtime.serde.ToStringSerializer;
import software.amazon.smithy.java.runtime.shapes.IOShape;
import software.amazon.smithy.java.runtime.shapes.SdkSchema;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.JsonNameTrait;
import software.amazon.smithy.model.traits.RequiredTrait;

// Example of a potentially generated shape.
public final class PutPersonInput implements IOShape {

    public static final ShapeId ID = ShapeId.from("smithy.example#PutPersonInput");
    private static final SdkSchema SCHEMA_NAME = SdkSchema
            .memberBuilder(0, "name", SharedSchemas.STRING)
            .id(ID).traits(new HttpLabelTrait(), new RequiredTrait()).build();
    private static final SdkSchema SCHEMA_FAVORITE_COLOR = SdkSchema
            .memberBuilder(1, "favoriteColor", SharedSchemas.STRING)
            .id(ID).traits(new HttpQueryTrait("favoriteColor")).build();
    private static final SdkSchema SCHEMA_AGE = SdkSchema
            .memberBuilder(2, "age", SharedSchemas.INTEGER)
            .id(ID).traits(new JsonNameTrait("Age")).build();
    private static final SdkSchema SCHEMA_BIRTHDAY = SdkSchema
            .memberBuilder(3, "birthday", SharedSchemas.BIRTHDAY)
            .id(ID).build();
    private static final SdkSchema SCHEMA_BINARY = SdkSchema
            .memberBuilder(4, "binary", SharedSchemas.BLOB).id(ID).build();
    static final SdkSchema SCHEMA = SdkSchema.builder()
            .id(ID)
            .type(ShapeType.STRUCTURE)
            .members(SCHEMA_NAME, SCHEMA_FAVORITE_COLOR, SCHEMA_AGE, SCHEMA_BIRTHDAY, SCHEMA_BINARY)
            .build();

    private final String name;
    private final int age;
    private final Instant birthday;
    private final String favoriteColor;
    private final byte[] binary;

    private PutPersonInput(Builder builder) {
        this.name = builder.name;
        this.age = builder.age;
        this.birthday = builder.birthday;
        this.favoriteColor = builder.favoriteColor;
        this.binary = builder.binary;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int age() {
        return age;
    }

    public String name() {
        return name;
    }

    public Instant birthday() {
        return birthday;
    }

    public byte[] binary() {
        return binary;
    }

    @Override
    public String toString() {
        return ToStringSerializer.serialize(this);
    }

    @Override
    public void serialize(ShapeSerializer serializer) {
        serializer.beginStruct(SCHEMA, st -> {
            st.stringMember(SCHEMA_NAME, name);
            st.integerMember(SCHEMA_AGE, age);
            st.timestampMemberIf(SCHEMA_BIRTHDAY, birthday);
            st.stringMemberIf(SCHEMA_FAVORITE_COLOR, favoriteColor);
            st.blobMemberIf(SCHEMA_BINARY, binary);
        });
    }

    public static final class Builder implements IOShape.Builder<PutPersonInput> {

        private String name;
        private int age = 0;
        private Instant birthday;
        private String favoriteColor;
        private byte[] binary;

        private Builder() {}

        @Override
        public PutPersonInput build() {
            return new PutPersonInput(this);
        }

        public Builder age(int age) {
            this.age = age;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder birthday(Instant birthday) {
            this.birthday = birthday;
            return this;
        }

        public Builder favoriteColor(String favoriteColor) {
            this.favoriteColor = favoriteColor;
            return this;
        }

        public Builder binary(byte[] binary) {
            this.binary = binary;
            return this;
        }

        @Override
        public Builder deserialize(ShapeDeserializer decoder) {
            decoder.readStruct(SCHEMA, (member, de) -> {
                switch (member.memberIndex()) {
                    case 0 -> name(de.readString(member));
                    case 1 -> favoriteColor(de.readString(member));
                    case 2 -> age(de.readInteger(member));
                    case 3 -> birthday(de.readTimestamp(member));
                    case 4 -> binary(de.readBlob(member));
                }
            });
            return this;
        }
    }
}
