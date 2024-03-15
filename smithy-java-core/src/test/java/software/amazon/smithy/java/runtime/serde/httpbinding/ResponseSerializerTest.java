/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.serde.httpbinding;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.java.runtime.myservice.model.PutPerson;
import software.amazon.smithy.java.runtime.myservice.model.PutPersonOutput;
import software.amazon.smithy.java.runtime.net.http.SmithyHttpResponse;
import software.amazon.smithy.java.runtime.serde.json.JsonCodec;

public class ResponseSerializerTest {
    @Test
    public void serializesResponse() {
        JsonCodec codec = JsonCodec.builder().useJsonName(true).useTimestampFormat(true).build();

        ResponseSerializer serializer = HttpBinding.responseSerializer();
        serializer.operation(new PutPerson().schema());
        serializer.payloadCodec(codec);
        serializer.shapeValue(PutPersonOutput.builder()
                                      .age(10)
                                      .name("Roosevelt")
                                      .favoriteColor("Orange")
                                      .birthday(Instant.now())
                                      .status(201)
                                      .build());

        SmithyHttpResponse response = serializer.serializeResponse();

        assertThat(response.statusCode(), equalTo(201));
    }
}
