/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.example.model;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import software.amazon.smithy.java.runtime.core.context.Context;
import software.amazon.smithy.java.runtime.core.serde.streaming.StreamHandler;
import software.amazon.smithy.java.runtime.core.serde.streaming.StreamPublisher;
import software.amazon.smithy.java.runtime.core.serde.streaming.StreamingShape;

// An example of a generated service interface.
public interface PersonDirectory {

    // Each operation generates two methods: one that takes context and one that doesn't.
    default PutPersonOutput putPerson(PutPersonInput input) {
        return putPerson(input, Context.create());
    }

    default PutPersonOutput putPerson(PutPersonInput input, Context context) {
        return putPersonAsync(input, context).join();
    }

    default CompletableFuture<PutPersonOutput> putPersonAsync(PutPersonInput input) {
        return putPersonAsync(input, Context.create());
    }

    CompletableFuture<PutPersonOutput> putPersonAsync(PutPersonInput input, Context context);

    default PutPersonImageOutput putPersonImage(PutPersonImageInput input, StreamPublisher image) {
        return putPersonImage(input, image, Context.create());
    }

    default PutPersonImageOutput putPersonImage(PutPersonImageInput input, StreamPublisher image, Context context) {
        return putPersonImageAsync(input, image, context).join();
    }

    default CompletableFuture<PutPersonImageOutput> putPersonImageAsync(
            PutPersonImageInput input,
            StreamPublisher image
    ) {
        return putPersonImageAsync(input, image, Context.create());
    }

    CompletableFuture<PutPersonImageOutput> putPersonImageAsync(
            PutPersonImageInput input,
            StreamPublisher image,
            Context context
    );

    default StreamingShape<GetPersonImageOutput, InputStream> getPersonImage(GetPersonImageInput input) {
        return getPersonImage(input, Context.create());
    }

    default StreamingShape<GetPersonImageOutput, InputStream> getPersonImage(
            GetPersonImageInput input,
            Context context
    ) {
        return getPersonImageAsync(input, context, StreamHandler.ofInputStream()).join();
    }

    default <ResultT> CompletableFuture<StreamingShape<GetPersonImageOutput, ResultT>> getPersonImageAsync(
            GetPersonImageInput input,
            StreamHandler<GetPersonImageOutput, ResultT> handler
    ) {
        return getPersonImageAsync(input, Context.create(), handler);
    }

    <ResultT> CompletableFuture<StreamingShape<GetPersonImageOutput, ResultT>> getPersonImageAsync(
            GetPersonImageInput input,
            Context context,
            StreamHandler<GetPersonImageOutput, ResultT> handler
    );
}
