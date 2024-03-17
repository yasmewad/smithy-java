/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.client;

import software.amazon.smithy.java.runtime.client.interceptor.ClientInterceptor;
import software.amazon.smithy.java.runtime.shapes.IOShape;
import software.amazon.smithy.java.runtime.shapes.SdkException;

/**
 * An abstract class for implementing client handlers that takes care of interceptors.
 */
public abstract class InterceptorHandler implements ClientHandler {

    private static final System.Logger LOGGER = System.getLogger(InterceptorHandler.class.getName());

    @Override
    @SuppressWarnings("unchecked")
    public final <I extends IOShape, O extends IOShape> O send(ClientCall<I, O> call) {
        ClientInterceptor interceptor = call.interceptor();
        var context = call.context();
        context.setAttribute(CallContext.INPUT, call.input());
        context.setAttribute(CallContext.OPERATION_SCHEMA, call.operation().schema());
        context.setAttribute(CallContext.INPUT_SCHEMA, call.operation().inputSchema());
        context.setAttribute(CallContext.OUTPUT_SCHEMA, call.operation().outputSchema());

        interceptor.readBeforeExecution(context);

        context.setAttribute(CallContext.INPUT, interceptor.modifyInputBeforeSerialization(call.input(), context));

        interceptor.readBeforeSerialization(context);

        createRequest(call);

        interceptor.readAfterSerialization(context);

        interceptor.modifyRequestBeforeRetryLoop(context);

        // Retry loop

        interceptor.readBeforeAttempt(context);

        interceptor.modifyRequestBeforeSigning(context);

        interceptor.readBeforeSigning(context);

        // sign

        interceptor.readAfterSigning(context);

        interceptor.modifyRequestBeforeTransmit(context);

        interceptor.readBeforeTransmit(context);

        sendRequest(call);

        interceptor.readAfterTransmit(context);

        interceptor.modifyResponseBeforeDeserialization(context);

        interceptor.readResponseBeforeDeserialization(context);

        try {
            context.setAttribute(CallContext.OUTPUT, deserializeResponse(call));
        } catch (SdkException e) {
            context.setAttribute(CallContext.ERROR, e);
        }

        interceptor.readAfterDeserialization(context);

        try {
            IOShape output = context.expectAttribute(CallContext.OUTPUT);
            context.setAttribute(CallContext.OUTPUT, interceptor.modifyOutputBeforeAttemptCompletion(output, context));
        } catch (SdkException e) {
            context.setAttribute(CallContext.ERROR, e);
        }

        interceptor.readAfterAttempt(context);

        // End of retry loop

        try {
            IOShape output = context.expectAttribute(CallContext.OUTPUT);
            context.setAttribute(CallContext.OUTPUT, interceptor.modifyOutputBeforeCompletion(output, context));
        } catch (SdkException e) {
            context.setAttribute(CallContext.ERROR, e);
        }

        interceptor.readAfterExecution(context);

        SdkException error = context.getAttribute(CallContext.ERROR);
        if (error != null) {
            throw error;
        }

        return (O) context.expectAttribute(CallContext.OUTPUT);
    }

    abstract protected void createRequest(ClientCall<?, ?> call);

    abstract protected void sendRequest(ClientCall<?, ?> call);

    abstract protected <I extends IOShape, O extends IOShape> O deserializeResponse(ClientCall<I, O> call);
}
