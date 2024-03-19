/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.myservice;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import software.amazon.smithy.java.runtime.client.CallPipeline;
import software.amazon.smithy.java.runtime.client.ClientCall;
import software.amazon.smithy.java.runtime.client.ClientProtocol;
import software.amazon.smithy.java.runtime.endpoint.Endpoint;
import software.amazon.smithy.java.runtime.endpoint.EndpointParams;
import software.amazon.smithy.java.runtime.endpoint.EndpointProvider;
import software.amazon.smithy.java.runtime.myservice.model.GetPersonImage;
import software.amazon.smithy.java.runtime.myservice.model.GetPersonImageInput;
import software.amazon.smithy.java.runtime.myservice.model.GetPersonImageOutput;
import software.amazon.smithy.java.runtime.myservice.model.PersonDirectory;
import software.amazon.smithy.java.runtime.myservice.model.PutPerson;
import software.amazon.smithy.java.runtime.myservice.model.PutPersonImage;
import software.amazon.smithy.java.runtime.myservice.model.PutPersonImageInput;
import software.amazon.smithy.java.runtime.myservice.model.PutPersonImageOutput;
import software.amazon.smithy.java.runtime.myservice.model.PutPersonInput;
import software.amazon.smithy.java.runtime.myservice.model.PutPersonOutput;
import software.amazon.smithy.java.runtime.serde.streaming.StreamHandler;
import software.amazon.smithy.java.runtime.serde.streaming.StreamPublisher;
import software.amazon.smithy.java.runtime.serde.streaming.StreamingShape;
import software.amazon.smithy.java.runtime.shapes.ModeledSdkException;
import software.amazon.smithy.java.runtime.shapes.SdkOperation;
import software.amazon.smithy.java.runtime.shapes.SdkSchema;
import software.amazon.smithy.java.runtime.shapes.SerializableShape;
import software.amazon.smithy.java.runtime.shapes.TypeRegistry;
import software.amazon.smithy.java.runtime.util.Context;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyBuilder;

// Example of a potentially generated client.
public final class PersonDirectoryClient implements PersonDirectory {

    private static final System.Logger LOGGER = System.getLogger(PersonDirectoryClient.class.getName());

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
    public CompletableFuture<PutPersonOutput> putPersonAsync(PutPersonInput input, Context context) {
        return call(input, null, StreamHandler.discarding(), new PutPerson(), context)
                .thenApply(StreamingShape::shape);
    }

    @Override
    public CompletableFuture<PutPersonImageOutput> putPersonImageAsync(
            PutPersonImageInput input,
            StreamPublisher image,
            Context context
    ) {
        return call(input, image, StreamHandler.discarding(), new PutPersonImage(), context)
                .thenApply(StreamingShape::shape);
    }

    @Override
    public <ResultT> CompletableFuture<StreamingShape<GetPersonImageOutput, ResultT>> getPersonImageAsync(
            GetPersonImageInput input,
            Context context,
            StreamHandler<GetPersonImageOutput, ResultT> transformer
    ) {
        return call(input, null, transformer, new GetPersonImage(), context);
    }

    private <I extends SerializableShape, O extends SerializableShape, StreamT>
    CompletableFuture<StreamingShape<O, StreamT>> call(
            I input,
            StreamPublisher inputStream,
            StreamHandler<O, StreamT> streamHandler,
            SdkOperation<I, O> operation,
            Context context
    ) {
        // Create a copy of the type registry that adds the errors this operation can encounter.
        TypeRegistry operationRegistry = TypeRegistry.builder()
                .putAllTypes(typeRegistry, operation.typeRegistry())
                .build();

        return pipeline.send(ClientCall.<I, O, StreamT> builder()
                                     .input(input)
                                     .operation(operation)
                                     .endpoint(resolveEndpoint(operation.schema()))
                                     .context(context)
                                     .inputStream(inputStream)
                                     .streamHandler(streamHandler)
                                     .errorCreator((c, id) -> {
                                         ShapeId shapeId = ShapeId.from(id);
                                         return operationRegistry.create(shapeId, ModeledSdkException.class);
                                     })
                                     .build());
    }

    private Endpoint resolveEndpoint(SdkSchema operation) {
        Context endpointContext = Context.create();
        endpointContext.setAttribute(EndpointParams.OPERATION_ID, operation.id());
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
