/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.client.interceptor;

import java.util.List;
import software.amazon.smithy.java.runtime.shapes.IOShape;
import software.amazon.smithy.java.runtime.shapes.ModeledSdkException;
import software.amazon.smithy.java.runtime.util.Context;
import software.amazon.smithy.java.runtime.util.Either;

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
    public IOShape modifyInputBeforeSerialization(IOShape input, Context context) {
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
    public void modifyRequestBeforeRetryLoop(Context context) {
        for (var interceptor : interceptors) {
            interceptor.modifyRequestBeforeRetryLoop(context);
        }
    }

    @Override
    public void readBeforeAttempt(Context context) {
        for (var interceptor : interceptors) {
            interceptor.readBeforeAttempt(context);
        }
    }

    @Override
    public void modifyRequestBeforeSigning(Context context) {
        for (var interceptor : interceptors) {
            interceptor.modifyRequestBeforeSigning(context);
        }
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
    public void modifyRequestBeforeTransmit(Context context) {
        for (var interceptor : interceptors) {
            interceptor.modifyRequestBeforeTransmit(context);
        }
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
    public void modifyResponseBeforeDeserialization(Context context) {
        for (var interceptor : interceptors) {
            interceptor.modifyResponseBeforeDeserialization(context);
        }
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
    public Either<IOShape, ModeledSdkException> modifyOutputBeforeAttemptCompletion(IOShape output, Context context) {
        Either<IOShape, ModeledSdkException> result = Either.left(output);
        for (var interceptor : interceptors) {
            if (result.isRight()) {
                return result;
            }
            result = interceptor.modifyOutputBeforeAttemptCompletion(result.left(), context);
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
    public Either<IOShape, ModeledSdkException> modifyOutputBeforeCompletion(IOShape output, Context context) {
        Either<IOShape, ModeledSdkException> result = Either.left(output);
        for (var interceptor : interceptors) {
            if (result.isRight()) {
                return result;
            }
            result = interceptor.modifyOutputBeforeCompletion(result.left(), context);
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
