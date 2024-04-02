/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.core.serde.any;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.java.runtime.core.schema.SdkSchema;
import software.amazon.smithy.java.runtime.core.schema.SdkShapeBuilder;
import software.amazon.smithy.java.runtime.core.schema.SerializableShape;
import software.amazon.smithy.java.runtime.core.serde.SdkSerdeException;
import software.amazon.smithy.java.runtime.core.serde.ShapeSerializer;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * The runtime type used for Smithy document types.
 */
public interface Any extends SerializableShape {

    /**
     * The default Schema attached to Any types that have no known Schema.
     */
    SdkSchema SCHEMA = SdkSchema.builder().id(ShapeId.from("smithy.api#Document")).type(ShapeType.DOCUMENT).build();

    /**
     * Create a boolean Any.
     *
     * @param value Value to wrap.
     * @return the Any type.
     */
    static Any of(boolean value) {
        return of(value, SCHEMA);
    }

    /**
     * Create a boolean Any with a Schema.
     *
     * @param value Value to wrap.
     * @param schema Schema of the value. The Schema must have the same type or use the catch-all {@link #SCHEMA}.
     * @return the Any type.
     * @throws SdkSerdeException if the schema type is incompatible with the Any type.
     */
    static Any of(boolean value, SdkSchema schema) {
        return new BooleanAny(value, schema);
    }

    /**
     * Create a blob Any.
     *
     * @param value Value to wrap.
     * @return the Any type.
     */
    static Any of(byte[] value) {
        return of(value, SCHEMA);
    }

    /**
     * Create a blob Any with a Schema.
     *
     * @param value Value to wrap.
     * @param schema Schema of the value. The Schema must have the same type or use the catch-all {@link #SCHEMA}.
     * @return the Any type.
     * @throws SdkSerdeException if the schema type is incompatible with the Any type.
     */
    static Any of(byte[] value, SdkSchema schema) {
        return new BlobAny(value, schema);
    }

    /**
     * Create a timestamp Any.
     *
     * @param value Value to wrap.
     * @return the Any type.
     */
    static Any of(Instant value) {
        return of(value, SCHEMA);
    }

    /**
     * Create a timestamp Any with a Schema.
     *
     * @param value Value to wrap.
     * @param schema Schema of the value. The Schema must have the same type or use the catch-all {@link #SCHEMA}.
     * @return the Any type.
     * @throws SdkSerdeException if the schema type is incompatible with the Any type.
     */
    static Any of(Instant value, SdkSchema schema) {
        return new TimestampAny(value, schema);
    }

    /**
     * Create a string Any.
     *
     * @param value Value to wrap.
     * @return the Any type.
     */
    static Any of(String value) {
        return of(value, SCHEMA);
    }

    /**
     * Create a string Any with a Schema.
     *
     * @param value Value to wrap.
     * @param schema Schema of the value. The Schema must have the same type or use the catch-all {@link #SCHEMA}.
     * @return the Any type.
     * @throws SdkSerdeException if the schema type is incompatible with the Any type.
     */
    static Any of(String value, SdkSchema schema) {
        return new StringAny(value, schema);
    }

    /**
     * Create a byte Any.
     *
     * @param value Value to wrap.
     * @return the Any type.
     */
    static Any of(byte value) {
        return of(value, SCHEMA);
    }

    /**
     * Create a byte Any with a Schema.
     *
     * @param value Value to wrap.
     * @param schema Schema of the value. The Schema must have the same type or use the catch-all {@link #SCHEMA}.
     * @return the Any type.
     * @throws SdkSerdeException if the schema type is incompatible with the Any type.
     */
    static Any of(byte value, SdkSchema schema) {
        return new ByteAny(value, schema);
    }

    /**
     * Create a short Any.
     *
     * @param value Value to wrap.
     * @return the Any type.
     */
    static Any of(short value) {
        return of(value, SCHEMA);
    }

    /**
     * Create a short Any with a Schema.
     *
     * @param value Value to wrap.
     * @param schema Schema of the value. The Schema must have the same type or use the catch-all {@link #SCHEMA}.
     * @return the Any type.
     * @throws SdkSerdeException if the schema type is incompatible with the Any type.
     */
    static Any of(short value, SdkSchema schema) {
        return new ShortAny(value, schema);
    }

