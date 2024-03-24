/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.api;

/**
 * Common parameters used when resolving endpoints.
 */
public final class EndpointParams {
    /**
     * Contains the name of the operation being called.
     */
    public static final EndpointKey<String> OPERATION_NAME = EndpointKey.of("Name of the operation");
}
