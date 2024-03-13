/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime;

import java.io.ByteArrayInputStream;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.java.runtime.client.RestJsonClientHandler;
import software.amazon.smithy.java.runtime.client.javaclient.JavaHttpClient;
import software.amazon.smithy.java.runtime.endpoint.EndpointProvider;
import software.amazon.smithy.java.runtime.myservice.PersonDirectoryClient;
import software.amazon.smithy.java.runtime.myservice.model.PersonDirectory;
import software.amazon.smithy.java.runtime.myservice.model.PutPersonImageInput;
import software.amazon.smithy.java.runtime.myservice.model.PutPersonImageOutput;
import software.amazon.smithy.java.runtime.myservice.model.PutPersonInput;
import software.amazon.smithy.java.runtime.myservice.model.PutPersonOutput;
import software.amazon.smithy.java.runtime.myservice.model.ValidationError;
import software.amazon.smithy.java.runtime.net.StoppableInputStream;
import software.amazon.smithy.java.runtime.serde.Codec;
import software.amazon.smithy.java.runtime.serde.any.Any;
import software.amazon.smithy.java.runtime.serde.json.JsonCodec;
import software.amazon.smithy.java.runtime.shapes.ModeledSdkException;
import software.amazon.smithy.java.runtime.shapes.SdkShapeBuilder;
import software.amazon.smithy.java.runtime.shapes.TypeRegistry;

public class GenericTest {

    @Test
    public void theClient() {
        HttpClient httpClient = HttpClient.newBuilder().build();
        PersonDirectory client = PersonDirectoryClient.builder()
                .protocol(new RestJsonClientHandler(new JavaHttpClient(httpClient)))
                .endpointProvider(EndpointProvider.staticEndpoint("https://example.com"))
                .build();
        PutPersonInput input = PutPersonInput.builder()
                .name("Michael")
                .age(999)
                .favoriteColor("Green")
                .birthday(Instant.now())
                .build();
        PutPersonOutput output = client.putPerson(input);
    }

    @Test
    public void streamingRequestPayload() {
        HttpClient httpClient = HttpClient.newBuilder().build();
        PersonDirectory client = PersonDirectoryClient.builder()
                .protocol(new RestJsonClientHandler(new JavaHttpClient(httpClient)))
                .endpointProvider(EndpointProvider.staticEndpoint("https://example.com"))
                .build();
        PutPersonImageInput input = PutPersonImageInput.builder()
                .name("Michael")
                .image(StoppableInputStream.of(new ByteArrayInputStream("foo".getBytes(StandardCharsets.UTF_8))))
                .tags(List.of("Foo", "Bar"))
                .moreTags(List.of("Abc", "one two"))
                .build();
        PutPersonImageOutput output = client.putPersonImage(input);
    }

    @Test
    public void testAny() {
        Codec codec = JsonCodec.builder()
                .useJsonName(true)
                .useTimestampFormat(true)
                .build();

        PutPersonInput input = PutPersonInput.builder()
                .name("Michael")
                .age(999)
                .favoriteColor("Green")
                .birthday(Instant.now())
                .binary("Hello".getBytes(StandardCharsets.UTF_8))
                .build();

        // Serialize directly to JSON.
        System.out.println(codec.serializeToString(input));

        // Convert to an Any and then serialize to JSON.
        Any any = Any.of(input);
        System.out.println(codec.serializeToString(any));

        // Send the Any to a person builder.
        PutPersonInput inputCopy = any.asShape(PutPersonInput.builder());

        // Now serialize that to see that it round-trips.
        System.out.println(codec.serializeToString(inputCopy));
    }

    @Test
    public void testTypeRegistry() {
        TypeRegistry registry = TypeRegistry.builder()
                .putType(PutPersonInput.ID, PutPersonInput.class, PutPersonInput::builder)
                .putType(ValidationError.ID, ValidationError.class, ValidationError::builder)
                .build();

        registry.create(ValidationError.ID, ModeledSdkException.class)
                .map(SdkShapeBuilder::build)
                .ifPresent(System.out::println);
    }

    @Test
    public void serde() {
        PutPersonInput input = PutPersonInput.builder()
                .name("Michael")
                .age(999)
                .favoriteColor("Green")
                .birthday(Instant.now())
                .build();

        JsonCodec codec = JsonCodec.builder().useJsonName(true).useTimestampFormat(true).build();

        // Use a helper to serialize the shape into string.
        String jsonString = codec.serializeToString(input);

        // Use a helper to deserialize directly into a builder and create the shape.
        PutPersonInput copy = codec.deserializeShape(jsonString, PutPersonInput.builder());

        // Dump out the copy of the shape.
        System.out.println(codec.serializeToString(copy));
    }
}
