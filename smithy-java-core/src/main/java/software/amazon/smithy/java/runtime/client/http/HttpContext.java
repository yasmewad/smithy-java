/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.client.http;

import software.amazon.smithy.java.runtime.net.http.SmithyHttpRequest;
import software.amazon.smithy.java.runtime.net.http.SmithyHttpResponse;
import software.amazon.smithy.java.runtime.serde.Codec;
import software.amazon.smithy.java.runtime.util.Constant;

public final class HttpContext {

    public static final Constant<SmithyHttpRequest> HTTP_REQUEST = new Constant<>(
            SmithyHttpRequest.class, "HTTP Request");

    public static final Constant<SmithyHttpResponse> HTTP_RESPONSE = new Constant<>(
            SmithyHttpResponse.class, "HTTP Response");

    public static final Constant<Codec> PAYLOAD_CODEC = new Constant<>(Codec.class, "Payload Codec");

    public static final Constant<HttpSigner> SIGNER = new Constant<>(HttpSigner.class, "HTTP signer");

    private HttpContext() {}
}
