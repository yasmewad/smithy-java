/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.net;

import java.io.FilterInputStream;
import java.io.InputStream;
import java.util.Objects;

/**
 * Input stream that can be stopped.
 *
 * <p>Stopping the stream typically means to destroy underlying connection without reading more data.
 * This may be desirable when the cost of reading the rest of the data exceeds that of establishing a new connection.
 */
public final class StoppableInputStream extends FilterInputStream implements Stoppable {

    private final Stoppable stoppable;

    private StoppableInputStream(InputStream delegate, Stoppable stoppable) {
        super(delegate);
        this.stoppable = stoppable;
    }

    /**
     * Creates an empty input stream.
     *
     * @return Returns the empty, stoppabale stream.
     */
    public static StoppableInputStream ofEmpty() {
        return of(InputStream.nullInputStream());
    }

    /**
     * Create a stoppable input stream that ignores being stopped.
     *
     * @param delegate Input stream to wrap.
     * @return Returns the stoppable input stream.
     */
    public static StoppableInputStream of(InputStream delegate) {
        return of(delegate, () -> { });
    }

    /**
     * Create a stoppable input stream.
     *
     * @param delegate  Input stream to wrap.
     * @param stoppable Stoppable implementation to invoke when the stream is stopped.
     * @return Returns the stoppable input stream.
     */
    public static StoppableInputStream of(InputStream delegate, Stoppable stoppable) {
        return new StoppableInputStream(delegate, Objects.requireNonNull(stoppable));
    }

    @Override
    public void stop() {
        stoppable.stop();
    }
}
