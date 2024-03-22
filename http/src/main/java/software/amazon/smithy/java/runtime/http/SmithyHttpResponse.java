/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.http;

import java.net.http.HttpHeaders;
import software.amazon.smithy.java.runtime.core.serde.DataStream;

public interface SmithyHttpResponse extends SmithyHttpMessage {

    @Override
    default String startLine() {
        return httpVersion() + " " + statusCode();
    }

    int statusCode();

    SmithyHttpResponse withStatusCode(int statusCode);

    static Builder builder() {
        return new Builder();
    }

    final class Builder {

        int statusCode;
        DataStream body;
        HttpHeaders headers;
        SmithyHttpVersion httpVersion = SmithyHttpVersion.HTTP_1_1;

        private Builder() {}

        public Builder httpVersion(SmithyHttpVersion httpVersion) {
            this.httpVersion = httpVersion;
            return this;
        }

        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder body(DataStream body) {
            this.body = body;
            return this;
        }

        public Builder headers(HttpHeaders headers) {
            this.headers = headers;
            return this;
        }

        public Builder with(SmithyHttpResponse response) {
            this.httpVersion = response.httpVersion();
            this.headers = response.headers();
            this.body = response.body();
            this.statusCode = response.statusCode();
            return this;
        }

        public SmithyHttpResponse build() {
            return new SmithyHttpResponseImpl(this);
        }
    }
}
