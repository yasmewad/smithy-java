/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.serde.httpbinding;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.java.runtime.myservice.model.PutPerson;
import software.amazon.smithy.java.runtime.myservice.model.PutPersonInput;
import software.amazon.smithy.java.runtime.net.http.SmithyHttpRequest;
import software.amazon.smithy.java.runtime.serde.json.JsonCodec;

public class RequestSerializerTest {
    @Test
    public void serializesResponse() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        queryParams.put("a", List.of("a 1", "a2"));
        queryParams.put("b", List.of("b 1", "b2"));

        JsonCodec codec = JsonCodec.builder().useJsonName(true).useTimestampFormat(true).build();
        RequestSerializer serializer = HttpBinding.requestSerializer();
        serializer.operation(new PutPerson().schema());
        serializer.payloadCodec(codec);
        serializer.endpoint(URI.create("https://example.com"));
        serializer.shapeValue(PutPersonInput.builder()
                .age(10)
                .name("Roosevelt")
                .favoriteColor("Orange")
                .birthday(Instant.now())
                .queryParams(queryParams)
                .build());

        SmithyHttpRequest request = serializer.serializeRequest();

        assertThat(request.uri().getRawQuery(), equalTo("favoriteColor=Orange&a=a%201&a=a2&b=b%201&b=b2"));
    }
}
