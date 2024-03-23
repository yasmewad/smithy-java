/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.api;

import software.amazon.smithy.java.runtime.context.ReadableContext;

/**
 * An authentication scheme supported for the endpoint.
 */
public interface EndpointAuthScheme {
    /**
     * The ID of the auth scheme (e.g., "aws.auth#sigv4").
     *
     * @return the auth scheme ID.
     */
    String schemeId();

    /**
     * Typed properties associated with the auth scheme.
     *
     * @return the properties.
     */
    ReadableContext properties();
}
