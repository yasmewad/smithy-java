/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.client;

import software.amazon.smithy.java.runtime.net.http.SmithyHttpClient;
import software.amazon.smithy.java.runtime.serde.json.JsonCodec;

/**
 * Implements aws.protocols#restJson1.
 */
public final class RestJsonClientHandler extends HttpBindingClientHandler {
    public RestJsonClientHandler(SmithyHttpClient client) {
        super(client, JsonCodec.builder().useJsonName(true).useTimestampFormat(true).build());
    }
}
