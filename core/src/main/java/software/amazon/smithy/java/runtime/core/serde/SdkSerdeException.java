/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.core.serde;

import software.amazon.smithy.java.runtime.core.schema.SdkException;

public class SdkSerdeException extends SdkException {
    public SdkSerdeException(String message) {
        super(message, Fault.OTHER);
    }

    public SdkSerdeException(Throwable cause) {
        this(cause.getMessage(), cause);
    }

    public SdkSerdeException(String message, Throwable cause) {
        super(message, cause, Fault.OTHER);
    }
}
