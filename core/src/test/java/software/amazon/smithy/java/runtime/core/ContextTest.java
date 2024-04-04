/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ContextTest {

    private static final Context.Key<String> FOO = Context.key("Foo");
    private static final Context.Key<Integer> BAR = Context.key("Foo");

    @Test
    public void getTypedValue() {
        var context = Context.create();
        context.put(FOO, "Hi");
        context.put(BAR, 1);

        assertThat(context.get(FOO), equalTo("Hi"));
        assertThat(context.expect(FOO), equalTo("Hi"));

        assertThat(context.get(BAR), is(1));
        assertThat(context.expect(BAR), is(1));
    }

    @Test
    public void returnsNullWhenNotFound() {
        var context = Context.create();

        assertThat(context.get(FOO), nullValue());
    }

    @Test
    public void throwsWhenExpectedAndNotFound() {
        var context = Context.create();

        Assertions.assertThrows(NullPointerException.class, () -> context.expect(FOO));
    }

    @Test
    public void computesAndSets() {
        var context = Context.create();

        assertThat(context.computeIfAbsent(FOO, key -> "hi"), equalTo("hi"));
        assertThat(context.computeIfAbsent(FOO, key -> "bye"), equalTo("hi"));
    }

    @Test
    public void returnsKeys() {
        var context = Context.create();
        context.put(FOO, "test");
        context.put(BAR, 1);

        Set<Context.Key<?>> keys = new HashSet<>();
        context.keys().forEachRemaining(keys::add);

        assertThat(keys, containsInAnyOrder(FOO, BAR));
    }

    @Test
    public void createsValueWrapper() {
        var value = Context.value(FOO, "hi");

        assertThat(value.key(), is(FOO));
        assertThat(value.value(), equalTo("hi"));
    }

    @Test
    public void valueGetsIfMatch() {
        var value = Context.value(FOO, "hi");

        Object[] container = new Object[1];
        value.getIf(FOO, v -> container[0] = v);
        value.getIf(BAR, i -> container[0] = i);

        assertThat(container[0], equalTo("hi"));
    }

    @Test
    public void valueMapsIfMatch() {
        var value = Context.value(FOO, "hi");

        var updated = value.mapIf(FOO, v -> v.toUpperCase(Locale.ENGLISH));
        assertThat(updated.key(), is(FOO));
        assertThat(updated.value(), equalTo("HI"));

        var same = value.mapIf(BAR, i -> i + 1);
        assertThat(same.key(), is(FOO));
        assertThat(same.value(), equalTo("hi"));
    }
}
