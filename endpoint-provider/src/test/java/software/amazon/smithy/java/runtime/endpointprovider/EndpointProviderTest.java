/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.endpointprovider;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.java.runtime.core.context.Context;

public class EndpointProviderTest {
    @Test
    public void returnsStaticEndpoint() {
        EndpointProvider provider = EndpointProvider.staticEndpoint("https://example.com");

        MatcherAssert.assertThat(provider.resolveEndpoint(Context.create()).uri().toString(),
                                 Matchers.equalTo("https://example.com"));
    }
}
