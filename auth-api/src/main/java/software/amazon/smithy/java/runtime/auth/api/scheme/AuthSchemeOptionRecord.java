/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.auth.api.scheme;

import software.amazon.smithy.java.runtime.context.Context;

record AuthSchemeOptionRecord(
        String schemeId,
        Context identityProperties,
        Context signerProperties
) implements AuthSchemeOption {}
