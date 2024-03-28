/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.client.aws.restjson1;

import software.amazon.smithy.java.runtime.client.http.HttpBindingClientProtocol;
import software.amazon.smithy.java.runtime.json.JsonCodec;

/**
 * Implements aws.protocols#restJson1.
 */
public final class RestJsonClientProtocol extends HttpBindingClientProtocol {
    public RestJsonClientProtocol() {
        super("aws.protocols#restJson1", JsonCodec.builder().useJsonName(true).useTimestampFormat(true).build());
    }
}
