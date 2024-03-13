/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.net;

/**
 * A stoppable task.
 */
public interface Stoppable {
    /**
     * Attempts to stop the task.
     * <p>
     * This method is idempotent; stopping a stopped task should return without error.
     */
    void stop();
}
