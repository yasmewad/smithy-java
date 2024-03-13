/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.endpoint;

import software.amazon.smithy.java.runtime.util.Constant;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Common parameters used when resolving endpoints.
 */
public final class EndpointParams {
    /**
     * Contains the shape ID of the operation being called.
     */
    public static final Constant<ShapeId> OPERATION_ID = new Constant<>(ShapeId.class, "ID of the operation");
}
