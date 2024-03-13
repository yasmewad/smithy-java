/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.util;

import java.io.IOException;

public interface ThrowableRunnable {
    void run() throws IOException;
}
