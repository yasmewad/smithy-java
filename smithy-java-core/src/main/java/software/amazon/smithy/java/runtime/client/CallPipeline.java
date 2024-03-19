/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.client;

import java.util.concurrent.CompletableFuture;
import software.amazon.smithy.java.runtime.client.interceptor.ClientInterceptor;
import software.amazon.smithy.java.runtime.serde.streaming.StreamingShape;
import software.amazon.smithy.java.runtime.shapes.SerializableShape;
import software.amazon.smithy.java.runtime.util.Context;

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
    public <I extends SerializableShape, O extends SerializableShape, StreamT>
    CompletableFuture<StreamingShape<O, StreamT>> send(ClientCall<I, O, StreamT> call) {
        ClientInterceptor interceptor = call.interceptor();
        var context = call.context();
        context.setAttribute(CallContext.INPUT, call.input());
        context.setAttribute(CallContext.OPERATION_SCHEMA, call.operation().schema());
        context.setAttribute(CallContext.INPUT_SCHEMA, call.operation().inputSchema());
        context.setAttribute(CallContext.OUTPUT_SCHEMA, call.operation().outputSchema());

        interceptor.readBeforeExecution(context);

        context.setAttribute(CallContext.INPUT, interceptor.modifyInputBeforeSerialization(call.input(), context));

        interceptor.readBeforeSerialization(context);

        RequestT request = protocol.createRequest(call);

        interceptor.readAfterSerialization(context);

        request = interceptor.modifyRequestBeforeRetryLoop(context, request);

        // Retry loop

        interceptor.readBeforeAttempt(context);

        request = interceptor.modifyRequestBeforeSigning(context, request);

        interceptor.readBeforeSigning(context);

        return protocol.signRequest(call, request)
                .thenApply(signedRequest -> {
                    interceptor.readAfterSigning(context);
                    signedRequest = interceptor.modifyRequestBeforeTransmit(context, signedRequest);
                    interceptor.readBeforeTransmit(context);
                    return signedRequest;
                })
                .thenCompose(signed -> protocol
                        .sendRequest(call, signed)
                        .thenCompose(response -> deserialize(call, signed, response, interceptor)));
    }

    private <I extends SerializableShape, O extends SerializableShape, StreamT>
    CompletableFuture<StreamingShape<O, StreamT>> deserialize(
            ClientCall<I, O, StreamT> call,
            RequestT request,
            ResponseT response,
            ClientInterceptor interceptor
    ) {
        LOGGER.log(System.Logger.Level.TRACE, () -> "Deserializing response with "
                                                    + protocol.getClass()
                                                    + " for " + request + ": " + response);

        Context context = call.context();

        interceptor.readAfterTransmit(context);

        ResponseT modifiedResponse = interceptor.modifyResponseBeforeDeserialization(context, response);

        interceptor.readResponseBeforeDeserialization(context);

        return protocol
                .deserializeResponse(call, request, modifiedResponse)
                .thenCompose(ioShape -> {
                    O shape = ioShape.shape();
                    context.setAttribute(CallContext.OUTPUT, shape);
                    interceptor.readAfterDeserialization(context);
                    shape = interceptor.modifyOutputBeforeAttemptCompletion(shape, context);
                    interceptor.readAfterAttempt(context);

                    // End of retry loop
                    shape = interceptor.modifyOutputBeforeCompletion(shape, context);
                    interceptor.readAfterExecution(context);
                    var finalShape = shape;

                    // Subscribe the stream handler and then return it's future result.
                    var subscriber = call.responseStreamHandler().apply(finalShape);
                    // Note: this expects that if the body is already consumed upstream, then it has been replaced
                    // with an empty body that can accept the subscriber. Otherwise, an IllegalStateException might
                    // be thrown by publishers that accept a single subscriber.
                    ioShape.value().subscribe(subscriber);
                    return subscriber.result().thenApply(result -> StreamingShape.of(finalShape, result));
                });
    }
}
