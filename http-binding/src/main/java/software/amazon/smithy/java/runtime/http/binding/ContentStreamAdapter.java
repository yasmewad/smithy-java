/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.http.binding;

import java.io.InputStream;
import software.amazon.smithy.java.runtime.core.serde.DataStream;
import software.amazon.smithy.java.runtime.http.api.ContentStream;

public final class ContentStreamAdapter implements ContentStream {

    private final DataStream delegate;

    public ContentStreamAdapter(DataStream delegate) {
        this.delegate = delegate;
    }

    @Override
    public InputStream inputStream() {
        return delegate.inputStream();
    }

    @Override
    public boolean rewind() {
        return delegate.rewind();
    }
}
