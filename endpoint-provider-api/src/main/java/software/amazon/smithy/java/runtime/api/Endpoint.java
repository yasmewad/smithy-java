/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.api;

import java.net.URI;
import software.amazon.smithy.java.runtime.context.Context;

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
     * Endpoint specific context map.
     *
     * @return Returns the context map.
     */
    Context context();
}
