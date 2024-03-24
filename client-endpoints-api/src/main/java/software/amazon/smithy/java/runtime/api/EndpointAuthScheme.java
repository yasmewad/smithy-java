/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.api;

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
     * Get an auth scheme-specific property using a strongly typed key, or {@code null}.
     *
     * @param key Key of the property to get.
     * @return Returns the value or null of not found.
     */
    <T> T attribute(EndpointKey<T> key);
}
