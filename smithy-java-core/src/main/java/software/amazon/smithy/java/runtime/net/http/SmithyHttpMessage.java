/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.net.http;

import java.net.http.HttpHeaders;
import software.amazon.smithy.java.runtime.net.StoppableInputStream;

public interface SmithyHttpMessage {
    SmithyHttpVersion httpVersion();

    SmithyHttpMessage withHttpVersion(SmithyHttpVersion version);

    HttpHeaders headers();

    SmithyHttpMessage withHeaders(HttpHeaders headers);

    StoppableInputStream body();

    SmithyHttpMessage withBody(StoppableInputStream body);
}
