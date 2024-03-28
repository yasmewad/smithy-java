/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.client.http;

import software.amazon.smithy.java.runtime.client.core.ClientCall;
import software.amazon.smithy.java.runtime.client.core.ClientTransport;
import software.amazon.smithy.java.runtime.http.api.HttpClientCall;
import software.amazon.smithy.java.runtime.http.api.SmithyHttpClient;
import software.amazon.smithy.java.runtime.http.api.SmithyHttpRequest;
import software.amazon.smithy.java.runtime.http.api.SmithyHttpResponse;

/**
 * Implements an HTTP {@link ClientTransport}.
 */
public final class HttpClientTransport implements ClientTransport<SmithyHttpRequest, SmithyHttpResponse> {

    private static final System.Logger LOGGER = System.getLogger(HttpClientTransport.class.getName());
    private final SmithyHttpClient client;

    /**
     * @param client Client to send requests.
     */
    public HttpClientTransport(SmithyHttpClient client) {
        this.client = client;
    }

    @Override
    public SmithyHttpResponse sendRequest(ClientCall<?, ?> call, SmithyHttpRequest request) {
        LOGGER.log(System.Logger.Level.TRACE, () -> "Sending HTTP request: " + request.startLine());
        var response = client.send(HttpClientCall.builder()
                .request(request)
                .properties(call.context().get(HttpContext.HTTP_PROPERTIES))
                .build());
        LOGGER.log(System.Logger.Level.TRACE, () -> "Got HTTP response: " + response.startLine());
        call.context().put(HttpContext.HTTP_RESPONSE, response);
        return response;
    }
}
