/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.client.http;

import software.amazon.smithy.java.runtime.client.ClientCall;
import software.amazon.smithy.java.runtime.net.http.SmithyHttpClient;
import software.amazon.smithy.java.runtime.net.http.SmithyHttpRequest;
import software.amazon.smithy.java.runtime.net.http.SmithyHttpResponse;
import software.amazon.smithy.java.runtime.serde.Codec;
import software.amazon.smithy.java.runtime.serde.httpbinding.HttpBinding;
import software.amazon.smithy.java.runtime.shapes.IOShape;

/**
 * An HTTP-based protocol that uses HTTP binding traits.
 */
public class HttpBindingClientProtocol extends HttpClientProtocol {

    public HttpBindingClientProtocol(SmithyHttpClient client, Codec codec) {
        super(client, codec);
    }

    @Override
    protected SmithyHttpRequest createHttpRequest(Codec codec, ClientCall<?, ?> call) {
        return HttpBinding.requestSerializer()
                .operation(call.operation().schema())
                .payloadCodec(codec)
                .shapeValue(call.input())
                .endpoint(call.endpoint().uri())
                .serializeRequest();
    }

    @Override
    protected <I extends IOShape, O extends IOShape> void deserializeHttpResponse(
            ClientCall<I, O> call,
            Codec codec,
            SmithyHttpRequest request,
            SmithyHttpResponse response,
            IOShape.Builder<O> builder
    ) {
        HttpBinding.responseDeserializer()
                .payloadCodec(codec)
                .outputShapeBuilder(builder)
                .response(response)
                .deserialize();
    }

    @Override
    SmithyHttpResponse sendHttpRequest(ClientCall<?, ?> call, SmithyHttpClient client, SmithyHttpRequest request) {
        return client.send(request, call.context());
    }
}
