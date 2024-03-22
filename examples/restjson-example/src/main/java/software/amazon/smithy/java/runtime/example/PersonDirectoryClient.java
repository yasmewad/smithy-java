/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.example;

import java.util.Objects;
import software.amazon.smithy.java.runtime.client.core.CallPipeline;
import software.amazon.smithy.java.runtime.client.core.ClientCall;
import software.amazon.smithy.java.runtime.client.core.ClientProtocol;
import software.amazon.smithy.java.runtime.core.context.Context;
import software.amazon.smithy.java.runtime.core.serde.DataStream;
import software.amazon.smithy.java.runtime.core.shapes.ModeledSdkException;
import software.amazon.smithy.java.runtime.core.shapes.SdkOperation;
import software.amazon.smithy.java.runtime.core.shapes.SdkSchema;
import software.amazon.smithy.java.runtime.core.shapes.SerializableShape;
import software.amazon.smithy.java.runtime.core.shapes.TypeRegistry;
import software.amazon.smithy.java.runtime.endpointprovider.Endpoint;
import software.amazon.smithy.java.runtime.endpointprovider.EndpointParams;
import software.amazon.smithy.java.runtime.endpointprovider.EndpointProvider;
import software.amazon.smithy.java.runtime.example.model.GetPersonImage;
import software.amazon.smithy.java.runtime.example.model.GetPersonImageInput;
import software.amazon.smithy.java.runtime.example.model.GetPersonImageOutput;
import software.amazon.smithy.java.runtime.example.model.PersonDirectory;
import software.amazon.smithy.java.runtime.example.model.PutPerson;
import software.amazon.smithy.java.runtime.example.model.PutPersonImage;
import software.amazon.smithy.java.runtime.example.model.PutPersonImageInput;
import software.amazon.smithy.java.runtime.example.model.PutPersonImageOutput;
import software.amazon.smithy.java.runtime.example.model.PutPersonInput;
import software.amazon.smithy.java.runtime.example.model.PutPersonOutput;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyBuilder;

// Example of a potentially generated client.
public final class PersonDirectoryClient implements PersonDirectory {

    private final EndpointProvider endpointProvider;
    private final CallPipeline<?, ?> pipeline;
    private final TypeRegistry typeRegistry;

    private PersonDirectoryClient(Builder builder) {
        this.endpointProvider = Objects.requireNonNull(builder.endpointProvider, "endpointProvider is null");
        this.pipeline = new CallPipeline<>(Objects.requireNonNull(builder.protocol, "protocol is null"));
        // Here is where you would register errors bound to the service on the registry.
        // ...
        this.typeRegistry = TypeRegistry.builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public PutPersonOutput putPerson(PutPersonInput input, Context context) {
        return call(input, null, null, new PutPerson(), context);
    }

    @Override
    public PutPersonImageOutput putPersonImage(PutPersonImageInput input, Context context) {
        return call(input, input.image(), null, new PutPersonImage(), context);
    }

    @Override
    public GetPersonImageOutput getPersonImage(GetPersonImageInput input, Context context) {
        return call(input, null, null, new GetPersonImage(), context);
    }

    /**
     * Performs the actual RPC call.
     *
     * @param input       Input to send.
     * @param inputStream Any kind of data stream extracted from the input, or null.
     * @param eventStream The event stream extracted from the input, or null. TODO: Implement.
     * @param operation   The operation shape.
     * @param context     Context of the call.
     * @return Returns the deserialized output.
     * @param <I> Input shape.
     * @param <O> Output shape.
     */
    private <I extends SerializableShape, O extends SerializableShape> O call(
            I input,
            DataStream inputStream,
            Object eventStream,
            SdkOperation<I, O> operation,
            Context context
    ) {
        // Create a copy of the type registry that adds the errors this operation can encounter.
        TypeRegistry operationRegistry = TypeRegistry.builder()
                .putAllTypes(typeRegistry, operation.typeRegistry())
                .build();

        return pipeline.send(ClientCall.<I, O> builder()
                                     .input(input)
                                     .operation(operation)
                                     .endpoint(resolveEndpoint(operation.schema()))
                                     .context(context)
                                     .requestInputStream(inputStream)
                                     .requestEventStream(eventStream)
                                     .errorCreator((c, id) -> {
                                         ShapeId shapeId = ShapeId.from(id);
                                         return operationRegistry.create(shapeId, ModeledSdkException.class);
                                     })
                                     .build());
    }

    private Endpoint resolveEndpoint(SdkSchema operation) {
        Context endpointContext = Context.create();
        endpointContext.setAttribute(EndpointParams.OPERATION_NAME, operation.id().getName());
        return endpointProvider.resolveEndpoint(endpointContext);
    }

    public static final class Builder implements SmithyBuilder<PersonDirectoryClient> {

        private ClientProtocol<?, ?> protocol;
        private EndpointProvider endpointProvider;

        private Builder() {}

        /**
         * Set the protocol to use to call the service.
         *
         * @param protocol Protocol to use.
         * @return Returns the builder.
         */
        public Builder protocol(ClientProtocol<?, ?> protocol) {
            this.protocol = protocol;
            return this;
        }

        /**
         * Set the provider used to resolve endpoints.
         *
         * @param endpointProvider Endpoint provider to use to resolve endpoints.
         * @return Returns the endpoint provider.
         */
        public Builder endpointProvider(EndpointProvider endpointProvider) {
            this.endpointProvider = endpointProvider;
            return this;
        }

        @Override
        public PersonDirectoryClient build() {
            return new PersonDirectoryClient(this);
        }
    }
}
