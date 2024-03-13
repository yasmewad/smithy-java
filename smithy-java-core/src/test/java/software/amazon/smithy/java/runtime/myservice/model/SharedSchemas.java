/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.myservice.model;

import software.amazon.smithy.java.runtime.shapes.SdkSchema;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.traits.StreamingTrait;

/**
 * Defines package-private shared shapes across the model package that are not part of another code-generated type.
 */
final class SharedSchemas {

    static final SdkSchema STRING = SdkSchema.builder().type(ShapeType.STRING).id("smithy.api#String").build();

    static final SdkSchema INTEGER = SdkSchema.builder().type(ShapeType.INTEGER).id("smithy.api#Integer").build();

    static final SdkSchema BIRTHDAY = SdkSchema.builder()
            .type(ShapeType.TIMESTAMP)
            .id("smithy.example#Birthday")
            .traits(new SensitiveTrait())
            .build();

    static final SdkSchema STREAM = SdkSchema.builder()
            .type(ShapeType.BLOB)
            .id("smithy.example#Stream")
            .traits(new StreamingTrait())
            .build();

    static final SdkSchema BLOB = SdkSchema.builder().type(ShapeType.BLOB).id("smithy.api#Blob").build();

    private SharedSchemas() {}
}
