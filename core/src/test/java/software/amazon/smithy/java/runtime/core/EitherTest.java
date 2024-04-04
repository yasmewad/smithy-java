/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class EitherTest {
    @Test
    public void capturesLeft() {
        var either = Either.<String, String>left("foo");

        assertThat(either.isLeft(), equalTo(true));
        assertThat(either.left(), equalTo("foo"));
        assertThat(either, equalTo(Either.left("foo")));
        assertThat(either, equalTo(either));
        assertThat(either, not(equalTo(null)));
        assertThat(either, not(equalTo("X")));
        assertThat(either, not(equalTo(Either.left("bar"))));
        assertThat(either, not(equalTo(Either.right("foo"))));
        assertThat(either, not(equalTo(either.swap())));
    }

    @Test
    public void capturesRight() {
        var either = Either.<String, String>right("foo");

        assertThat(either.isRight(), equalTo(true));
        assertThat(either.right(), equalTo("foo"));
        assertThat(either, equalTo(either));
        assertThat(either, equalTo(Either.right("foo")));
        assertThat(either, not(equalTo(Either.right("bar"))));
        assertThat(either, not(equalTo(Either.left("foo"))));
        assertThat(either, not(equalTo(either.swap())));
    }

    @Test
    public void filterAndMapRight() {
        var either = Either.<String, String>right("foo");

        assertThat(either.map(s -> s.toUpperCase(Locale.ENGLISH)), equalTo(Either.right("FOO")));
        assertThat(either.filter(s -> true), equalTo(Optional.of(Either.right("foo"))));
    }

    @Test
    public void filterAndMapIgnoreLeft() {
        var either = Either.<String, String>left("foo");

        assertThat(either.map(s -> s.toUpperCase(Locale.ENGLISH)), equalTo(Either.left("foo")));
        assertThat(either.filter(s -> true), equalTo(Optional.empty()));
    }
}
