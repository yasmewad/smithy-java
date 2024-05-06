/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.codegen.server.generators;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.directed.GenerateOperationDirective;
import software.amazon.smithy.java.codegen.CodeGenerationContext;
import software.amazon.smithy.java.codegen.CodegenUtils;
import software.amazon.smithy.java.codegen.JavaCodegenSettings;
import software.amazon.smithy.java.codegen.sections.ClassSection;
import software.amazon.smithy.java.codegen.server.ServerSymbolProperties;
import software.amazon.smithy.java.server.core.RequestContext;

public class OperationInterfaceGenerator implements
    Consumer<GenerateOperationDirective<CodeGenerationContext, JavaCodegenSettings>> {

    @Override
    public void accept(GenerateOperationDirective<CodeGenerationContext, JavaCodegenSettings> directive) {
        var shape = directive.shape();
        var input = directive.symbolProvider().toSymbol(directive.model().expectShape(shape.getInputShape()));
        var output = directive.symbolProvider().toSymbol(directive.model().expectShape(shape.getOutputShape()));
        var operationMethodName = directive.symbol().getProperty(ServerSymbolProperties.OPERATION_FIELD_NAME);
        Symbol stubSymbol = directive.symbol().expectProperty(ServerSymbolProperties.STUB_OPERATION);
        Symbol asyncStubSymbol = directive.symbol().expectProperty(ServerSymbolProperties.ASYNC_STUB_OPERATION);
        for (Symbol symbol : List.of(stubSymbol, asyncStubSymbol)) {
            directive.context()
                .writerDelegator()
                .useFileWriter(symbol.getDeclarationFile(), symbol.getNamespace(), writer -> {
                    writer.pushState(new ClassSection(shape));
                    writer.write("@$T", FunctionalInterface.class);
                    writer.putContext("interface", symbol);
                    writer.putContext("requestContext", RequestContext.class);
                    writer.putContext(
                        "output",
                        CodegenUtils.fromClass(CompletableFuture.class)
                            .toBuilder()
                            .addReference(output)
                            .build()
                    );
                    writer.putContext("methodName", operationMethodName);
                    writer.putContext("input", input);
                    writer.write("""
                        public interface ${interface:T} {
                            ${output:T} ${methodName:L}(${input:T} input, ${requestContext:T} context);
                        }
                        """);
                    writer.popState();
                });
        }
    }
}
