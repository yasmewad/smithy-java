/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.client;

import software.amazon.smithy.java.runtime.shapes.IOShape;
import software.amazon.smithy.java.runtime.shapes.SdkException;

public interface ClientProtocol {
    /**
     * Creates the underlying transport request and adds it to the call's context.
     *
     * @param call Call being sent.
     */
    void createRequest(ClientCall<?, ?> call);

    /**
     * Signs the underlying transport request and updates it on the call's context.
     *
     * @param call Call being sent.
     */
    void signRequest(ClientCall<?, ?> call);

    /**
     * Sends the underlying transport request, and sets the transport response on the call's context.
     *
     * @param call Call being sent.
     */
    void sendRequest(ClientCall<?, ?> call);

    /**
     * Deserializes the output from the transport response or throws a modeled or unmodeled exception.
     *
     * @param call Call being sent.
     * @throws SdkException if an error occurs, including deserialized modeled errors and protocol errors.
     */
    <I extends IOShape, O extends IOShape> O deserializeResponse(ClientCall<I, O> call);
}
