/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.client.auth.api;

import software.amazon.smithy.java.runtime.context.ReadableContext;

/**
 * An authentication scheme option, composed of the scheme ID and properties for use when resolving the identity and
 * signing the request.
 *
 * <p>This is used in the output from the auth scheme resolver. The resolver returns a list of these, in the order the
 * auth scheme resolver wishes to use them.
 *
 * @see AuthScheme
 */
public interface AuthSchemeOption {
    /**
     * Retrieve the scheme ID, a unique identifier for the authentication scheme
     * (aws.auth#sigv4, smithy.api#httpBearerAuth).
     *
     * @return the scheme ID.
     */
    String schemeId();

    /**
     * Get the resolved identity properties.
     *
     * @return the identity properties.
     */
    ReadableContext identityProperties();

    /**
     * Get the resolved signer properties.
     *
     * @return the signer properties.
     */
    ReadableContext signerProperties();

    /**
     * Create a new AuthScheme option.
     *
     * @param schemeId           Scheme ID of the option.
     * @param identityProperties Identity properties.
     * @param signerProperties   Signer properties.
     * @return the created auth scheme option.
     */
    static AuthSchemeOption create(
            String schemeId, ReadableContext identityProperties, ReadableContext signerProperties) {
        return new AuthSchemeOptionRecord(schemeId, identityProperties, signerProperties);
    }
}
