/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.api;

import software.amazon.smithy.java.runtime.context.Constant;

/**
 * Common parameters used when resolving endpoints.
 */
public final class EndpointParams {
    /**
     * Contains the name of the operation being called.
     */
    public static final Constant<String> OPERATION_NAME = new Constant<>(String.class, "Name of the operation");
}
