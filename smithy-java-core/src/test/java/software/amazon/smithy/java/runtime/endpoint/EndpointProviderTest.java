/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.endpoint;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.java.runtime.util.Context;

public class EndpointProviderTest {
    @Test
    public void returnsStaticEndpoint() {
        EndpointProvider<Context> provider = EndpointProvider.staticEndpoint("https://example.com");

        assertThat(provider.resolveEndpoint(Context.create()).uri().toString(), equalTo("https://example.com"));
    }
}
