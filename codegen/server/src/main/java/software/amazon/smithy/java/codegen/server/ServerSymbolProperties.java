/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.codegen.server;

import software.amazon.smithy.codegen.core.Property;
import software.amazon.smithy.codegen.core.Symbol;

public final class ServerSymbolProperties {

    private ServerSymbolProperties() {

    }

    public static final Property<Symbol> ASYNC_STUB_OPERATION = Property.named("async-stub-operation");

    public static final Property<Symbol> STUB_OPERATION = Property.named("stub-operation");

    public static final Property<String> OPERATION_FIELD_NAME = Property.named("operation-field-name");
}
