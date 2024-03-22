/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.clientcore;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import software.amazon.smithy.java.runtime.clientinterceptor.ClientInterceptor;
import software.amazon.smithy.java.runtime.core.context.Context;
import software.amazon.smithy.java.runtime.core.serde.streaming.StreamHandler;
import software.amazon.smithy.java.runtime.core.serde.streaming.StreamPublisher;
import software.amazon.smithy.java.runtime.core.shapes.ModeledSdkException;
import software.amazon.smithy.java.runtime.core.shapes.SdkOperation;
import software.amazon.smithy.java.runtime.core.shapes.SdkShapeBuilder;
import software.amazon.smithy.java.runtime.core.shapes.SerializableShape;
import software.amazon.smithy.java.runtime.endpointprovider.Endpoint;

/**
 * Basic implementation of {@link ClientCall}.
 *
 * @param <I> Operation input shape.
 * @param <O> Operation output shape.
 */
final class ClientCallImpl<I extends SerializableShape, O extends SerializableShape, OutputStreamT>
        implements ClientCall<I, O, OutputStreamT> {

    private final I input;
    private final Endpoint endpoint;
    private final SdkOperation<I, O> operation;
    private final Context context;
    private final BiFunction<Context, String, Optional<SdkShapeBuilder<ModeledSdkException>>> errorCreator;
    private final ClientInterceptor interceptor;
    private final StreamPublisher inputStream;
    private final StreamHandler<O, OutputStreamT> streamHandler;

    ClientCallImpl(ClientCall.Builder<I, O, OutputStreamT> builder) {
        input = Objects.requireNonNull(builder.input, "input is null");
        operation = Objects.requireNonNull(builder.operation, "operation is null");
        context = Objects.requireNonNull(builder.context, "context is null");
        errorCreator = Objects.requireNonNull(builder.errorCreator, "errorCreator is null");
        endpoint = Objects.requireNonNull(builder.endpoint, "endpoint is null");
        interceptor = Objects.requireNonNull(builder.interceptor, "interceptor is null");
        inputStream = builder.inputStream;
        streamHandler = builder.streamHandler;
    }

    @Override
    public I input() {
        return input;
    }

    @Override
    public SdkOperation<I, O> operation() {
        return operation;
    }

    @Override
    public Optional<SdkShapeBuilder<ModeledSdkException>> createExceptionBuilder(Context context, String shapeId) {
        return errorCreator.apply(context, shapeId);
    }

    @Override
    public Context context() {
        return context;
    }

    @Override
    public Endpoint endpoint() {
        return endpoint;
    }

    @Override
    public ClientInterceptor interceptor() {
        return interceptor;
    }

    @Override
    public Optional<StreamPublisher> requestStream() {
        return Optional.ofNullable(inputStream);
    }

    @Override
    public StreamHandler<O, OutputStreamT> responseStreamHandler() {
        return streamHandler;
    }
}
