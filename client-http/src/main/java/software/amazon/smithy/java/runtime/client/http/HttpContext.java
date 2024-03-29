/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.client.http;

import java.time.Duration;
import software.amazon.smithy.java.runtime.core.Context;
import software.amazon.smithy.java.runtime.core.serde.Codec;
import software.amazon.smithy.java.runtime.http.api.SmithyHttpRequest;
import software.amazon.smithy.java.runtime.http.api.SmithyHttpResponse;

public final class HttpContext {

    public static final Context.Key<SmithyHttpRequest> HTTP_REQUEST = Context.key("HTTP Request");

    public static final Context.Key<SmithyHttpResponse> HTTP_RESPONSE = Context.key("HTTP Response");

    public static final Context.Key<Codec> HTTP_PAYLOAD_CODEC = Context.key("HTTP payload Codec");

    /**
     * The time from when an HTTP request is sent, and when the response is received. If the response is not
     * received in time, then the request is considered timed out. This setting does not apply to streaming
     * operations.
     */
    public static final Context.Key<Duration> HTTP_REQUEST_TIMEOUT = Context.key("HTTP.RequestTimeout");

    private HttpContext() {}
}
