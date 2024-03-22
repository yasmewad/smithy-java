/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.clienthttp;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import software.amazon.smithy.java.runtime.core.context.Context;
import software.amazon.smithy.java.runtime.core.serde.streaming.StreamPublisher;
import software.amazon.smithy.java.runtime.http.HttpRequestOptions;
import software.amazon.smithy.java.runtime.http.SmithyHttpClient;
import software.amazon.smithy.java.runtime.http.SmithyHttpRequest;
import software.amazon.smithy.java.runtime.http.SmithyHttpResponse;
import software.amazon.smithy.java.runtime.http.SmithyHttpVersion;

/**
 * Provides a SmithyHttpClient implementation using Java's {@link HttpClient}.
 */
public final class JavaHttpClient implements SmithyHttpClient {

    private static final System.Logger LOGGER = System.getLogger(JavaHttpClient.class.getName());
    private final HttpClient httpClient;

    /**
     * @param httpClient HTTP client to wrap.
     */
    public JavaHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public CompletableFuture<SmithyHttpResponse> send(SmithyHttpRequest request, Context context) {
        var bodyPublisher = request.body().contentLength() == 0
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.fromPublisher(request.body());

        HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder()
                .version(smithyToHttpVersion(request.httpVersion()))
                .method(request.method(), bodyPublisher)
                .uri(request.uri());

        Duration requestTimeout = context.getAttribute(HttpRequestOptions.REQUEST_TIMEOUT);
        if (requestTimeout != null) {
            httpRequestBuilder.timeout(requestTimeout);
        }

        for (var entry : request.headers().map().entrySet()) {
            for (var value : entry.getValue()) {
                httpRequestBuilder.header(entry.getKey(), value);
            }
        }
        HttpRequest httpRequest = httpRequestBuilder.build();

        LOGGER.log(System.Logger.Level.TRACE, () -> "Sending request " + httpRequest + " " + httpRequest.headers());

        return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofPublisher()).thenApply(response -> {
            var responsePublisher = response.body();
            var contentType = response.headers().firstValue("content-type").orElse(null);
            var contentLength = response.headers().firstValue("content-length").map(Long::valueOf).orElse(-1L);
            LOGGER.log(System.Logger.Level.TRACE, () -> "Got response: " + response
                                                        + "; Content-Type: " + contentType
                                                        + "; Content-Length: " + contentLength);
            return SmithyHttpResponse.builder()
                    .httpVersion(javaToSmithyVersion(response.version()))
                    .statusCode(response.statusCode())
                    .headers(response.headers())
                    // Flatten the List<ByteBuffer> to ByteBuffer.
                    .body(StreamPublisher.ofPublisher(new ListByteBufferToByteBuffer(responsePublisher),
                                                      contentType,
                                                      contentLength))
                    .build();
        });
    }

    private HttpClient.Version smithyToHttpVersion(SmithyHttpVersion version) {
        return switch (version) {
            case HTTP_1_1 -> HttpClient.Version.HTTP_1_1;
            case HTTP_2 -> HttpClient.Version.HTTP_2;
            default -> throw new UnsupportedOperationException("Unsupported HTTP version: " + version);
        };
    }

    private SmithyHttpVersion javaToSmithyVersion(HttpClient.Version version) {
        return switch (version) {
            case HTTP_1_1 -> SmithyHttpVersion.HTTP_1_1;
            case HTTP_2 -> SmithyHttpVersion.HTTP_2;
            default -> throw new UnsupportedOperationException("Unsupported HTTP version: " + version);
        };
    }

    private record ListByteBufferToByteBuffer(Flow.Publisher<List<ByteBuffer>> originalPublisher)
            implements Flow.Publisher<ByteBuffer> {
        @Override
            public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
                originalPublisher.subscribe(new Flow.Subscriber<>() {
                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        subscriber.onSubscribe(subscription);
                    }

                    @Override
                    public void onNext(List<ByteBuffer> item) {
                        item.forEach(subscriber::onNext);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        subscriber.onError(throwable);
                    }

                    @Override
                    public void onComplete() {
                        subscriber.onComplete();
                    }
                });
            }
        }
}
