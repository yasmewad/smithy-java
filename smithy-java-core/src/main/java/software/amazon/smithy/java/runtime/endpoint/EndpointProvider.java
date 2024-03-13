/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.endpoint;

import java.net.URI;
import java.net.URISyntaxException;
import software.amazon.smithy.java.runtime.serde.SdkSerdeException;
import software.amazon.smithy.java.runtime.util.Context;

/**
 * Resolves an endpoint for an operation.
 *
 * @param <T> Context type.
 */
public interface EndpointProvider<T extends Context> {
    /**
     * Resolves an endpoint using the provided context.
     *
     * @param parameters Context parameters used during endpoint resolution.
     * @return Returns the resolved endpoint.
     */
    Endpoint resolveEndpoint(T parameters);

    /**
     * Create an endpoint provider that always returns the same endpoint.
     *
     * @param endpoint Endpoint to always resolve.
     * @return Returns the endpoint provider.
     */
    static EndpointProvider<Context> staticEndpoint(String endpoint) {
        try {
            return staticEndpoint(new URI(endpoint));
        } catch (URISyntaxException e) {
            throw new SdkSerdeException("Invalid URI: " + e.getMessage(), e);
        }
    }

    /**
     * Create an endpoint provider that always returns the same endpoint.
     *
     * @param endpoint Endpoint to always resolve.
     * @return Returns the endpoint provider.
     */
    static EndpointProvider<Context> staticEndpoint(URI endpoint) {
        Context context = Context.create();
        return params -> new Endpoint() {
            @Override
            public URI uri() {
                return endpoint;
            }

            @Override
            public Context context() {
                return context;
            }
        };
    }
}
