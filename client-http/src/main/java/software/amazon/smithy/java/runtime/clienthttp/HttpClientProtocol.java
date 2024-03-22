/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.clienthttp;

import java.net.http.HttpHeaders;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import software.amazon.smithy.java.runtime.clientcore.ClientCall;
import software.amazon.smithy.java.runtime.clientcore.ClientProtocol;
import software.amazon.smithy.java.runtime.core.serde.Codec;
import software.amazon.smithy.java.runtime.core.serde.streaming.StreamPublisher;
import software.amazon.smithy.java.runtime.core.serde.streaming.StreamingShape;
import software.amazon.smithy.java.runtime.core.shapes.ModeledSdkException;
import software.amazon.smithy.java.runtime.core.shapes.SdkException;
import software.amazon.smithy.java.runtime.core.shapes.SdkShapeBuilder;
import software.amazon.smithy.java.runtime.core.shapes.SerializableShape;
import software.amazon.smithy.java.runtime.http.SmithyHttpClient;
import software.amazon.smithy.java.runtime.http.SmithyHttpRequest;
import software.amazon.smithy.java.runtime.http.SmithyHttpResponse;
import software.amazon.smithy.java.runtime.httpbinding.HttpBinding;

/**
 * An abstract HTTP-Based protocol.
 */
public abstract class HttpClientProtocol implements ClientProtocol<SmithyHttpRequest, SmithyHttpResponse> {

    private static final System.Logger LOGGER = System.getLogger(HttpClientProtocol.class.getName());
    private static final String X_AMZN_ERROR_TYPE = "X-Amzn-Errortype";
    private static final Set<String> TEXT_CONTENT_TYPES = Set.of("application/json", "application/xml");

    private final SmithyHttpClient client;
    private final Codec codec;

    public HttpClientProtocol(SmithyHttpClient client, Codec codec) {
        this.client = client;
        this.codec = codec;
    }

    /**
     * Create an unsigned HTTP request for the given call.
     *
     * @param codec Codec used to serialize structured payloads.
     * @param call  Call being invoked.s
     * @return Returns the serialized request.
     */
    abstract protected SmithyHttpRequest createHttpRequest(Codec codec, ClientCall<?, ?, ?> call);

    /**
     * Send the HTTP request and return the eventually completed response.
     *
     * @param call    Call being invoked.
     * @param client  Client used to send the request.
     * @param request Request to send.
     * @return the future response.
     */
    abstract CompletableFuture<SmithyHttpResponse> sendHttpRequest(
            ClientCall<?, ?, ?> call,
            SmithyHttpClient client,
            SmithyHttpRequest request
    );

    /**
     * Deserializes the HTTP response and returns the updated output stream.
     *
     * @param call     Call being invoked.
     * @param codec    Codec used to deserialize structured payloads.
     * @param request  Request that was sent.
     * @param response Response that was received.
     * @param builder  Builder to populate while deserializing.
     * @return Returns a future that contains the updated output stream, needed if the stream was consumed and must
     *         be replaced by an empty stream, or if other wrappers are applied.
     * @param <I> Input shape.
     * @param <O> Output shape.
     */
    protected abstract <I extends SerializableShape, O extends SerializableShape>
    CompletableFuture<StreamPublisher> deserializeHttpResponse(
            ClientCall<I, O, ?> call,
            Codec codec,
            SmithyHttpRequest request,
            SmithyHttpResponse response,
            SdkShapeBuilder<O> builder
    );

    @Override
    public final SmithyHttpRequest createRequest(ClientCall<?, ?, ?> call) {
        // Initialize the context with more HTTP information.
        call.context().setAttribute(HttpContext.PAYLOAD_CODEC, codec);
        var request = createHttpRequest(codec, call);
        call.context().setAttribute(HttpContext.HTTP_REQUEST, request);
        return request;
    }

