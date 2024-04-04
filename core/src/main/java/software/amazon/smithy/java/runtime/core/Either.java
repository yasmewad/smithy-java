/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.core;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A value that is either left or right.
 *
 * <p>Either is right-biased; map, filter, and other operations operate on the right value and are ignored if it's
 * the left value.
 *
 * @param <L> The left value type.
 * @param <R> The right value type.
 */
public final class Either<L, R> {

    private final L left;
    private final R right;

    private Either(L left, R right) {
        this.left = left;
        this.right = right;
    }

    /**
     * Create a left Either.
     *
     * @param left Value to set.
     * @return the created Either.
     * @param <L> Left value type.
     * @param <R> Right value type.
     * @throws NullPointerException if left is null.
     */
    public static <L, R> Either<L, R> left(L left) {
        return new Either<>(Objects.requireNonNull(left, "left cannot be null"), null);
    }

    /**
     * Create a right Either.
     *
     * @param right Value to set.
     * @return the created Either.
     * @param <L> Left value type.
     * @param <R> Right value type.
     * @throws NullPointerException if right is null.
     */
    public static <L, R> Either<L, R> right(R right) {
        return new Either<>(null, Objects.requireNonNull(right, "right cannot be null"));
    }

    /**
     * Get the left value if set, or null if not.
     *
     * @return the left value or null.
     */
    public L left() {
        return left;
    }

    /**
     * Get the right value if set, or null if not.
     *
     * @return the right value.
     */
    public R right() {
        return right;
    }

    /**
     * Check if the left value is set.
     *
     * @return true if left.
     */
    public boolean isLeft() {
        return left != null;
    }

    /**
     * Check if the right value is set.
     *
     * @return true if right.
     */
    public boolean isRight() {
        return right != null;
    }

    /**
     * Swap the either so that left becomes right and right becomes left.
     *
     * @return the swapped Either.
     */
    public Either<R, L> swap() {
        if (isLeft()) {
            return right(left());
        } else {
            return left(right());
        }
    }

    /**
     * Tests if the right value matches the predicate, and if so returns an optional of this Either.
     *
     * @param predicate Predicate to test the right value.
     * @return the optional Either.
     */
    public Optional<Either<L, R>> filter(Predicate<? super R> predicate) {
        if (isLeft() || !predicate.test(right())) {
            return Optional.empty();
        } else {
            return Optional.of(this);
        }
    }

    /**
     * Map over the right value of the Either and return a new Either, or do nothing if left.
     *
     * @param mapper Mapper to apply to the right value.
     * @return the either.
     * @param <U> Updated value type.
     */
    @SuppressWarnings("unchecked")
    public <U> Either<L, U> map(Function<? super R, ? extends U> mapper) {
        if (isLeft()) {
            return (Either<L, U>) this;
        } else {
            return Either.right(mapper.apply(right()));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Either<?, ?> either = (Either<?, ?>) o;
        return Objects.equals(left, either.left) && Objects.equals(right, either.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isLeft(), isRight());
    }

    @Override
    public String toString() {
        return "Either{left=" + left + ", right=" + right + '}';
    }
}
