/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.core.shapes;

import java.util.Objects;

/**
 * The top-level exception that should be used to throw application-level errors from clients and servers.
 *
 * <p>This should be used in protocol error deserialization, throwing errors based on protocol-hints, network
 * errors, and shape validation errors. It should not be used for illegal arguments, null argument validation,
 * or other kinds of logic errors sufficiently covered by the Java standard library.
 */
public class SdkException extends RuntimeException {

    /**
     * The party that is at fault for the error, if any.
     *
     * <p>This kind of enum is used rather than top-level client/server errors that services extend from because
     * services generate their own dedicated error hierarchies that all extend from a common base-class for the
     * service.
     */
    public enum Fault {
        /**
         * The client is at fault for this error (e.g., it omitted a require parameter or sent an invalid request).
         */
        CLIENT,

        /**
         * The server is at fault (e.g., it was unable to connect to a database, or other unexpected errors occurred).
         */
        SERVER,

        /**
         * The fault isn't necessarily client or server.
         */
        OTHER
    }

    private final Fault errorType;

    public SdkException(String message) {
        this(message, Fault.OTHER);
    }

    public SdkException(String message, Fault errorType) {
        super(message);
        this.errorType = Objects.requireNonNull(errorType);
    }

    public SdkException(String message, Throwable cause) {
        this(message, cause, Fault.OTHER);
    }

    public SdkException(String message, Throwable cause, Fault errorType) {
        super(message, cause);
        this.errorType = Objects.requireNonNull(errorType);
    }

    /**
     * Get the error type for this exception.
     *
     * @return Returns the error type (e.g., client, server, etc).
     */
    public Fault getFault() {
        return errorType;
    }
}
