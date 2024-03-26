/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.api;

import java.net.URI;
import java.util.List;

/**
 * A resolved endpoint.
 */
public interface Endpoint {
    /**
     * The endpoint URI.
     *
     * @return URI of the endpoint.
     */
    URI uri();

    /**
     * Get an endpoint-specific property using a strongly typed key, or {@code null}.
     *
     * <p>For example, in some AWS use cases, this might contain HTTP headers to add to each request.
     *
     * @param key Endpoint key to get.
     * @return Returns the value or null of not found.
     */
    <T> T endpointAttribute(EndpointKey<T> key);

    /**
     * Get the list of auth schemes supported at this endpoint.
     *
     * @return supported auth schemes.
     */
    default List<EndpointAuthScheme> supportedAuthSchemes() {
        return List.of();
    }
}
