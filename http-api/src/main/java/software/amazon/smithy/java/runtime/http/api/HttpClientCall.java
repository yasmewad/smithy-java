/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.http.api;

import java.util.Objects;
import software.amazon.smithy.java.runtime.context.Context;

/**
 * Contains the context necessary to send an HTTP request.
 */
public final class HttpClientCall {

    private final SmithyHttpRequest request;
    private final Context context;

    private HttpClientCall(Builder builder) {
        this.request = Objects.requireNonNull(builder.request, "request is null");
        this.context = Objects.requireNonNullElseGet(builder.context, Context::create);
    }

    public static Builder builder() {
        return new Builder();
    }

    public SmithyHttpRequest request() {
        return request;
    }

    public Context context() {
        return context;
    }

    public static final class Builder {

        private SmithyHttpRequest request;
        private Context context;

        private Builder() {}

        public HttpClientCall build() {
            return new HttpClientCall(this);
        }

        public Builder request(SmithyHttpRequest request) {
            this.request = request;
            return this;
        }

        public Builder context(Context context) {
            this.context = context;
            return this;
        }
    }
}
