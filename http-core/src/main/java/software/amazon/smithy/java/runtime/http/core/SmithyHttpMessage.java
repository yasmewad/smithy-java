/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.http.core;

import java.net.http.HttpHeaders;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import software.amazon.smithy.java.runtime.core.serde.DataStream;

public interface SmithyHttpMessage extends AutoCloseable {

    String startLine();

    SmithyHttpVersion httpVersion();

    SmithyHttpMessage withHttpVersion(SmithyHttpVersion version);

    HttpHeaders headers();

    SmithyHttpMessage withHeaders(HttpHeaders headers);

    /**
     * Add header fields and values to the current message.
     *
     * <p>If a header field already exists, the header field will have multiple values.
     *
     * @param fieldAndValues An array where even entries are fields, and odd entries are values for the field.
     * @return Returns the created message.
     */
    default SmithyHttpMessage withHeaders(String... fieldAndValues) {
        if (fieldAndValues.length == 0) {
            return this;
        } else if (fieldAndValues.length % 2 != 0) {
            throw new IllegalArgumentException("Uneven number of header keys and fields: " + fieldAndValues.length);
        }
        var current = new LinkedHashMap<>(headers().map());
        for (int i = 0; i < fieldAndValues.length - 1; i += 2) {
            String field = fieldAndValues[i];
            String value = fieldAndValues[i + 1];
            current.computeIfAbsent(field, f -> new ArrayList<>()).add(value);
        }
        return withHeaders(HttpHeaders.of(current, (k, v) -> true));
    }

    DataStream body();

    SmithyHttpMessage withBody(DataStream body);

    /**
     * Close underlying resources, if necessary.
     *
     * <p>If the resource is already closed, this method does nothing.
     */
    @Override
    default void close() {
        body().close();
    }
}
