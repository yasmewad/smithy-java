/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.http.core;

import software.amazon.smithy.java.runtime.context.Context;

public interface SmithyHttpClient {
    SmithyHttpResponse send(SmithyHttpRequest request, Context context);
}
