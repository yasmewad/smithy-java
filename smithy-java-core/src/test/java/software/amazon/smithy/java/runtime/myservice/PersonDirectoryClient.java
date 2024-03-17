/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.myservice;

import java.util.Objects;
import software.amazon.smithy.java.runtime.client.ClientCall;
import software.amazon.smithy.java.runtime.client.ClientHandler;
import software.amazon.smithy.java.runtime.endpoint.Endpoint;
import software.amazon.smithy.java.runtime.endpoint.EndpointParams;
import software.amazon.smithy.java.runtime.endpoint.EndpointProvider;
import software.amazon.smithy.java.runtime.myservice.model.PersonDirectory;
import software.amazon.smithy.java.runtime.myservice.model.PutPerson;
import software.amazon.smithy.java.runtime.myservice.model.PutPersonImage;
import software.amazon.smithy.java.runtime.myservice.model.PutPersonImageInput;
import software.amazon.smithy.java.runtime.myservice.model.PutPersonImageOutput;
import software.amazon.smithy.java.runtime.myservice.model.PutPersonInput;
import software.amazon.smithy.java.runtime.myservice.model.PutPersonOutput;
import software.amazon.smithy.java.runtime.shapes.IOShape;
import software.amazon.smithy.java.runtime.shapes.ModeledSdkException;
import software.amazon.smithy.java.runtime.shapes.SdkOperation;
import software.amazon.smithy.java.runtime.shapes.SdkSchema;
import software.amazon.smithy.java.runtime.shapes.TypeRegistry;
import software.amazon.smithy.java.runtime.util.Context;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyBuilder;

// Example of a potentially generated client.
public final class PersonDirectoryClient implements PersonDirectory {

    private final EndpointProvider endpointProvider;
    private final ClientHandler protocol;
    private final TypeRegistry typeRegistry;

    private PersonDirectoryClient(Builder builder) {
        this.protocol = Objects.requireNonNull(builder.protocol, "protocol is null");
        this.endpointProvider = Objects.requireNonNull(builder.endpointProvider, "endpointProvider is null");

        // Here is where you would register errors bound to the service on the registry.
        // ...
        this.typeRegistry = TypeRegistry.builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public PutPersonOutput putPerson(PutPersonInput input, Context context) {
        return call(input, new PutPerson(), context);
    }

    @Override
    public PutPersonImageOutput putPersonImage(PutPersonImageInput input, Context context) {
        return call(input, new PutPersonImage(), context);
    }

    private <I extends IOShape, O extends IOShape> O call(
            I input,
            SdkOperation<I, O> operation,
            Context context
    ) {
        // Create a copy of the type registry that adds the errors this operation can encounter.
        TypeRegistry operationRegistry = TypeRegistry.builder()
                .putAllTypes(typeRegistry, operation.typeRegistry())
                .build();

        return protocol.send(ClientCall.<I, O> builder()
                                     .input(input)
                                     .operation(operation)
                                     .endpoint(resolveEndpoint(operation.schema()))
                                     .context(context)
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

        private ClientHandler protocol;
        private EndpointProvider endpointProvider;

        private Builder() {}

        /**
         * Set the protocol to use to call the service.
         *
         * @param protocol Protocol to use.
         * @return Returns the builder.
         */
        public Builder protocol(ClientHandler protocol) {
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
