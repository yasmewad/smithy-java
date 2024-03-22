/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.clienthttp;

import software.amazon.smithy.java.runtime.core.context.Context;
import software.amazon.smithy.java.runtime.http.SmithyHttpRequest;

public interface HttpSigner {
    SmithyHttpRequest sign(SmithyHttpRequest request, Context context);
}
