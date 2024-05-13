/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.codegen.server.integrations;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.URL;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.java.codegen.server.JavaServerCodegenPlugin;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ObjectNode;

public class ServiceBuilderIntegrationTest {

    private static final URL TEST_FILE = Objects.requireNonNull(
        ServiceBuilderIntegrationTest.class.getResource("server-codegen-test.smithy")
    );
    private final MockManifest manifest = new MockManifest();

    @BeforeEach
    public void setup() {
        var model = Model.assembler()
            .addImport(TEST_FILE)
            .assemble()
            .unwrap();
        var context = PluginContext.builder()
            .fileManifest(manifest)
            .settings(
                ObjectNode.builder()
                    .withMember("service", "test.smithy.java.codegen.server#TestService")
                    .withMember("namespace", "test.smithy.java.codegen.server")
                    .build()
            )
            .model(model)
            .build();
        SmithyBuildPlugin plugin = new JavaServerCodegenPlugin();
        plugin.execute(context);

        assertFalse(manifest.getFiles().isEmpty());
    }

    @Test
    void basicTest() {
        //TODO add actual tests for it.
        manifest.getFiles().forEach(p -> manifest.getFileString(p).ifPresent(System.out::println));

    }
}