    /**
     * Create an integer Any.
     *
     * @param value Value to wrap.
     * @return the Any type.
     */
    static Any of(int value) {
        return of(value, SCHEMA);
    }

    /**
     * Create an integer Any with a Schema.
     *
     * @param value Value to wrap.
     * @param schema Schema of the value. The Schema must have the same type or use the catch-all {@link #SCHEMA}.
     * @return the Any type.
     * @throws SdkSerdeException if the schema type is incompatible with the Any type.
     */
    static Any of(int value, SdkSchema schema) {
        return new IntegerAny(value, schema);
    }

    /**
     * Create a long Any.
     *
     * @param value Value to wrap.
     * @return the Any type.
     */
    static Any of(long value) {
        return of(value, SCHEMA);
    }

    /**
     * Create a long Any with a Schema.
     *
     * @param value Value to wrap.
     * @param schema Schema of the value. The Schema must have the same type or use the catch-all {@link #SCHEMA}.
     * @return the Any type.
     * @throws SdkSerdeException if the schema type is incompatible with the Any type.
     */
    static Any of(long value, SdkSchema schema) {
        return new LongAny(value, schema);
    }

    /**
     * Create a float Any.
     *
     * @param value Value to wrap.
     * @return the Any type.
     */
    static Any of(float value) {
        return of(value, SCHEMA);
    }

    /**
     * Create a float Any with a Schema.
     *
     * @param value Value to wrap.
     * @param schema Schema of the value. The Schema must have the same type or use the catch-all {@link #SCHEMA}.
     * @return the Any type.
     * @throws SdkSerdeException if the schema type is incompatible with the Any type.
     */
    static Any of(float value, SdkSchema schema) {
        return new FloatAny(value, schema);
    }

    /**
     * Create a double Any.
     *
     * @param value Value to wrap.
     * @return the Any type.
     */
    static Any of(double value) {
        return of(value, SCHEMA);
    }

    /**
     * Create a double Any with a Schema.
     *
     * @param value Value to wrap.
     * @param schema Schema of the value. The Schema must have the same type or use the catch-all {@link #SCHEMA}.
     * @return the Any type.
     * @throws SdkSerdeException if the schema type is incompatible with the Any type.
     */
    static Any of(double value, SdkSchema schema) {
        return new DoubleAny(value, schema);
    }

    /**
     * Create a bigInteger Any.
     *
     * @param value Value to wrap.
     * @return the Any type.
     */
    static Any of(BigInteger value) {
        return of(value, SCHEMA);
    }

    /**
     * Create a BigInteger Any with a Schema.
     *
     * @param value Value to wrap.
     * @param schema Schema of the value. The Schema must have the same type or use the catch-all {@link #SCHEMA}.
     * @return the Any type.
     * @throws SdkSerdeException if the schema type is incompatible with the Any type.
     */
    static Any of(BigInteger value, SdkSchema schema) {
        return new BigIntegerAny(value, schema);
    }

    /**
     * Create a bigDecimal Any.
     *
     * @param value Value to wrap.
     * @return the Any type.
     */
    static Any of(BigDecimal value) {
        return of(value, SCHEMA);
    }

    /**
     * Create a BigDecimal Any with a Schema.
     *
     * @param value Value to wrap.
     * @param schema Schema of the value. The Schema must have the same type or use the catch-all {@link #SCHEMA}.
     * @return the Any type.
     * @throws SdkSerdeException if the schema or a member schema is invalid.
     */
    static Any of(BigDecimal value, SdkSchema schema) {
        return new BigDecimalAny(value, schema);
    }

    /**
     * Create a list Any that has no schema.
     *
     * <p>Every member of the list must have the same schema (if any). If the member schema is a member shape, the
     * member must have a name of "member". If the member is not a member schema, it must be a document schema.
     *
     * @param value Value to wrap.
     * @return the Any type.
     * @throws SdkSerdeException if the schema or a member schema is invalid.
     */
    static Any of(List<Any> value) {
        return of(value, SCHEMA);
    }

