/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.mcp.cli;

import software.amazon.smithy.java.mcp.cli.model.TelemetryData;

public interface TelemetryPublisher {

    void publish(TelemetryData data);

}
