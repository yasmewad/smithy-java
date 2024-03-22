/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.client.http;

import software.amazon.smithy.java.runtime.client.core.ClientCall;
import software.amazon.smithy.java.runtime.core.serde.Codec;
import software.amazon.smithy.java.runtime.core.shapes.SdkShapeBuilder;
import software.amazon.smithy.java.runtime.core.shapes.SerializableShape;
import software.amazon.smithy.java.runtime.http.core.SmithyHttpClient;
import software.amazon.smithy.java.runtime.http.core.SmithyHttpRequest;
import software.amazon.smithy.java.runtime.http.core.SmithyHttpResponse;
import software.amazon.smithy.java.runtime.http.binding.HttpBinding;

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
                .payload(call.requestInputStream().orElse(null))
                .payloadCodec(codec)
                .shapeValue(call.input())
                .endpoint(call.endpoint().uri())
                .serializeRequest();
    }

    @Override
    protected <I extends SerializableShape, O extends SerializableShape> void deserializeHttpResponse(
            ClientCall<I, O> call,
            Codec codec,
            SmithyHttpRequest request,
            SmithyHttpResponse response,
            SdkShapeBuilder<O> builder
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