    /**
     * Create a List Any with a Schema.
     *
     * <p>The Schema must have a list or document type. Every member of the list must have the same schema (if any).
     * If the member schema is a member shape, the member must have a name of "member". If the member is not a member
     * schema, it must be a document schema.
     *
     * @param value Value to wrap.
     * @param schema Schema of the value. The Schema must have the same type or use the catch-all {@link #SCHEMA}.
     * @return the Any type.
     * @throws SdkSerdeException if the schema, a key schema, or a value schema is invalid.
     */
    static Any of(List<Any> value, SdkSchema schema) {
        return new ListAny(value, schema);
    }

    /**
     * Create a Map Any.
     *
     * <p>Every key and value member of the map must have the same schema. If the key member schema is a member shape,
     * the member must have a name of "key". If the member is not a member schema, it must be a document schema. If the
     * value member schema is a member shape, the member must have a name of "value". If the member is not a member
     * schema, it must be a document schema.
     *
     * @param value Value to wrap.
     * @return the Any type.
     * @throws SdkSerdeException if the schema, key schema, or value schema is invalid.
     */
    static Any of(Map<Any, Any> value) {
        return of(value, SCHEMA);
    }

    /**
     * Create a Map Any with a Schema.
     *
     * <p>The Schema must have a map or document type. Every key and value member of the map must have the same schema.
     * If the key member schema is a member shape, the member must have a name of "key". If the member is not a member
     * schema, it must be a document schema. If the value member schema is a member shape, the member must have a name
     * of "value". If the member is not a member schema, it must be a document schema.
     *
     * @param value Value to wrap.
     * @param schema Schema of the value. The Schema must have the same type or use the catch-all {@link #SCHEMA}.
     * @return the Any type.
     * @throws SdkSerdeException if the schema type is incompatible with the Any type.
     */
    static Any of(Map<Any, Any> value, SdkSchema schema) {
        return new MapAny(value, schema);
    }

    /**
     * Create an Any from a shape.
     *
     * @param shape Shape to turn into an Any.
     * @return the Any type.
     */
    static Any of(SerializableShape shape) {
        AnyParser parser = new AnyParser();
        shape.serialize(parser);
        return parser.getResult();
    }

    /**
     * Get the schema of the Any.
     *
     * <p>If the value is untyped, {@link #SCHEMA} is returned.
     *
     * @return the schema.
     */
    SdkSchema schema();

    /**
     * Get the Any shape type for the Smithy data model.
     *
     * <p>This could differ from the result of {@code schema().type()} if the schema of the Any is a document schema.
     *
     * @return the Any type.
     */
    ShapeType type();

    /**
     * Get the boolean value of the Any if it is a boolean.
     *
     * @return the boolean value.
     * @throws IllegalStateException if the Any is not a boolean.
     */
    default boolean asBoolean() {
        throw new IllegalStateException("Expected a boolean value, but found " + type());
    }

    /**
     * Get the byte value of the Any if it is a byte.
     *
     * @return the byte value.
     * @throws IllegalStateException if the Any is not a byte.
     */
    default byte asByte() {
        throw new IllegalStateException("Expected a byte value, but found " + type());
    }

    /**
     * Get the short value of the Any if it is a short.
     *
     * @return the short value.
     * @throws IllegalStateException if the Any is not a short.
     */
    default short asShort() {
        throw new IllegalStateException("Expected a short value, but found " + type());
    }

    /**
     * Get the integer value of the Any if it is an integer.
     *
     * @return the integer value.
     * @throws IllegalStateException if the Any is not an integer.
     */
    default int asInteger() {
        throw new IllegalStateException("Expected an integer value, but found " + type());
    }

    /**
     * Get the long value of the Any if it is a long.
     *
     * @return the long value.
     * @throws IllegalStateException if the Any is not a long.
     */
    default long asLong() {
        throw new IllegalStateException("Expected a long value, but found " + type());
    }

    /**
     * Get the float value of the Any if it is a float.
     *
     * @return the float value.
     * @throws IllegalStateException if the Any is not a float.
     */
    default float asFloat() {
        throw new IllegalStateException("Expected a float value, but found " + type());
    }

