/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.client.http;

import software.amazon.smithy.java.runtime.context.Context;
import software.amazon.smithy.java.runtime.core.serde.Codec;
import software.amazon.smithy.java.runtime.http.core.SmithyHttpRequest;
import software.amazon.smithy.java.runtime.http.core.SmithyHttpResponse;

public final class HttpContext {

    public static final Context.Key<SmithyHttpRequest> HTTP_REQUEST = Context.key("HTTP Request");

    public static final Context.Key<SmithyHttpResponse> HTTP_RESPONSE = Context.key("HTTP Response");

    public static final Context.Key<Codec> PAYLOAD_CODEC = Context.key("Payload Codec");

    public static final Context.Key<HttpSigner> SIGNER = Context.key("HTTP signer");

    private HttpContext() {}
}
