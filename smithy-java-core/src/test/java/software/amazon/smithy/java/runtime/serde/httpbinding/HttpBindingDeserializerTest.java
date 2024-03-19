/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.serde.httpbinding;

import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.java.runtime.myservice.model.PutPersonOutput;
import software.amazon.smithy.java.runtime.serde.json.JsonCodec;
import software.amazon.smithy.java.runtime.serde.streaming.StreamPublisher;
import software.amazon.smithy.java.runtime.shapes.SdkSchema;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.JsonNameTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;

public class HttpBindingDeserializerTest {

    static final SdkSchema STRING = SdkSchema.builder().type(ShapeType.STRING).id("smithy.api#String").build();

    static final SdkSchema INTEGER = SdkSchema.builder().type(ShapeType.INTEGER).id("smithy.api#Integer").build();

    static final SdkSchema BIRTHDAY = SdkSchema.builder()
            .type(ShapeType.TIMESTAMP)
            .id("smithy.example#Birthday")
            .traits(new SensitiveTrait())
            .build();

    static final SdkSchema BLOB = SdkSchema.builder().type(ShapeType.BLOB).id("smithy.api#Blob").build();

    public static final ShapeId PUT_PERSON_INPUT_ID = ShapeId.from("smithy.example#PutPersonInput");

    private static final SdkSchema SCHEMA_NAME = SdkSchema.memberBuilder(0, "name", STRING)
            .id(PUT_PERSON_INPUT_ID).traits(new HttpHeaderTrait("X-Name"), new RequiredTrait()).build();
    private static final SdkSchema SCHEMA_FAVORITE_COLOR = SdkSchema.memberBuilder(1, "favoriteColor", STRING)
            .id(PUT_PERSON_INPUT_ID).traits(new HttpQueryTrait("favoriteColor")).build();
    private static final SdkSchema SCHEMA_AGE = SdkSchema.memberBuilder(2, "age", INTEGER)
            .id(PUT_PERSON_INPUT_ID).traits(new JsonNameTrait("Age")).build();
    private static final SdkSchema SCHEMA_BIRTHDAY = SdkSchema.memberBuilder(3, "birthday", BIRTHDAY)
            .id(PUT_PERSON_INPUT_ID).build();
    private static final SdkSchema SCHEMA_BINARY = SdkSchema.memberBuilder(4, "binary", BLOB)
            .id(PUT_PERSON_INPUT_ID).build();
    static final SdkSchema PUT_PERSON_INPUT = SdkSchema.builder()
            .id(PUT_PERSON_INPUT_ID)
            .type(ShapeType.STRUCTURE)
            .members(SCHEMA_NAME, SCHEMA_FAVORITE_COLOR, SCHEMA_AGE, SCHEMA_BIRTHDAY, SCHEMA_BINARY)
            .build();

    private static final ShapeId PUT_PERSON_OUTPUT_ID = ShapeId.from("smithy.example#PutPersonOutput");

    private static final SdkSchema O_SCHEMA_NAME = SdkSchema.memberBuilder(0, "name", STRING)
            .id(PUT_PERSON_OUTPUT_ID).traits(new RequiredTrait()).build();
    private static final SdkSchema O_SCHEMA_FAVORITE_COLOR = SdkSchema.memberBuilder(1, "favoriteColor", STRING)
            .id(PUT_PERSON_OUTPUT_ID).traits(new HttpHeaderTrait("X-Favorite-Color")).build();
    private static final SdkSchema O_SCHEMA_AGE = SdkSchema.memberBuilder(2, "age", INTEGER)
            .id(PUT_PERSON_OUTPUT_ID).traits(new JsonNameTrait("Age")).build();
    private static final SdkSchema O_SCHEMA_BIRTHDAY = SdkSchema.memberBuilder(3, "birthday", BIRTHDAY)
            .id(PUT_PERSON_OUTPUT_ID)
            .traits(new TimestampFormatTrait(TimestampFormatTrait.DATE_TIME))
            .build();

    static final SdkSchema PUT_PERSON_OUTPUT = SdkSchema.builder()
            .id(PUT_PERSON_OUTPUT_ID)
            .type(ShapeType.STRUCTURE)
            .members(O_SCHEMA_NAME, O_SCHEMA_FAVORITE_COLOR, O_SCHEMA_AGE, O_SCHEMA_BIRTHDAY)
            .build();

    private static final SdkSchema PUT_PERSON = SdkSchema.builder()
            .type(ShapeType.OPERATION)
            .id("smithy.example#PutPerson")
            .traits(HttpTrait.builder()
                            .method("PUT")
                            .uri(UriPattern.parse("/persons"))
                            .code(200)
                            .build())
            .build();

    @Test
    public void deserializesBody() {
        JsonCodec json = JsonCodec.builder()
                .useJsonName(true)
                .useTimestampFormat(true)
                .build();

        var headerMap = Map.of(
                "X-Favorite-Color", List.of("Green"),
                "X-Age", List.of("100")
        );

        var headers = HttpHeaders.of(headerMap, (k, v) -> true);
        var bday = Instant.now().toString();
        var data = "{\"name\":\"Michael\",\"favoriteColor\":\"Green\",\"Age\":100,\"birthday\":\"" + bday + "\"}";
        var bytes = data.getBytes(StandardCharsets.UTF_8);

        HttpBindingDeserializer deserializer = HttpBindingDeserializer.builder()
                .payloadCodec(json)
                .responseStatus(200)
                .headers(headers)
                .body(StreamPublisher.ofString("test"))
                .build();

        PutPersonOutput output = PutPersonOutput.builder().deserialize(deserializer).build();

        System.out.println(output);
    }
}
