/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.client.auth.api;

import software.amazon.smithy.java.runtime.context.ReadableContext;

record AuthSchemeOptionRecord(
        String schemeId,
        ReadableContext identityProperties,
        ReadableContext signerProperties
) implements AuthSchemeOption {}
