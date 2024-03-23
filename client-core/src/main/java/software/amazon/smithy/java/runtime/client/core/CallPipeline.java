/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.client.core;

import software.amazon.smithy.java.runtime.client.interceptor.ClientInterceptor;
import software.amazon.smithy.java.runtime.context.Context;
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
        ClientInterceptor interceptor = call.interceptor();
        var context = call.context();
        context.setProperty(CallContext.INPUT, call.input());
        context.setProperty(CallContext.OPERATION_SCHEMA, call.operation().schema());
        context.setProperty(CallContext.INPUT_SCHEMA, call.operation().inputSchema());
        context.setProperty(CallContext.OUTPUT_SCHEMA, call.operation().outputSchema());

        interceptor.readBeforeExecution(context);

        context.setProperty(CallContext.INPUT, interceptor.modifyInputBeforeSerialization(call.input(), context));

        interceptor.readBeforeSerialization(context);

        RequestT request = protocol.createRequest(call);

        interceptor.readAfterSerialization(context);

        request = interceptor.modifyRequestBeforeRetryLoop(context, request);

        // Retry loop

        interceptor.readBeforeAttempt(context);

        request = interceptor.modifyRequestBeforeSigning(context, request);

        interceptor.readBeforeSigning(context);

        var signedRequest = protocol.signRequest(call, request);
        interceptor.readAfterSigning(context);
        signedRequest = interceptor.modifyRequestBeforeTransmit(context, signedRequest);
        interceptor.readBeforeTransmit(context);
        var response = protocol.sendRequest(call, signedRequest);
        return deserialize(call, signedRequest, response, interceptor);
    }

    private <I extends SerializableShape, O extends SerializableShape> O deserialize(
            ClientCall<I, O> call,
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

        var shape = protocol.deserializeResponse(call, request, modifiedResponse);

        context.setProperty(CallContext.OUTPUT, shape);
        interceptor.readAfterDeserialization(context);
        shape = interceptor.modifyOutputBeforeAttemptCompletion(shape, context);
        interceptor.readAfterAttempt(context);

        // End of retry loop
        shape = interceptor.modifyOutputBeforeCompletion(shape, context);
        interceptor.readAfterExecution(context);

        return shape;
    }
}
