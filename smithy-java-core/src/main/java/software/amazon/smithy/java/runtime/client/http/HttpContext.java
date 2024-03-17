/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.client.http;

import software.amazon.smithy.java.runtime.net.http.SmithyHttpRequest;
import software.amazon.smithy.java.runtime.net.http.SmithyHttpResponse;
import software.amazon.smithy.java.runtime.util.Constant;

public final class HttpContext {

    public static final Constant<SmithyHttpRequest> HTTP_REQUEST = new Constant<>(
            SmithyHttpRequest.class, "HTTP Request");

    public static final Constant<SmithyHttpResponse> HTTP_RESPONSE = new Constant<>(
            SmithyHttpResponse.class, "HTTP Response");

    private HttpContext() {}
}
