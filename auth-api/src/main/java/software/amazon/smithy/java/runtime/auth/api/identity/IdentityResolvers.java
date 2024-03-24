/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.auth.api.identity;

/**
 * An interface to allow retrieving an IdentityProvider based on the identity type.
 */
public interface IdentityResolvers {
    /**
     * Retrieve an identity provider for the provided identity type.
     */
    <T extends Identity> IdentityResolver<T> identityResolver(Class<T> identityType);
}
