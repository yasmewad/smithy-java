/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.http;

import java.util.concurrent.CompletableFuture;
import software.amazon.smithy.java.runtime.core.context.Context;

public interface SmithyHttpClient {
    CompletableFuture<SmithyHttpResponse> send(SmithyHttpRequest request, Context context);
}
