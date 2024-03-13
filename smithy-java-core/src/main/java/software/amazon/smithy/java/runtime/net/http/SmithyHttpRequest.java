/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.net.http;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpHeaders;
import software.amazon.smithy.java.runtime.net.StoppableInputStream;
import software.amazon.smithy.utils.SmithyBuilder;

public interface SmithyHttpRequest extends SmithyHttpMessage {

    String method();

    SmithyHttpRequest withMethod(String method);

    URI uri();

    SmithyHttpRequest withUri(URI uri);

    static Builder builder() {
        return new Builder();
    }

    final class Builder implements SmithyBuilder<SmithyHttpRequest> {

        String method;
        URI uri;
        StoppableInputStream body;
        HttpHeaders headers;
        SmithyHttpVersion httpVersion = SmithyHttpVersion.HTTP_1_1;

        private Builder() {}

        public Builder httpVersion(SmithyHttpVersion httpVersion) {
            this.httpVersion = httpVersion;
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder uri(URI uri) {
            this.uri = uri;
            return this;
        }

        public Builder body(StoppableInputStream body) {
            this.body = body;
            return this;
        }

        public Builder body(InputStream body) {
            return body(StoppableInputStream.of(body));
        }

        public Builder headers(HttpHeaders headers) {
            this.headers = headers;
            return this;
        }

        public Builder with(SmithyHttpRequest request) {
            this.httpVersion = request.httpVersion();
            this.headers = request.headers();
            this.body = request.body();
            this.method = request.method();
            this.uri = request.uri();
            return this;
        }

        @Override
        public SmithyHttpRequest build() {
            return new SmithyHttpRequestImpl(this);
        }
    }
}
