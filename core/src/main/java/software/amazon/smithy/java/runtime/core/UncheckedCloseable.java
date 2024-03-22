/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.core;

import java.io.Closeable;

public interface UncheckedCloseable extends Closeable {
    @Override
    void close();
}
