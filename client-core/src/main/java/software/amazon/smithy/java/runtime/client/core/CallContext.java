/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.client.core;

import software.amazon.smithy.java.runtime.api.EndpointProvider;
import software.amazon.smithy.java.runtime.context.Constant;
import software.amazon.smithy.java.runtime.core.schema.SdkException;
import software.amazon.smithy.java.runtime.core.schema.SdkSchema;
import software.amazon.smithy.java.runtime.core.schema.SerializableShape;

/**
 * Context parameters made available to underlying transports like HTTP clients.
 */
public final class CallContext {
    /**
     * Contains the input of the operation being sent.
     */
    public static final Constant<SerializableShape> INPUT = new Constant<>(SerializableShape.class, "Input shape");

    /**
     * Deserialized output of the call.
     */
    public static final Constant<SerializableShape> OUTPUT = new Constant<>(SerializableShape.class, "Output");

    /**
     * Error encountered by the call that will be thrown.
     */
    public static final Constant<SdkException> ERROR = new Constant<>(SdkException.class, "Error");

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
     * The endpoint provider used to resolve the destination endpoint for a request.
     */
    public static final Constant<EndpointProvider> ENDPOINT_PROVIDER = new Constant<>(
            EndpointProvider.class, "EndpointProvider");

    private CallContext() {}
}
