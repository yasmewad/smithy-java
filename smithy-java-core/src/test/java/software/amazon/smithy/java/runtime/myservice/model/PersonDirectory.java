/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.myservice.model;

import software.amazon.smithy.java.runtime.util.Context;

// An example of a generated service interface.
public interface PersonDirectory {

    // Each operation generates two methods: one that takes context and one that doesn't.
    default PutPersonOutput putPerson(PutPersonInput input) {
        return putPerson(input, Context.create());
    }

    PutPersonOutput putPerson(PutPersonInput input, Context context);

    default PutPersonImageOutput putPersonImage(PutPersonImageInput input) {
        return putPersonImage(input, Context.create());
    }

    PutPersonImageOutput putPersonImage(PutPersonImageInput input, Context context);
}
