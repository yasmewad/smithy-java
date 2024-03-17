/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.client.http;

import software.amazon.smithy.java.runtime.client.ClientCall;
import software.amazon.smithy.java.runtime.net.http.SmithyHttpClient;
import software.amazon.smithy.java.runtime.net.http.SmithyHttpResponse;
import software.amazon.smithy.java.runtime.serde.Codec;
import software.amazon.smithy.java.runtime.serde.httpbinding.HttpBinding;
import software.amazon.smithy.java.runtime.shapes.IOShape;
import software.amazon.smithy.java.runtime.util.Context;

/**
 * An abstract class for implementing handlers for protocols that use HTTP bindings.
 */
public abstract class HttpBindingClientHandler extends HttpHandler {

    public HttpBindingClientHandler(SmithyHttpClient client, Codec codec) {
        super(client, codec);
    }

    @Override
    protected final void createRequest(ClientCall<?, ?> call) {
        call.context().setAttribute(HttpContext.HTTP_REQUEST, HttpBinding.requestSerializer()
                .operation(call.operation().schema())
                .payloadCodec(codec())
                .shapeValue(call.input())
                .endpoint(call.endpoint().uri())
                .serializeRequest());
    }

    @Override
    protected final void deserializeResponse(
            IOShape.Builder<?> outputBuilder,
            Codec codec,
            SmithyHttpResponse response
    ) {
        HttpBinding.responseDeserializer()
                .payloadCodec(codec)
                .outputShapeBuilder(outputBuilder)
                .response(response)
                .deserialize();
    }
}
