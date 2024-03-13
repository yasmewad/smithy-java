/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.net.http;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.java.runtime.net.StoppableInputStream;

final class SmithyHttpRequestImpl implements SmithyHttpRequest {

    private final SmithyHttpVersion httpVersion;
    private final String method;
    private final URI uri;
    private final StoppableInputStream body;
    private final HttpHeaders headers;

    SmithyHttpRequestImpl(SmithyHttpRequest.Builder builder) {
        this.httpVersion = Objects.requireNonNull(builder.httpVersion);
        this.method = Objects.requireNonNull(builder.method);
        this.uri = Objects.requireNonNull(builder.uri);
        this.body = Objects.requireNonNullElseGet(builder.body, StoppableInputStream::ofEmpty);
        this.headers = Objects.requireNonNullElseGet(builder.headers, () -> HttpHeaders.of(Map.of(), (k, v) -> true));
    }

    @Override
    public String method() {
        return method;
    }

    @Override
    public SmithyHttpRequest withMethod(String method) {
        return SmithyHttpRequest.builder().with(this).method(method).build();
    }

    @Override
    public URI uri() {
        return uri;
    }

    @Override
    public SmithyHttpRequest withUri(URI uri) {
        return SmithyHttpRequest.builder().with(this).uri(uri).build();
    }

    @Override
    public SmithyHttpVersion httpVersion() {
        return httpVersion;
    }

    @Override
    public SmithyHttpRequest withHttpVersion(SmithyHttpVersion httpVersion) {
        return SmithyHttpRequest.builder().with(this).httpVersion(httpVersion).build();
    }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    @Override
    public SmithyHttpRequest withHeaders(HttpHeaders headers) {
        return SmithyHttpRequest.builder().with(this).headers(headers).build();
    }

    @Override
    public StoppableInputStream body() {
        return body;
    }

    @Override
    public SmithyHttpRequest withBody(StoppableInputStream body) {
        return SmithyHttpRequest.builder().with(this).body(body).build();
    }
}
