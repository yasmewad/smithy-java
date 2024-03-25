/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.client.core;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import software.amazon.smithy.java.runtime.client.core.interceptors.ClientInterceptor;
import software.amazon.smithy.java.runtime.core.Context;
import software.amazon.smithy.java.runtime.core.Either;
import software.amazon.smithy.java.runtime.core.schema.SdkException;
import software.amazon.smithy.java.runtime.core.schema.SerializableShape;

public final class CallPipeline<RequestT, ResponseT> {

    private static final System.Logger LOGGER = System.getLogger(CallPipeline.class.getName());
    private final ClientProtocol<RequestT, ResponseT> protocol;

    public CallPipeline(ClientProtocol<RequestT, ResponseT> protocol) {
        this.protocol = protocol;
    }

    /**
     * Send the input and deserialize a response or throw errors.
     *
     * @param call Call to invoke.
     * @return Returns the deserialized response if successful.
     * @param <I> Input shape.
     * @param <O> Output shape.
     */
    public <I extends SerializableShape, O extends SerializableShape> O send(ClientCall<I, O> call) {
        var context = call.context();
        context.put(CallContext.INPUT, call.input());
        context.put(CallContext.OPERATION_SCHEMA, call.operation().schema());
        context.put(CallContext.INPUT_SCHEMA, call.operation().inputSchema());
        context.put(CallContext.OUTPUT_SCHEMA, call.operation().outputSchema());
        var timeout = context.get(CallContext.API_CALL_TIMEOUT);

        // Call the actual service in a virtual thread to support total-call timeout.
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<O> result = executor.submit(() -> doSend(call));
            if (timeout == null) {
                return result.get();
            } else {
                return result.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private <I extends SerializableShape, O extends SerializableShape> O doSend(ClientCall<I, O> call) {
        ClientInterceptor interceptor = call.interceptor();
        var context = call.context();
        var input = call.input();
        var requestKey = protocol.requestKey();

        interceptor.readBeforeExecution(context, input);
        context.put(CallContext.INPUT, interceptor.modifyBeforeSerialization(context, input));

        interceptor.readBeforeSerialization(context, input);
        RequestT request = protocol.createRequest(call);
        interceptor.readAfterSerialization(context, input, Context.value(requestKey, request));

        request = interceptor.modifyBeforeRetryLoop(context, input, Context.value(requestKey, request)).value();

        // Retry loop

        interceptor.readBeforeAttempt(context, input, Context.value(requestKey, request));

        // Signing.
        request = interceptor.modifyBeforeSigning(context, input, Context.value(requestKey, request)).value();
        interceptor.readBeforeSigning(context, input, Context.value(requestKey, request));
        request = protocol.signRequest(call, request);
        interceptor.readAfterSigning(context, input, Context.value(requestKey, request));

        request = interceptor.modifyBeforeTransmit(context, input, Context.value(requestKey, request)).value();
        interceptor.readBeforeTransmit(context, input, Context.value(requestKey, request));
        var response = protocol.sendRequest(call, request);
        return deserialize(call, request, response, interceptor);
    }

    private <I extends SerializableShape, O extends SerializableShape> O deserialize(
            ClientCall<I, O> call,
            RequestT request,
            ResponseT response,
            ClientInterceptor interceptor
    ) {
        var input = call.input();
        var requestKey = protocol.requestKey();
        var responseKey = protocol.responseKey();
        LOGGER.log(System.Logger.Level.TRACE, () -> "Deserializing response with "
                                                    + protocol.getClass()
                                                    + " for " + request + ": " + response);

        Context context = call.context();

        interceptor.readAfterTransmit(context, input, Context.value(requestKey, request),
                                      Context.value(responseKey, response));

        ResponseT modifiedResponse = interceptor.modifyBeforeDeserialization(
                        context, input, Context.value(requestKey, request), Context.value(responseKey, response))
                .value();

        interceptor.readBeforeDeserialization(context, input, Context.value(requestKey, request),
                                              Context.value(responseKey, response));

        var shape = protocol.deserializeResponse(call, request, modifiedResponse);
        context.put(CallContext.OUTPUT, shape);
        Either<O, SdkException> result = Either.left(shape);

        interceptor.readAfterDeserialization(context, input, Context.value(requestKey, request),
                                             Context.value(responseKey, response), result);

        result = interceptor.modifyBeforeAttemptCompletion(context, input, Context.value(requestKey, request),
                                                           Context.value(responseKey, response), result);

        interceptor.readAfterAttempt(context, input, Context.value(requestKey, request),
                                     Context.value(responseKey, response), result);

        // End of retry loop
        result = interceptor.modifyBeforeCompletion(context, input, Context.value(requestKey, request),
                                                    Context.value(responseKey, response), result);

        interceptor.readAfterExecution(context, input, Context.value(requestKey, request),
                                       Context.value(responseKey, response), result);

        if (result.isLeft()) {
            return shape;
        } else {
            throw result.right();
        }
    }
}
