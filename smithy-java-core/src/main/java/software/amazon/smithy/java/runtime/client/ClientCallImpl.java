/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.client;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import software.amazon.smithy.java.runtime.client.interceptor.ClientInterceptor;
import software.amazon.smithy.java.runtime.endpoint.Endpoint;
import software.amazon.smithy.java.runtime.shapes.IOShape;
import software.amazon.smithy.java.runtime.shapes.ModeledSdkException;
import software.amazon.smithy.java.runtime.shapes.SdkOperation;
import software.amazon.smithy.java.runtime.shapes.SdkShapeBuilder;
import software.amazon.smithy.java.runtime.util.Context;

/**
 * Basic implementation of {@link ClientCall}.
 *
 * @param <I> Operation input shape.
 * @param <O> Operation output shape.
 */
final class ClientCallImpl<I extends IOShape, O extends IOShape> implements ClientCall<I, O> {

    private final I input;
    private final Endpoint endpoint;
    private final SdkOperation<I, O> operation;
    private final Context context;
    private final BiFunction<Context, String, Optional<SdkShapeBuilder<ModeledSdkException>>> errorCreator;
    private final ClientInterceptor interceptor;

    ClientCallImpl(ClientCall.Builder<I, O> builder) {
        input = Objects.requireNonNull(builder.input, "input is null");
        operation = Objects.requireNonNull(builder.operation, "operation is null");
        context = Objects.requireNonNull(builder.context, "context is null");
        errorCreator = Objects.requireNonNull(builder.errorCreator, "errorCreator is null");
        endpoint = Objects.requireNonNull(builder.endpoint, "endpoint is null");
        interceptor = Objects.requireNonNull(builder.interceptor, "interceptor is null");
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
}
