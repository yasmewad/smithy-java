/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.core.serde.any;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.java.runtime.core.serde.ToStringSerializer;

public class AnyTest {
    @Test
    public void getAsNumber() {
        assertThat(Any.of((byte) 1).asNumber(), equalTo(Byte.valueOf((byte) 1)));
        assertThat(Any.of((short) 1).asNumber(), equalTo(Short.valueOf((short) 1)));
        assertThat(Any.of(1).asNumber(), equalTo(Integer.valueOf(1)));
        assertThat(Any.of(1L).asNumber(), equalTo(Long.valueOf(1L)));
        assertThat(Any.of(1f).asNumber(), equalTo(Float.valueOf(1f)));
        assertThat(Any.of(1.0).asNumber(), equalTo(Double.valueOf(1.0)));
        assertThat(Any.of(new BigDecimal(1)).asNumber(), equalTo(new BigDecimal(1)));
        assertThat(Any.of(BigInteger.valueOf(1)).asNumber(), equalTo(BigInteger.valueOf(1)));
    }

    @ParameterizedTest
    @MethodSource("defaultSerializationProvider")
    public void defaultSerialization(Any value) {
        var expected = ToStringSerializer.serialize(value);
        var toString = new ToStringSerializer();
        AnySerializer.serialize(value, toString);

        assertThat(expected, equalTo(toString.toString()));
    }

    public static List<Arguments> defaultSerializationProvider() {
        return List.of(
            Arguments.of(Any.of((byte) 1)),
            Arguments.of(Any.of((short) 1)),
            Arguments.of(Any.of(1)),
            Arguments.of(Any.of(1L)),
            Arguments.of(Any.of(1f)),
            Arguments.of(Any.of(1.0)),
            Arguments.of(Any.of(BigInteger.valueOf(1))),
            Arguments.of(Any.of(new BigDecimal(1))),
            Arguments.of(Any.of("a")),
            Arguments.of(Any.of("a".getBytes(StandardCharsets.UTF_8))),
            Arguments.of(Any.of(true)),
            Arguments.of(Any.of(Instant.EPOCH)),
            Arguments.of(Any.of(List.of(Any.of(1), Any.of("a"))))
        );
    }
}
