/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.auth.api.identity;

import software.amazon.smithy.java.runtime.context.Context;
import software.amazon.smithy.java.runtime.context.ReadableContext;

/**
 * Interface for loading {@link Identity} that is used for authentication.
 */
public interface IdentityResolver<IdentityT extends Identity> {
    /**
     * Retrieve the class of identity this identity provider produces.
     *
     * <p>Used to resolve an identity by class.
     */
    Class<IdentityT> identityType();

    /**
     * Resolve the identity from this identity provider.
     *
     * @param request The request context used to resolve an Identity.
     * @return the resolved identity.
     * @param <ResolvedIdentityT> Resolved identity type.
     */
    <ResolvedIdentityT extends IdentityT> ResolvedIdentityT resolveIdentity(ReadableContext request);

    /**
     * Resolve the identity from this identity provider.
     *
     * @return the resolved identity.
     * @param <ResolvedIdentityT> Resolved identity type.
     */
    default <ResolvedIdentityT extends IdentityT> ResolvedIdentityT resolveIdentity() {
        return resolveIdentity(Context.create());
    }
}
