/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.serde.httpbinding;

import java.util.Objects;
import software.amazon.smithy.java.runtime.shapes.SdkSchema;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;

final class BindingMatcher {

    enum Binding { HEADER, QUERY, PAYLOAD, BODY, LABEL }

    private final boolean isRequest;
    private HttpHeaderTrait header;
    private HttpLabelTrait label;
    private HttpQueryTrait query;
    private HttpPayloadTrait payload;

    BindingMatcher(boolean isRequest) {
        this.isRequest = isRequest;
    }

    Binding match(SdkSchema member) {
        if (isRequest) {
            label = member.getTrait(HttpLabelTrait.class).orElse(null);
            if (label != null) {
                return Binding.LABEL;
            }
            query = member.getTrait(HttpQueryTrait.class).orElse(null);
            if (query != null) {
                return Binding.QUERY;
            }
        }

        header = member.getTrait(HttpHeaderTrait.class).orElse(null);
        if (header != null) {
            return Binding.HEADER;
        }

        payload = member.getTrait(HttpPayloadTrait.class).orElse(null);
        if (payload != null) {
            return Binding.PAYLOAD;
        }

        return Binding.BODY;
    }

    HttpHeaderTrait header() {
        return Objects.requireNonNull(header, "Not a header binding");
    }

    HttpLabelTrait label() {
        return Objects.requireNonNull(label, "Not a label binding");
    }

    HttpQueryTrait query() {
        return Objects.requireNonNull(query, "Not a query binding");
    }

    HttpPayloadTrait payload() {
        return Objects.requireNonNull(payload, "Not a payload binding");
    }
}
