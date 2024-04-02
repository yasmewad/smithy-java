/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.core.serde.any;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.java.runtime.core.serde.any.Any;

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
}
