/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.http.api;

import java.time.Duration;
import java.util.Objects;

/**
 * An identity-based key used to add properties to {@link HttpProperties}.
 *
 * @param <T> Value type associated with the property.
 */
public final class HttpProperty<T> {
    /**
     * The time from when an HTTP request is sent, and when the response is received. If the response is not
     * received in time, then the request is considered timed out. This setting does not apply to streaming
     * operations.
     */
    public static final HttpProperty<Duration> REQUEST_TIMEOUT = of("HTTP.RequestTimeout");

    private final String name;

    private HttpProperty(String name) {
        this.name = Objects.requireNonNull(name);
    }

    /**
     * Create a new identity-based HttpProperty.
     *
     * @param name Name of the value used to describe the property.
     * @return the created property.
     * @param <T> Value type associated with the property.
     */
    public static <T> HttpProperty<T> of(String name) {
        return new HttpProperty<>(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
