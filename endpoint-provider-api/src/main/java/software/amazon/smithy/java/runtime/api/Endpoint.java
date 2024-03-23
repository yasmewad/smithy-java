/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.api;

import java.net.URI;
import java.util.List;
import software.amazon.smithy.java.runtime.context.ReadableContext;

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
     * Endpoint specific properties.
     *
     * @return Returns the properties.
     */
    ReadableContext properties();

    /**
     * Get the list of auth schemes supported at this endpoint.
     *
     * @return supported auth schemes.
     */
    default List<EndpointAuthScheme> supportedAuthSchemes() {
        return List.of();
    }
}
