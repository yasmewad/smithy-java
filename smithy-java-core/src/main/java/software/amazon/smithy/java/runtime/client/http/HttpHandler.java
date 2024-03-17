/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.client.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import software.amazon.smithy.java.runtime.client.ClientCall;
import software.amazon.smithy.java.runtime.client.InterceptorHandler;
import software.amazon.smithy.java.runtime.net.http.SmithyHttpClient;
import software.amazon.smithy.java.runtime.net.http.SmithyHttpResponse;
import software.amazon.smithy.java.runtime.serde.Codec;
import software.amazon.smithy.java.runtime.serde.httpbinding.HttpBinding;
import software.amazon.smithy.java.runtime.shapes.IOShape;
import software.amazon.smithy.java.runtime.shapes.ModeledSdkException;
import software.amazon.smithy.java.runtime.shapes.SdkException;
import software.amazon.smithy.java.runtime.shapes.SdkShapeBuilder;

/**
 * An abstract class for implementing client handlers that use HTTP.
 */
public abstract class HttpHandler extends InterceptorHandler {

    private static final System.Logger LOGGER = System.getLogger(HttpHandler.class.getName());
    private static final String X_AMZN_ERROR_TYPE = "X-Amzn-Errortype";

    private final SmithyHttpClient client;
    private final Codec codec;

    public HttpHandler(SmithyHttpClient client, Codec codec) {
        this.client = client;
        this.codec = codec;
    }

    protected SmithyHttpClient client() {
        return client;
    }

    protected Codec codec() {
        return codec;
    }

    @Override
    protected void sendRequest(ClientCall<?, ?> call) {
        var request = call.context().expectAttribute(HttpContext.HTTP_REQUEST);
        var response = client().send(request, call.context());
        call.context().setAttribute(HttpContext.HTTP_RESPONSE, response);
    }

    @Override
    protected <I extends IOShape, O extends IOShape> O deserializeResponse(ClientCall<I, O> call) {
        SmithyHttpResponse response = call.context().expectAttribute(HttpContext.HTTP_RESPONSE);
        if (isSuccess(call, response)) {
            var outputBuilder = call.createOutputBuilder(call.context(), call.operation().outputSchema().id());
            deserializeResponse(outputBuilder, codec, response);
            return outputBuilder.errorCorrection().build();
        } else {
            throw createError(call, response);
        }
    }

    abstract protected void deserializeResponse(
            IOShape.Builder<?> builder,
            Codec codec,
            SmithyHttpResponse response
    );

    private boolean isSuccess(ClientCall<?, ?> call, SmithyHttpResponse response) {
        // TODO: Better error checking.
        return response.statusCode() >= 200 && response.statusCode() <= 299;
    }

    private SdkException createError(ClientCall<?, ?> call, SmithyHttpResponse response) {
        return response
                .headers()
                // Grab the error ID from the header first.
                .firstValue(X_AMZN_ERROR_TYPE)
                // If not in the header, check the payload for __type.
                .or(() -> {
                    // TODO: check payload for type.
                    return Optional.empty();
                })
                // Attempt to match the extracted error ID to a modeled error type.
                .flatMap(errorId -> call.createExceptionBuilder(call.context(), errorId)
                        .<SdkException> map(error -> createModeledException(response, error)))
                // If no error was matched, then create an error from protocol hints.
                .orElseGet(() -> createErrorFromHints(call, response));
    }

    private ModeledSdkException createModeledException(
            SmithyHttpResponse response,
            SdkShapeBuilder<ModeledSdkException> error
    ) {
        // Deserialize the error response.
        HttpBinding.responseDeserializer()
                .payloadCodec(codec)
                .errorShapeBuilder(error)
                .response(response)
                .deserialize();
        return error.errorCorrection().build();
    }

    private SdkException createErrorFromHints(ClientCall<?, ?> call, SmithyHttpResponse response) {
        LOGGER.log(System.Logger.Level.WARNING, () -> "Unknown " + response.statusCode() + " error response from "
                                                      + call.operation().schema().id());

        SdkException.Fault fault = determineFault(response.statusCode());
        StringBuilder message = new StringBuilder();
        message.append(switch (fault) {
            case CLIENT -> "Client error ";
            case SERVER -> "Server error ";
            default -> "Unknown error ";
        });

        message.append("encountered from operation ").append(call.operation().schema().id());
        message.append(System.lineSeparator());
        message.append(response.httpVersion()).append(' ').append(response.statusCode()).append(System.lineSeparator());
        writeHeaders(message, response.headers());
        message.append(System.lineSeparator());
        writeBody(message, response);

        return new SdkException(message.toString(), fault);
    }

    private SdkException.Fault determineFault(int statusCode) {
        if (statusCode >= 400 && statusCode <= 499) {
            return SdkException.Fault.CLIENT;
        } else if (statusCode >= 500 && statusCode <= 599) {
            return SdkException.Fault.SERVER;
        } else {
            return SdkException.Fault.OTHER;
        }
    }

    private void writeHeaders(StringBuilder builder, HttpHeaders headers) {
        headers.map().forEach((field, values) -> {
            values.forEach(value -> {
                builder.append(field).append(": ").append(value).append(System.lineSeparator());
            });
        });
    }

    private void writeBody(StringBuilder builder, SmithyHttpResponse response) {
        try (InputStream responseStream = response.body()) {
            // Include up to 16 KB of the output.
            builder.append(new String(responseStream.readNBytes(16384), StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOGGER.log(System.Logger.Level.WARNING, () -> "Unable to read error response payload from " + response);
        }
    }
}
