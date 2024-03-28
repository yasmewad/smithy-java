/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.client.core;

/**
 * Responsible for sending a request and receiving a response.
 *
 * @param <RequestT>  Transport message type.
 * @param <ResponseT> Transport response type.
 */
public interface ClientTransport<RequestT, ResponseT> {
    /**
     * Sends the underlying transport request and returns the response.
     *
     * @param call    Call being sent.
     * @param request Request to send.
     * @return Returns the response.
     */
    ResponseT sendRequest(ClientCall<?, ?> call, RequestT request);
}
