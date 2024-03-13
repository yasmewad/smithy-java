/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.net.http;

import software.amazon.smithy.java.runtime.util.Context;

public interface SmithyHttpClient {
    SmithyHttpResponse send(SmithyHttpRequest request, Context context);
}
