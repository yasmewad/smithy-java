/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.client.core;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import software.amazon.smithy.java.runtime.api.Endpoint;
import software.amazon.smithy.java.runtime.client.core.interceptors.ClientInterceptor;
import software.amazon.smithy.java.runtime.core.Context;
import software.amazon.smithy.java.runtime.core.schema.ModeledSdkException;
import software.amazon.smithy.java.runtime.core.schema.SdkOperation;
import software.amazon.smithy.java.runtime.core.schema.SdkShapeBuilder;
import software.amazon.smithy.java.runtime.core.schema.SerializableShape;
import software.amazon.smithy.java.runtime.core.serde.DataStream;

/**
 * Basic implementation of {@link ClientCall}.
 *
 * @param <I> Operation input shape.
 * @param <O> Operation output shape.
 */
final class ClientCallImpl<I extends SerializableShape, O extends SerializableShape, OutputStreamT>
        implements ClientCall<I, O> {

    private final I input;
    private final Endpoint endpoint;
    private final SdkOperation<I, O> operation;
    private final Context context;
    private final BiFunction<Context, String, Optional<SdkShapeBuilder<ModeledSdkException>>> errorCreator;
    private final ClientInterceptor interceptor;
    private final DataStream requestInputStream;
    private final Object requestEventStream;

    ClientCallImpl(ClientCall.Builder<I, O> builder) {
        input = Objects.requireNonNull(builder.input, "input is null");
        operation = Objects.requireNonNull(builder.operation, "operation is null");
        context = Objects.requireNonNull(builder.context, "context is null");
        errorCreator = Objects.requireNonNull(builder.errorCreator, "errorCreator is null");
        endpoint = Objects.requireNonNull(builder.endpoint, "endpoint is null");
        interceptor = Objects.requireNonNull(builder.interceptor, "interceptor is null");
        requestInputStream = builder.requestInputStream;
        requestEventStream = builder.requestEventStream;
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
    public Optional<DataStream> requestInputStream() {
        return Optional.ofNullable(requestInputStream);
    }

    @Override
    public Optional<Object> requestEventStream() {
        return Optional.ofNullable(requestEventStream);
    }
}
