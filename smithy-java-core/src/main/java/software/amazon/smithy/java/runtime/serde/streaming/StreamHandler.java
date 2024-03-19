/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.serde.streaming;

import java.io.InputStream;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.BiFunction;
import software.amazon.smithy.java.runtime.shapes.SerializableShape;

/**
 * A handler for streaming shape data.
 *
 * <p> The {@code StreamHandler} interface allows inspection of shape data before the actual stream body is received,
 * and is responsible for creating a {@link StreamSubscriber} used to receive the data. The {@code StreamSubscriber}
 * consumes the actual stream of bytes and, typically, converts them into a higher-level Java type.
 *
 * <p> A {@code StreamHandler} is a function that takes a {@link SerializableShape} object; and that returns a
 * {@code StreamSubscriber}. The {@code StreamSubscriber} is invoked when streaming data is received, but before the
 * bytes are received.
 *
 * <p>Note: This class is based on {@link HttpResponse.BodyHandler}.
 *
 * @param <S> the shape to read before creating a {@link StreamSubscriber}.
 * @param <T> the converted result type.
 */
public interface StreamHandler<S extends SerializableShape, T> {
    /**
     * Returns a {@link StreamSubscriber} considering the given deserialized shape. This method is invoked before
     * the actual stream is read and its implementation must return a {@link StreamSubscriber} to consume the stream
     * bytes.
     *
     * <p> The stream can be discarded using one of {@link StreamSubscriber#discarding() discarding} or
     * {@link StreamSubscriber#replacing(Object) replacing}.
     *
     * @param shape The shape that contains the stream.
     * @return a shape stream subscriber.
     */
    StreamSubscriber<T> apply(S shape);

    /**
     * Returns a {@code StreamHandler<S, String>} that returns a {@link StreamSubscriber}{@code <String>} that decodes
     * the body as UTF-8.
     *
     * @return a stream handler
     * @param <S> Shape type.
     */
    static <S extends SerializableShape> StreamHandler<S, String> ofString() {
        return shape -> StreamSubscriber.ofString();
    }

    /**
     * Returns a {@code StreamHandler<S, byte[]>} that returns a {@link StreamSubscriber}{@code <byte[]>}.
     *
     * @return a stream handler
     * @param <S> Shape type.
     */
    static <S extends SerializableShape> StreamHandler<S, byte[]> ofByteArray() {
        return shape -> StreamSubscriber.ofByteArray();
    }

    /**
     * Returns a {@code StreamHandler<S, Path>} that returns a {@link StreamSubscriber}{@code <Path>} that saves the
     * contents of a stream to a file.
     *
     * <p>Equivalent to: {@code ofFile(file, CREATE, WRITE)}
     *
     * @param file Path to the file to save.
     * @return a stream handler
     * @param <S> Shape type.
     */
    static <S extends SerializableShape> StreamHandler<S, Path> ofFile(Path file) {
        return shape -> StreamSubscriber.ofFile(file);
    }

    /**
     * Returns a {@code StreamHandler<S, Path>} that returns a {@link StreamSubscriber}{@code <Path>} that saves the
     * contents of a stream to a file.
     *
     * @param file Path to the file to save.
     * @param  openOptions the list of options to open the file with.
     * @return a stream handler
     * @param <S> Shape type.
     */
    static <S extends SerializableShape> StreamHandler<S, Path> ofFile(Path file, OpenOption... openOptions) {
        return shape -> StreamSubscriber.ofFile(file, openOptions);
    }

    /**
     * Returns a {@code StreamHandler<S, Void>} that returns a {@link StreamSubscriber}{@code <Void>} that discards
     * any streaming payload that might be returned.
     *
     * @return a stream handler
     * @param <S> Shape type.
     */
    static <S extends SerializableShape> StreamHandler<S, Void> discarding() {
        return shape -> StreamSubscriber.discarding();
    }

    /**
     * Returns a {@code StreamHandler<S, U>} that returns a {@link StreamSubscriber}{@code <U>} that discards
     * any streaming payload that might be returned and replaces it with the given value.
     *
     * @return a stream handler
     * @param <S> Shape type.
     * @param <U> Replacement value type.
     */
    static <S extends SerializableShape, U> StreamHandler<S, U> replacing(U value) {
        return shape -> StreamSubscriber.replacing(value);
    }

    /**
     * Returns a {@code StreamHandler<S, InputStream>} that returns a {@link StreamSubscriber} which streams bytes as
     * a blocking {@link InputStream}.
     *
     * @apiNote To ensure that all resources associated with the corresponding exchange are properly released, the
     * caller must ensure to either read all bytes until EOF is reached, or call {@link InputStream#close} if it is
     * unable or unwilling to do so. Calling {@code close} before exhausting the stream may cause underlying
     * connections to be closed and prevent them from being reused for subsequent operations.
     *
     * @return a stream handler
     * @param <S> Shape type.
     */
    static <S extends SerializableShape> StreamHandler<S, InputStream> ofInputStream() {
        return shape -> StreamSubscriber.ofInputStream();
    }

    /**
     * Returns a {@code StreamHandler<S, U>} that maps over the result of another handler.
     *
     * @param upstream Stream handler to initially call.
     * @param mapper   Mapper used to convert the result of the upstread handler.
     * @return a stream handler.
     * @param <S> Shape type.
     * @param <T> Upstream result type.
     * @param <U> Downstream result type.
     */
    static <S extends SerializableShape, T, U> StreamHandler<S, U> ofMapping(
            StreamHandler<S, T> upstream,
            BiFunction<S, T, U> mapper
    ) {
        return shape -> {
            var upstreamSubscriber = upstream.apply(shape);
            return new StreamSubscriber<U>() {
                @Override
                public CompletionStage<U> result() {
                    return upstreamSubscriber.result().thenApply(result -> mapper.apply(shape, result));
                }

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    upstreamSubscriber.onSubscribe(subscription);
                }

                @Override
                public void onNext(ByteBuffer item) {
                    upstreamSubscriber.onNext(item);
                }

                @Override
                public void onError(Throwable throwable) {
                    upstreamSubscriber.onError(throwable);
                }

                @Override
                public void onComplete() {
                    upstreamSubscriber.onComplete();
                }
            };
        };
    }
}
