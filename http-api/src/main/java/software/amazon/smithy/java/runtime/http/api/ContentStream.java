/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.http.api;

import java.io.InputStream;

/**
 * A rewindable stream of data for HTTP requests.
 */
public interface ContentStream {
    /**
     * Get an empty ContentStream.
     */
    ContentStream EMPTY = new ContentStream() {
        @Override
        public InputStream inputStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public boolean rewind() {
            return false;
        }
    };

    /**
     * Get the InputStream.
     *
     * @return the underlying InputStream.
     */
    InputStream inputStream();

    /**
     * Attempt to rewind the input stream to the beginning of the stream.
     *
     * <p>This method must not throw if the stream is not rewindable.
     *
     * @return Returns true if the stream could be rewound.
     */
    boolean rewind();
}
