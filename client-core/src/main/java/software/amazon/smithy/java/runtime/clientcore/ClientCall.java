/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.clientcore;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import software.amazon.smithy.java.runtime.clientinterceptor.ClientInterceptor;
import software.amazon.smithy.java.runtime.core.context.Context;
import software.amazon.smithy.java.runtime.core.serde.streaming.StreamHandler;
import software.amazon.smithy.java.runtime.core.serde.streaming.StreamPublisher;
import software.amazon.smithy.java.runtime.core.shapes.ModeledSdkException;
import software.amazon.smithy.java.runtime.core.shapes.SdkOperation;
import software.amazon.smithy.java.runtime.core.shapes.SdkShapeBuilder;
import software.amazon.smithy.java.runtime.core.shapes.SerializableShape;
import software.amazon.smithy.java.runtime.endpointprovider.Endpoint;

/**
 * Contains the information needed to send a request from a client using a protocol.
 *
 * @param <I> Input to send.
 * @param <O> Output to return.
 * @param <OutputStreamT> Streaming output type.
 */
public interface ClientCall<I extends SerializableShape, O extends SerializableShape, OutputStreamT> {
    /**
     * Get the input of the operation.
     *
     * @return Return the operation input.
     */
    I input();

    /**
     * Get the operation definition.
     *
     * @return Returns the operation definition.
     */
    SdkOperation<I, O> operation();

    /**
     * The endpoint of the call.
     *
     * @return Returns the resolved endpoint of the call.
     */
    Endpoint endpoint();

    /**
     * Gets the client interceptor used by the call.
     *
     * @return Returns the client interceptor.
     */
    ClientInterceptor interceptor();

    /**
     * Get the context of the call.
     *
     * @return Return the call context.
     */
    Context context();

    /**
     * Contains the optionally present input stream.
     *
     * @return the optionally present input stream to send in a request.
     */
    Optional<StreamPublisher> requestStream();

    /**
     * Contains the non-null stream handler used to handle the output stream of the response.
     *
     * <p>This method may be called multiple times if retries occur.
     *
     * @return the stream handler used to process streaming output, if present.
     */
    StreamHandler<O, OutputStreamT> responseStreamHandler();

    /**
     * Create a builder for the output of the operation.
     *
     * <p>This is typically done by delegating to the underlying operation. This method allows creation to be
     * intercepted.
     *
     * @param context Context to pass to the creator.
     * @param shapeId Nullable ID of the error shape to create, if known.
     * @return Returns the created output builder.
     */
    default SdkShapeBuilder<O> createOutputBuilder(Context context, String shapeId) {
        return operation().outputBuilder();
    }

    /**
     * Attempts to create a builder for a modeled error.
     *
     * <p>If this method returns null, a protocol must create an appropriate error based on protocol hints.
     *
     * @param context Context to pass to the creator.
     * @param shapeId Nullable ID of the error shape to create, if known. A string is used because sometimes the
     *                only information we have is just a name.
     * @return Returns the error deserializer if present.
     */
    Optional<SdkShapeBuilder<ModeledSdkException>> createExceptionBuilder(Context context, String shapeId);

    /**
     * Create a ClientCall builder.
     *
     * @return Returns the created builder.
     * @param <I> Input type.
     * @param <O> Output type.
     */
    static <I extends SerializableShape, O extends SerializableShape, OutputStreamT> Builder<I, O, OutputStreamT>
    builder() {
        return new Builder<>();
    }

    /**
     * Builds the default implementation of a client call.
     *
     * @param <I> Input to send.
     * @param <O> Expected output.
     */
    final class Builder<I extends SerializableShape, O extends SerializableShape, OutputStreamT> {

        I input;
        Endpoint endpoint;
        SdkOperation<I, O> operation;
        Context context;
        BiFunction<Context, String, Optional<SdkShapeBuilder<ModeledSdkException>>> errorCreator;
        ClientInterceptor interceptor = ClientInterceptor.NOOP;
        StreamPublisher inputStream;
        StreamHandler<O, OutputStreamT> streamHandler;

        private Builder() {}

        /**
         * Set the input of the call.
         *
         * @param input Input to set.
         * @return Returns the builder.
         */
        public Builder<I, O, OutputStreamT> input(I input) {
            this.input = input;
            return this;
        }

        /**
         * Set the operation schema.
         *
         * @param operation Operation to call.
         * @return Returns the builder.
         */
        public Builder<I, O, OutputStreamT> operation(SdkOperation<I, O> operation) {
            this.operation = operation;
            return this;
        }

        /**
         * Sets the context of the call.
         *
         * @param context Context to use.
         * @return Returns the builder.
         */
        public Builder<I, O, OutputStreamT> context(Context context) {
            this.context = context;
            return this;
        }

        /**
         * Sets a supplier used to create an error based on the context and extracted shape ID.
         *
         * <p>If the supplier returns null or no supplier is provided, the protocol will create an error based on
         * protocol hints (e.g., HTTP status codes).
         *
         * @param errorCreator Error supplier to create the builder for an error.
         * @return Returns the builder.
         */
        public Builder<I, O, OutputStreamT> errorCreator(
                BiFunction<Context, String, Optional<SdkShapeBuilder<ModeledSdkException>>> errorCreator
        ) {
            this.errorCreator = errorCreator;
            return this;
        }

        /**
         * Set the resolved endpoint for the call.
         *
         * @param endpoint Endpoint to set.
         * @return Returns the builder.
         */
        public Builder<I, O, OutputStreamT> endpoint(Endpoint endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        /**
         * Set the interceptor to use with this call.
         *
         * @param interceptor Interceptor to use.
         * @return Returns the builder.
         */
        public Builder<I, O, OutputStreamT> interceptor(ClientInterceptor interceptor) {
            this.interceptor = Objects.requireNonNull(interceptor);
            return this;
        }

        public Builder<I, O, OutputStreamT> inputStream(StreamPublisher inputStream) {
            this.inputStream = inputStream;
            return this;
        }

        public Builder<I, O, OutputStreamT> streamHandler(StreamHandler<O, OutputStreamT> streamHandler) {
            this.streamHandler = streamHandler;
            return this;
        }

        public ClientCall<I, O, OutputStreamT> build() {
            return new ClientCallImpl<>(this);
        }
    }
}
