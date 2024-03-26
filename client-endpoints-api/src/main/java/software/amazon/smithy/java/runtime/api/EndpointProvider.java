/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.api;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Resolves an endpoint for an operation.
 */
public interface EndpointProvider {
    /**
     * Resolves an endpoint using the provided parameters.
     *
     * @param request Request parameters used during endpoint resolution.
     * @return Returns the resolved endpoint.
     */
    Endpoint resolveEndpoint(EndpointProviderRequest request);

    /**
     * Create an endpoint provider that always returns the same endpoint.
     *
     * @param endpoint Endpoint to always resolve.
     * @return Returns the endpoint provider.
     */
    static EndpointProvider staticEndpoint(String endpoint) {
        try {
            return staticEndpoint(new URI(endpoint));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URI: " + e.getMessage(), e);
        }
    }

    /**
     * Create an endpoint provider that always returns the same endpoint.
     *
     * @param endpoint Endpoint to always resolve.
     * @return Returns the endpoint provider.
     */
    static EndpointProvider staticEndpoint(URI endpoint) {
        return params -> new Endpoint() {
            @Override
            public URI uri() {
                return endpoint;
            }

            @Override
            public <T> T endpointAttribute(EndpointKey<T> key) {
                return null;
            }
        };
    }
}
