/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.net.http;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.java.runtime.net.StoppableInputStream;

public final class SmithyHttpResponseImpl implements SmithyHttpResponse {

    private final SmithyHttpVersion httpVersion;
    private final int statusCode;
    private final StoppableInputStream body;
    private final HttpHeaders headers;

    SmithyHttpResponseImpl(SmithyHttpResponse.Builder builder) {
        this.httpVersion = Objects.requireNonNull(builder.httpVersion);
        this.statusCode = builder.statusCode;
        this.body = Objects.requireNonNullElseGet(builder.body, StoppableInputStream::ofEmpty);
        this.headers = Objects.requireNonNullElseGet(builder.headers, () -> HttpHeaders.of(Map.of(), (k, v) -> true));
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(httpVersion).append(' ').append(statusCode).append(System.lineSeparator());
        headers.map().forEach((field, values) -> values.forEach(value -> {
            result.append(field).append(": ").append(value).append(System.lineSeparator());
        }));
        result.append(System.lineSeparator());

        try {
            // Include up to 16 KB of the output.
            result.append(new String(body.readNBytes(16384), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return result.toString();
    }

    @Override
    public int statusCode() {
        return statusCode;
    }

    @Override
    public SmithyHttpResponse withStatusCode(int statusCode) {
        return SmithyHttpResponse.builder().with(this).statusCode(statusCode).build();
    }

    @Override
    public SmithyHttpVersion httpVersion() {
        return httpVersion;
    }

    @Override
    public SmithyHttpResponse withHttpVersion(SmithyHttpVersion httpVersion) {
        return SmithyHttpResponse.builder().with(this).httpVersion(httpVersion).build();
    }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    @Override
    public SmithyHttpResponse withHeaders(HttpHeaders headers) {
        return SmithyHttpResponse.builder().with(this).headers(headers).build();
    }

    @Override
    public StoppableInputStream body() {
        return body;
    }

    @Override
    public SmithyHttpResponse withBody(StoppableInputStream body) {
        return SmithyHttpResponse.builder().with(this).body(body).build();
    }
}
