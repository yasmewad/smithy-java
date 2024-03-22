/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.clientcore;

import software.amazon.smithy.java.runtime.core.shapes.SdkException;
import software.amazon.smithy.java.runtime.core.shapes.SerializableShape;

public interface ClientProtocol<RequestT, ResponseT> {
    /**
     * Creates the underlying transport request.
     *
     * @param call Call being sent.
     * @return Returns the request to send.
     */
    RequestT createRequest(ClientCall<?, ?> call);

    /**
     * Signs and returns the underlying transport request.
     *
     * @param call    Call being sent.
     * @param request Request to sign.
     * @return Returns the signed request.
     */
    RequestT signRequest(ClientCall<?, ?> call, RequestT request);

    /**
     * Sends the underlying transport request and returns the response.
     *
     * @param call    Call being sent.
     * @param request Request to send.
     * @return Returns the response.
     */
    ResponseT sendRequest(ClientCall<?, ?> call, RequestT request);

    /**
     * Deserializes the output from the transport response or throws a modeled or unmodeled exception.
     *
     * @param call     Call being sent.
     * @param request  Request that was sent for this response.
     * @param response Response to deserialize.
     * @return the deserialized output shape.
     * @throws SdkException if an error occurs, including deserialized modeled errors and protocol errors.
     */
    <I extends SerializableShape, O extends SerializableShape> O deserializeResponse(
            ClientCall<I, O> call,
            RequestT request,
            ResponseT response
    );
}
