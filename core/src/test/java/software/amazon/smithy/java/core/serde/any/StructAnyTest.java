/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.core.serde.any;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.java.runtime.core.schema.SdkSchema;
import software.amazon.smithy.java.runtime.core.serde.SdkSerdeException;
import software.amazon.smithy.java.runtime.core.serde.ShapeSerializer;
import software.amazon.smithy.java.runtime.core.serde.SpecificShapeSerializer;
import software.amazon.smithy.java.runtime.core.serde.StructSerializer;
import software.amazon.smithy.java.runtime.core.serde.any.Any;
import software.amazon.smithy.model.shapes.ShapeType;

public class StructAnyTest {
    @Test
    public void ensureEveryStructureMemberIsDocumentOrMember() {
        Map<String, Any> entries = new LinkedHashMap<>();
        SdkSchema schema = SdkSchema.builder().type(ShapeType.INTEGER).id("a#B").build();
        entries.put("a", Any.of("a"));
        entries.put("b", Any.of(1, schema));

        var e = Assertions.assertThrows(SdkSerdeException.class, () -> Any.ofStruct(entries));

        assertThat(
            e.getMessage(),
            containsString(
                "Each member of a structure or union Any must have a document "
                    + "schema or a member schema, but found IntegerAny"
            )
        );
    }

    @Test
    public void createsAnyWithoutSchema() {
        Map<String, Any> entries = new LinkedHashMap<>();
        entries.put("a", Any.of("a"));
        entries.put("b", Any.of(1));
        var any = Any.ofStruct(entries);

        assertThat(any.type(), equalTo(ShapeType.STRUCTURE));
        assertThat(any.schema(), equalTo(Any.SCHEMA));
        assertThat(any.getStructMember("a"), equalTo(Any.of("a")));
        assertThat(any.getStructMember("b"), equalTo(Any.of(1)));
        assertThat(
            any.asMap(),
            equalTo(
                Map.of(
                    Any.of("a"),
                    Any.of("a"),
                    Any.of("b"),
                    Any.of(1)
                )
            )
        );
    }

    @Test
    public void createsAnyWithSchema() {
        var stringSchema = SdkSchema.builder().type(ShapeType.STRING).id("a#S").build();
        var intSchema = SdkSchema.builder().type(ShapeType.INTEGER).id("a#I").build();
        var structSchema = SdkSchema.builder()
            .id("a#B")
            .type(ShapeType.STRUCTURE)
            .members(
                SdkSchema.memberBuilder(0, "a", stringSchema).id("a#B").build(),
                SdkSchema.memberBuilder(1, "b", intSchema).id("a#B").build()
            )
            .build();

        Map<String, Any> entries = new LinkedHashMap<>();
        entries.put("a", Any.of("a", structSchema.member("a")));
        entries.put("b", Any.of(1, structSchema.member("b")));
        var any = Any.ofStruct(entries, structSchema);

        assertThat(any.type(), equalTo(ShapeType.STRUCTURE));
        assertThat(any.schema(), equalTo(structSchema));
    }

    @Test
    public void validatesStructAnyNames() {
        var stringSchema = SdkSchema.builder().type(ShapeType.STRING).id("a#S").build();
        var structSchema = SdkSchema.builder()
            .id("a#B")
            .type(ShapeType.STRUCTURE)
            .members(SdkSchema.memberBuilder(0, "a", stringSchema).id("a#B").build())
            .build();

        Map<String, Any> entries = new LinkedHashMap<>();
        entries.put("A", Any.of("a", structSchema.member("a")));

        var e = Assertions.assertThrows(SdkSerdeException.class, () -> Any.ofStruct(entries, structSchema));

        assertThat(
            e.getMessage(),
            containsString(
                "Expected Any struct member for key 'A' to have a matching member name, but found "
                    + "SdkSchema{id='a#B$a', type=string}"
            )
        );
    }

    @Test
    public void serializesShape() {
        var stringSchema = SdkSchema.builder().type(ShapeType.STRING).id("a#S").build();
        var intSchema = SdkSchema.builder().type(ShapeType.INTEGER).id("a#I").build();
        var structSchema = SdkSchema.builder()
            .id("a#B")
            .type(ShapeType.STRUCTURE)
            .members(
                SdkSchema.memberBuilder(0, "a", stringSchema).id("a#B").build(),
                SdkSchema.memberBuilder(1, "b", intSchema).id("a#B").build()
            )
            .build();

        Map<String, Any> entries = new LinkedHashMap<>();
        entries.put("a", Any.of("a", structSchema.member("a")));
        entries.put("b", Any.of(1, structSchema.member("b")));
        var any = Any.ofStruct(entries, structSchema);

        List<String> actions = new ArrayList<>();

        any.serialize(new SpecificShapeSerializer() {
            @Override
            public StructSerializer beginStruct(SdkSchema schema) {
                assertThat(schema, equalTo(structSchema));
                actions.add("beginStruct");
                return new StructSerializer() {
                    @Override
                    public void endStruct() {
                        actions.add("endStruct");
                    }

                    @Override
                    public void member(SdkSchema member, Consumer<ShapeSerializer> memberWriter) {
                        actions.add("member:" + member.memberName());
                        memberWriter.accept(new SpecificShapeSerializer() {
                            @Override
                            public void writeString(SdkSchema schema, String value) {
                                assertThat(schema, equalTo(structSchema.member("a")));
                                actions.add("value:string:" + value);
                            }

                            @Override
                            public void writeInteger(SdkSchema schema, int value) {
                                assertThat(schema, equalTo(structSchema.member("b")));
                                actions.add("value:integer:" + value);
                            }
                        });
                    }
                };
            }
        });

        assertThat(
            actions,
            containsInAnyOrder(
                "beginStruct",
                "endStruct",
                "member:a",
                "member:b",
                "value:string:a",
                "value:integer:1"
            )
        );
    }
}
