/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.client.interceptor;

import java.util.List;
import software.amazon.smithy.java.runtime.context.Context;
import software.amazon.smithy.java.runtime.core.schema.SerializableShape;

final class ClientInterceptorChain implements ClientInterceptor {

    private final List<ClientInterceptor> interceptors;

    public ClientInterceptorChain(List<ClientInterceptor> interceptors) {
        this.interceptors = interceptors;
    }

    @Override
    public void readBeforeExecution(Context context) {
        for (var interceptor : interceptors) {
            interceptor.readBeforeExecution(context);
        }
    }

    @Override
    public <I extends SerializableShape> I modifyInputBeforeSerialization(I input, Context context) {
        for (var interceptor : interceptors) {
            input = interceptor.modifyInputBeforeSerialization(input, context);
        }
        return input;
    }

    @Override
    public void readBeforeSerialization(Context context) {
        for (var interceptor : interceptors) {
            interceptor.readBeforeSerialization(context);
        }
    }

    @Override
    public void readAfterSerialization(Context context) {
        for (var interceptor : interceptors) {
            interceptor.readAfterSerialization(context);
        }
    }

    @Override
    public <T> T modifyRequestBeforeRetryLoop(Context context, T request) {
        for (var interceptor : interceptors) {
            request = interceptor.modifyRequestBeforeRetryLoop(context, request);
        }
        return request;
    }

    @Override
    public void readBeforeAttempt(Context context) {
        for (var interceptor : interceptors) {
            interceptor.readBeforeAttempt(context);
        }
    }

    @Override
    public <T> T modifyRequestBeforeSigning(Context context, T request) {
        for (var interceptor : interceptors) {
            request = interceptor.modifyRequestBeforeSigning(context, request);
        }
        return request;
    }

    @Override
    public void readBeforeSigning(Context context) {
        for (var interceptor : interceptors) {
            interceptor.readBeforeSigning(context);
        }
    }

    @Override
    public void readAfterSigning(Context context) {
        for (var interceptor : interceptors) {
            interceptor.readAfterSigning(context);
        }
    }

    @Override
    public <T> T modifyRequestBeforeTransmit(Context context, T request) {
        for (var interceptor : interceptors) {
            request = interceptor.modifyRequestBeforeTransmit(context, request);
        }
        return request;
    }

    @Override
    public void readBeforeTransmit(Context context) {
        for (var interceptor : interceptors) {
            interceptor.readBeforeTransmit(context);
        }
    }

    @Override
    public void readAfterTransmit(Context context) {
        for (var interceptor : interceptors) {
            interceptor.readAfterTransmit(context);
        }
    }

    @Override
    public <T> T modifyResponseBeforeDeserialization(Context context, T request) {
        for (var interceptor : interceptors) {
            request = interceptor.modifyResponseBeforeDeserialization(context, request);
        }
        return request;
    }

    @Override
    public void readResponseBeforeDeserialization(Context context) {
        for (var interceptor : interceptors) {
            interceptor.readResponseBeforeDeserialization(context);
        }
    }

    @Override
    public void readAfterDeserialization(Context context) {
        for (var interceptor : interceptors) {
            interceptor.readAfterDeserialization(context);
        }
    }

    @Override
    public <O extends SerializableShape> O modifyOutputBeforeAttemptCompletion(O output, Context context) {
        O result = output;
        for (var interceptor : interceptors) {
            result = interceptor.modifyOutputBeforeAttemptCompletion(result, context);
        }
        return result;
    }

    @Override
    public void readAfterAttempt(Context context) {
        for (var interceptor : interceptors) {
            interceptor.readAfterAttempt(context);
        }
    }

    @Override
    public <O extends SerializableShape> O modifyOutputBeforeCompletion(O output, Context context) {
        O result = output;
        for (var interceptor : interceptors) {
            result = interceptor.modifyOutputBeforeCompletion(result, context);
        }
        return result;
    }

    @Override
    public void readAfterExecution(Context context) {
        for (var interceptor : interceptors) {
            interceptor.readAfterExecution(context);
        }
    }
}
