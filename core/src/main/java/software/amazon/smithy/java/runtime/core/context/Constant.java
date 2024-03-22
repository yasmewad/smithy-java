/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.core.context;

import java.util.Objects;

/**
 * A {@code Constant} provides an identity-based, immutable token. This token captures additional information such as
 * the type of data it represents and a brief description of that data.
 */
public final class Constant<T> {

    private final Class<T> type;
    private final String purpose;

    /**
     * @param type The attribute's class
     * @param purpose Description of the Constant.
     */
    public Constant(Class<T> type, String purpose) {
        this.type = Objects.requireNonNull(type);
        this.purpose = Objects.requireNonNull(purpose);
    }

    public Class<T> getType() {
        return type;
    }

    @Override
    public String toString() {
        return purpose;
    }

    @Override
    public boolean equals(Object o) {
        return o == this;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}
