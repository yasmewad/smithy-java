/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.aws.mcp.cli.commands;

import java.util.Set;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import software.amazon.smithy.java.aws.servicebundle.bundler.AwsServiceBundler;
import software.amazon.smithy.java.mcp.cli.AbstractAddBundle;
import software.amazon.smithy.java.mcp.cli.CliBundle;
import software.amazon.smithy.java.mcp.cli.model.McpBundleConfig;
import software.amazon.smithy.java.mcp.cli.model.SmithyModeledBundleConfig;
import software.amazon.smithy.mcp.bundle.api.model.Bundle;
import software.amazon.smithy.mcp.bundle.api.model.BundleMetadata;
import software.amazon.smithy.mcp.bundle.api.model.SmithyMcpBundle;

@Command(name = "add-aws-bundle")
public class AddAwsServiceBundle extends AbstractAddBundle {

    @Option(names = "--overwrite",
            description = "Overwrite existing config",
            defaultValue = "false")
    protected boolean overwrite;

    @Option(names = {"-n", "--name"}, description = "Name of the AWS Service.", required = true)
    protected String awsServiceName;

    @Option(names = {"-a", "--allowed-apis"}, description = "List of APIs to expose in the MCP server")
    protected Set<String> allowedApis;

    @Option(names = {"-b", "--blocked-apis"}, description = "List of APIs to hide in the MCP server")
    protected Set<String> blockedApis;

    @Option(names = "--read-only-apis",
            description = "Include read only APIs in the MCP server")
    protected Boolean readOnlyApis;

    @Override
    protected CliBundle getNewToolConfig() {
        var bundleBuilder = AwsServiceBundler.builder()
                .serviceName(awsServiceName);
        if (allowedApis != null) {
            // User explicitly specified allowed APIs
            bundleBuilder.exposedOperations(allowedApis);
            // If readOnlyApis is also requested, include those as well
            if (Boolean.TRUE.equals(readOnlyApis)) {
                bundleBuilder.readOnlyOperations();
            }
        } else if (!Boolean.FALSE.equals(readOnlyApis)) {
            //If nothing is specified then default to only readOnlyOperations.
            bundleBuilder.readOnlyOperations();
        } else {
            throw new IllegalArgumentException("You have turned off readOnlyApis and also not specified any " +
                    "allowedApis so there are no operations to create a bundle for.");
        }

        if (blockedApis != null) {
            // Always apply blocked operations if specified
            bundleBuilder.blockedOperations(blockedApis);
        }
        var bundle = bundleBuilder.build().bundle();

        var bundleConfig = McpBundleConfig.builder()
                .smithyModeled(SmithyModeledBundleConfig.builder()
                        .name(awsServiceName)
                        .bundleLocation(getBundleFileLocation())
                        .build())
                .build();
        return new CliBundle(
                Bundle.builder()
                        .smithyBundle(SmithyMcpBundle.builder()
                                .bundle(bundle)
                                .metadata(BundleMetadata.builder().name(awsServiceName).build())
                                .build())
                        .build(),
                bundleConfig);
    }

    @Override
    protected String getToolBundleName() {
        return awsServiceName;
    }

    @Override
    protected boolean canOverwrite() {
        return overwrite;
    }
}
