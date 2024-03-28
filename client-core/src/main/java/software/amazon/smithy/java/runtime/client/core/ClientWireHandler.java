/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.client.core;

public record ClientWireHandler<RequestT, ResponseT>(ClientProtocol<RequestT, ResponseT> protocol,
        ClientTransport<RequestT, ResponseT> transport) {
}