    @Override
    public final CompletableFuture<SmithyHttpRequest> signRequest(ClientCall<?, ?, ?> call, SmithyHttpRequest request) {
        LOGGER.log(System.Logger.Level.TRACE, () -> "Signing HTTP request: " + request.startLine());

        var signer = call.context().getAttribute(HttpContext.SIGNER);
        if (signer == null) {
            LOGGER.log(System.Logger.Level.TRACE, () -> "No signer registered for request: " + request.startLine());
        } else {
            var signedRequest = signer.sign(request, call.context());
            LOGGER.log(System.Logger.Level.TRACE, () -> "Signed HTTP request: " + signedRequest.startLine());
            call.context().setAttribute(HttpContext.HTTP_REQUEST, signedRequest);
        }

        return CompletableFuture.completedFuture(request);
    }

    @Override
    public final CompletableFuture<SmithyHttpResponse> sendRequest(
            ClientCall<?, ?, ?> call,
            SmithyHttpRequest request
    ) {
        LOGGER.log(System.Logger.Level.TRACE, () -> "Sending HTTP request: " + request.startLine());
        return sendHttpRequest(call, client, request).thenApply(response -> {
            LOGGER.log(System.Logger.Level.TRACE, () -> "Got HTTP response: " + response.startLine());
            call.context().setAttribute(HttpContext.HTTP_RESPONSE, response);
            return response;
        });
    }

    @Override
    public final <I extends SerializableShape, O extends SerializableShape>
    CompletableFuture<StreamingShape<O, StreamPublisher>> deserializeResponse(
            ClientCall<I, O, ?> call,
            SmithyHttpRequest request,
            SmithyHttpResponse response
    ) {
        if (isSuccess(call, response)) {
            LOGGER.log(System.Logger.Level.TRACE, "Deserializing successful response with " + getClass().getName());
            var outputBuilder = call.createOutputBuilder(call.context(),
                                                         call.operation().outputSchema().id().toString());
            return deserializeHttpResponse(call, codec, request, response, outputBuilder)
                    .thenApply(updatedResponse -> {
                        LOGGER.log(System.Logger.Level.TRACE, "Deserialized HTTP response with " + getClass().getName()
                                                              + " into " + outputBuilder.getClass().getName());
                        O output = outputBuilder.errorCorrection().build();
                        // TODO: error handling from the builder.
                        LOGGER.log(System.Logger.Level.TRACE, "Successfully built " + output
                                                              + " from HTTP response with " + getClass().getName());
                        return StreamingShape.of(output, updatedResponse);
                    });
        } else {
            return createError(call, response).thenApply(e -> {
                throw e;
            });
        }
    }

    private boolean isSuccess(ClientCall<?, ?, ?> call, SmithyHttpResponse response) {
        // TODO: Better error checking.
        return response.statusCode() >= 200 && response.statusCode() <= 299;
    }

    /**
     * An overrideable error deserializer.
     *
     * @param call     Call being sent.
     * @param response HTTP response to deserialize.
     * @return Returns the deserialized error.
     */
    protected CompletableFuture<SdkException> createError(ClientCall<?, ?, ?> call, SmithyHttpResponse response) {
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
                .map(CompletableFuture::completedFuture)
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

    private CompletableFuture<SdkException> createErrorFromHints(
            ClientCall<?, ?, ?> call,
            SmithyHttpResponse response
    ) {
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

        String contentType = response.headers()
                .firstValue("Content-Type")
                .orElse("application/octet-stream")
                .toLowerCase(Locale.ENGLISH);

        if (!isText(contentType)) {
            return CompletableFuture.completedFuture(new SdkException(message.toString(), fault));
        }

        // Stream the response payload to the error message.
        return response.body().asString().thenApply(body -> {
            message.append(body);
            return new SdkException(message.toString(), fault);
        }).toCompletableFuture();
    }

    private boolean isText(String contentType) {
        return contentType.startsWith("text/")
                || contentType.contains("charset=utf-8")
                || contentType.endsWith("+json")
                || contentType.endsWith("+xml")
                || TEXT_CONTENT_TYPES.contains(contentType);
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
}