    /**
     * Get the double value of the Any if it is a double.
     *
     * @return the double value.
     * @throws IllegalStateException if the Any is not a double.
     */
    default double asDouble() {
        throw new IllegalStateException("Expected a double value, but found " + type());
    }

    /**
     * Get the BigInteger value of the Any if it is a bigInteger.
     *
     * @return the BigInteger value.
     * @throws IllegalStateException if the Any is not a bigInteger.
     */
    default BigInteger asBigInteger() {
        throw new IllegalStateException("Expected a bigInteger value, but found " + type());
    }

    /**
     * Get the BigDecimal value of the Any if it is a bigDecimal.
     *
     * @return the BigDecimal value.
     * @throws IllegalStateException if the Any is not a bigDecimal.
     */
    default BigDecimal asBigDecimal() {
        throw new IllegalStateException("Expected a bigDecimal value, but found " + type());
    }

    /**
     * Get the value of the builder as a Number.
     *
     * @return the Number value.
     * @throws IllegalStateException if the Any is not a numeric type.
     */
    default Number asNumber() {
        return switch (type()) {
            case BYTE -> asByte();
            case SHORT -> asShort();
            case INTEGER -> asInteger();
            case LONG -> asLong();
            case FLOAT -> asFloat();
            case DOUBLE -> asDouble();
            case BIG_INTEGER -> asBigInteger();
            case BIG_DECIMAL -> asBigDecimal();
            default -> throw new IllegalStateException("Expected a number value, but found " + type());
        };
    }

    /**
     * Get the string value of the Any if it is a string.
     *
     * @return the string value.
     * @throws IllegalStateException if the Any is not a string.
     */
    default String asString() {
        throw new IllegalStateException("Expected a string value, but found " + type());
    }

    /**
     * Get the Any as a byte array if the Any is a blob.
     *
     * @return the bytes of the blob.
     * @throws IllegalStateException if the Any is not a blob.
     */
    default byte[] asBlob() {
        throw new IllegalStateException("Expected a blob value, but found " + type());
    }

    /**
     * Get the Any as an Instant if the Any is a timestamp.
     *
     * @return the Instant value of the timestamp.
     * @throws IllegalStateException if the Any is not a timestamp.
     */
    default Instant asTimestamp() {
        throw new IllegalStateException("Expected a timestamp value, but found " + type());
    }

    /**
     * Get the list contents of the Any if it is a list.
     *
     * @return the list contents.
     * @throws IllegalStateException if the Any is not a list.
     */
    default List<Any> asList() {
        throw new IllegalStateException("Expected a list value, but found " + type());
    }

    /**
     * Get the map contents of the Any if it is a map.
     *
     * @return the map contents.
     * @throws IllegalStateException if the Any is not a map.
     */
    default Map<Any, Any> asMap() {
        throw new IllegalStateException("Expected a map value, but found " + type());
    }

    /**
     * Get a struct or union member from the Any by name.
     *
     * @param memberName Member to access from the Any. For Any types with a schema, this name is the name of the
     *                   member defined in a Smithy model.
     * @return the member, or null if not found.
     * @throws IllegalStateException if the Any is not a structure or union shape.
     */
    default Any getStructMember(String memberName) {
        throw new IllegalStateException("Expected a structure or union value, but found " + type());
    }

    /**
     * Attempt to deserialize the Any into a builder.
     *
     * @param builder Builder to populate from the Any.
     * @param <T> Shape type to build.
     */
    default <T extends SerializableShape> void deserializeInto(SdkShapeBuilder<T> builder) {
        builder.deserialize(new AnyDeserializer(this));
    }

    /**
     * Attempt to deserialize the Any into a builder and create the shape.
     *
     * @param builder Builder to populate from the Any.
     * @return the built and error-corrected shape.
     * @param <T> Shape type to build.
     */
    default <T extends SerializableShape> T asShape(SdkShapeBuilder<T> builder) {
        deserializeInto(builder);
        return builder.errorCorrection().build();
    }

    @Override
    default void serialize(ShapeSerializer encoder) {
        new AnySerializer(this).serialize(encoder);
    }
}
