/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.client.interceptor;

import java.util.List;
import software.amazon.smithy.java.runtime.shapes.IOShape;
import software.amazon.smithy.java.runtime.util.Context;

// TODO: why non-blocking? Does that mean credential fetching is external to interceptors? What about short-circuiting?
public interface ClientInterceptor {

    ClientInterceptor NOOP = new ClientInterceptor() {};

    static ClientInterceptor chain(List<ClientInterceptor> interceptors) {
        return new ClientInterceptorChain(interceptors);
    }

    default void readBeforeExecution(Context context) {}

    default IOShape modifyInputBeforeSerialization(IOShape input, Context context) {
        return input;
    }

    default void readBeforeSerialization(Context context) {}

    default void readAfterSerialization(Context context) {}

    // Could modify "TransmitRequest"
    default void modifyRequestBeforeRetryLoop(Context context) {}

    default void readBeforeAttempt(Context context) {}

    // Could modify "TransmitRequest"
    default void modifyRequestBeforeSigning(Context context) {}

    default void readBeforeSigning(Context context) {}

    default void readAfterSigning(Context context) {}

    // Could modify "TransmitRequest"
    default void modifyRequestBeforeTransmit(Context context) {}

    default void readBeforeTransmit(Context context) {}

    default void readAfterTransmit(Context context) {}

    // Could modify "TransmitResponse"
    default void modifyResponseBeforeDeserialization(Context context) {}

    default void readResponseBeforeDeserialization(Context context) {}

    default void readAfterDeserialization(Context context) {}

    default IOShape modifyOutputBeforeAttemptCompletion(IOShape output, Context context) {
        return output;
    }

    default void readAfterAttempt(Context context) {}

    default IOShape modifyOutputBeforeCompletion(IOShape output, Context context) {
        return output;
    }

    default void readAfterExecution(Context context) {}
}
