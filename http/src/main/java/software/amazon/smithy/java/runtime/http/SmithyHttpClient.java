/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.http;

import software.amazon.smithy.java.runtime.core.context.Context;

public interface SmithyHttpClient {
    SmithyHttpResponse send(SmithyHttpRequest request, Context context);
}
