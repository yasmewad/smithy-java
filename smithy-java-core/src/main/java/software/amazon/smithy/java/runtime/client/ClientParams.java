/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.client;

import software.amazon.smithy.java.runtime.shapes.IOShape;
import software.amazon.smithy.java.runtime.shapes.SdkSchema;
import software.amazon.smithy.java.runtime.util.Constant;
import software.amazon.smithy.java.runtime.util.Context;

/**
 * Context parameters made available to underlying transports like HTTP clients.
 */
public final class ClientParams {
    /**
     * Contains the input of the operation being sent.
     */
    public static final Constant<IOShape> INPUT = new Constant<>(IOShape.class, "Input shape");

    /**
     * Contains the schema of the operation being sent.
     */
    public static final Constant<SdkSchema> OPERATION_SCHEMA = new Constant<>(SdkSchema.class, "Operation schema");

    /**
     * Contains the input schema of the operation being sent.
     */
    public static final Constant<SdkSchema> INPUT_SCHEMA = new Constant<>(SdkSchema.class, "Input schema");

    /**
     * Contains the output schema of the operation being sent.
     */
    public static final Constant<SdkSchema> OUTPUT_SCHEMA = new Constant<>(SdkSchema.class, "Output schema");

    /**
     * Contains the context object of the operation being sent.
     */
    public static final Constant<Context> CALL_CONTEXT = new Constant<>(Context.class, "Call context");

    private ClientParams() {}
}
