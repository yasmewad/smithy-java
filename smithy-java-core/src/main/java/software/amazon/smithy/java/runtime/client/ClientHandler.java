/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.client;

import software.amazon.smithy.java.runtime.shapes.IOShape;

/**
 * A handler used by clients to send requests and deserialize responses.
 */
public interface ClientHandler {
    /**
     * Send the input and deserialize a response or throw errors.
     *
     * @param call Call to invoke.
     * @return Returns the deserialized response if successful.
     * @param <I> Input shape.
     * @param <O> Output shape.
     */
    <I extends IOShape, O extends IOShape> O send(ClientCall<I, O> call);
}
