/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.http.api;

/**
 * A blocking client responsible for sending HTTP requests.
 */
public interface SmithyHttpClient {
    /**
     * Send a blocking HTTP request and return the response.
     *
     * <p>TODO: define exceptions that are thrown.
     *
     * @param call Response call to send.
     * @return the response.
     */
    SmithyHttpResponse send(HttpClientCall call);
}
