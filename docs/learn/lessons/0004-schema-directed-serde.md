# Lesson 4 of 8 · One class, every wire format

*Section: Why it decouples · ≈ 7 min · real code · the core idea*

The single trick that lets a generated class serialize to JSON, CBOR, or XML
unchanged: the schema is the first argument to every serde method — and the
protocol, not the class, decides the bytes.

Lesson 1 promised that the *same* generated shape class can hit the wire as
JSON, CBOR, or XML without being touched. This lesson shows you the mechanism
that makes that true. It is one idea, and once you see it, the rest of the
framework clicks into place. The KT doc calls it **schema-directed serde**.

## The problem it solves

A generated class like `GetFoosInput` holds a field called `id`. Somebody has
to turn that field into bytes. If the *class itself* knew how to write JSON, it
would be welded to JSON forever — adding CBOR would mean regenerating every
class. So the class must *not* know the wire format. But then how does it hand
its data off to something that does?

> **The one idea to hold onto.** The generated class *pushes* its members — it
> says "here is a string field, here is its schema" — and hands them to a
> visitor. The visitor is the protocol's codec. The class supplies *what*; the
> codec decides *how*. Neither imports the other.

## The visitor: `ShapeSerializer`

The visitor is an interface named `ShapeSerializer`, and every one of its write
methods takes a `Schema` as its first argument. That is the whole design in one
line.

File: `core/src/main/java/software/amazon/smithy/java/core/serde/ShapeSerializer.java`

```java
public interface ShapeSerializer extends Flushable, AutoCloseable {

    void writeStruct(Schema schema, SerializableStruct struct);

    <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer);

    void writeString(Schema schema, String value);

    void writeTimestamp(Schema schema, Instant value);
}
```

Read `writeString(Schema schema, String value)` slowly. The caller passes the
value *and* the schema for the member that value came from. The value is the
payload; the schema is the instructions. A JSON codec reads the schema to find
the field-name bytes; a CBOR codec reads the *same* schema for its own encoded
field name. Same call, different bytes.

## The push side: generated code

Here is a real generated class from the test suite. Note it never mentions
JSON, CBOR, or XML — it only pushes.

File: `client/client-waiters/src/test/java/software/amazon/smithy/java/client/waiters/models/GetFoosInput.java`

```java
public record GetFoosInput(String id) implements SerializableStruct {
    public static final Schema SCHEMA = Schema.structureBuilder(ID)
            .putMember("id", PreludeSchemas.STRING)
            .build();
    public static final Schema SCHEMA_ID = SCHEMA.member("id");

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString(SCHEMA_ID, id);
    }
}
```

The method `serializeMembers(ShapeSerializer)` is the push. For each member it
calls one write method, passing that member's schema and the value. The KT doc
names this the *push-based serialization* principle: "Generated types implement
`serializeMembers(ShapeSerializer)` and push their own fields into the
serializer." No reflection, no format knowledge — just a list of pushes.

> **Why the schema, not the class, carries the wire rules.** Because the schema
> is passed to every method, one `serializeMembers()` works with any codec. The
> KT doc: "The same generated `serializeMembers()` works unchanged with JSON,
> CBOR, XML, or any future protocol. Switching protocols at runtime requires
> only a config/dependency change — no regeneration."

## Traits are never read at write time

You might expect the codec, at the moment of writing, to inspect the schema's
traits — "does this field have a `@jsonName`? what timestamp format?" It does
**not**. That would be slow, and it would happen on every single call. Instead,
every protocol decision is *precomputed once* when the schema is built, and
stashed in a per-protocol **extension**.

The KT doc is emphatic here:

> Schema traits are **never inspected at write-time**. All protocol decisions
> are precomputed into extensions (byte arrays, formatters, lookup tables) at
> Schema construction. At serialization: `currentFieldNameTable[memberIndex]`
> (O(1)) then `System.arraycopy`. No hashing, no trait lookups on the hot path.

So at write time the codec does two cheap things: it looks up the member's
precomputed field-name bytes by *index* (`fieldNameTable[memberIndex]`), then
copies those bytes with `System.arraycopy`. Each protocol has its own table —
`SmithyJsonSchemaExtensions` precomputes JSON field-name bytes,
`CborSchemaExtensions` precomputes CBOR-encoded names — but the generated class
is blind to which table is in play. Both extension providers are real classes
in the repo (`codecs/json-codec/.../SmithyJsonSchemaExtensions.java`,
`codecs/cbor-codec/.../CborSchemaExtensions.java`); Lesson 6 is devoted to them.

> **The trap this design creates.** If you add a trait that should change the
> wire but forget to teach its `SchemaExtensionProvider` about it, nothing
> breaks loudly — the codec simply never sees it. The KT doc lists this as a
> rule: "Adding a trait without updating its `SchemaExtensionProvider` silently
> has no wire effect." Remember that when you reach Lesson 6.

## Reading back: the callback pattern

Deserialization runs the same idea in reverse. Instead of the class pushing
values out, the codec pushes values *in* through a callback. The deserializer
method is `readStruct`, and it hands each member to a small consumer.

File: `core/src/main/java/software/amazon/smithy/java/core/serde/ShapeDeserializer.java`

```java
<T> void readStruct(Schema schema, T state, StructMemberConsumer<T> consumer);

interface StructMemberConsumer<T> {
    void accept(T state, Schema memberSchema, ShapeDeserializer memberDeserializer);
}
```

The generated builder implements that consumer and dispatches on the member's
integer index — a `switch` on `member.memberIndex()`, no string comparisons:

File: `aws/aws-event-streams/src/test/java/software/amazon/smithy/java/aws/events/model/TestOperationOutput.java`

```java
public void accept(Builder builder, Schema member, ShapeDeserializer de) {
    switch (member.memberIndex()) {
        case 0 -> builder.intMemberHeader(de.readInteger(member));
        case 1 -> builder.stringMember(de.readString(member));
        case 2 -> builder.outputStream((EventStream<TestEventStream>) de.readEventStream(member));
        default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
    }
}
```

The codec walks the incoming bytes, and for each field it recognizes it calls
`accept` with that member's schema. The builder switches on the index and pulls
the value out with the matching `read*` method — again passing the schema. The
KT doc notes this `switch` compiles to a JVM *tableswitch*: integer dispatch,
not string matching.

## Putting it together

Serialize: the class pushes members into a `ShapeSerializer`; the codec, holding
precomputed tables, turns each push into bytes. Deserialize: the codec reads
bytes and calls back into a `StructMemberConsumer`; the builder switches on the
index and reads each value. In both directions the schema is the first argument,
and in both directions the class knows nothing about the wire. That is why "add
a protocol" is a bounded job: you write a new codec that interprets schemas, and
every existing generated class works with it for free.

## Self-check

**At serialization time, how does the codec find a member's field name?**

- [ ] It reads the member schema traits on each single call
- [ ] It hashes the member name string on each write call
- [x] **It reads a precomputed table by the member index once**
- [ ] It asks the generated class which wire format to emit

Decisions are precomputed at schema construction; at write time the codec does
`fieldNameTable[memberIndex]` then `System.arraycopy`. Traits are never read on
the hot path.

## Primary source

KT doc §2 "Schemas & Schema-Directed Serde". Verified in
`core/.../serde/ShapeSerializer.java`, `core/.../serde/ShapeDeserializer.java`,
and generated code `GetFoosInput.java` / `TestOperationOutput.java`. Background:
<https://smithy.io/2.0/>.

---

← Prev: [The smallest real protocol](0003-the-smallest-real-protocol.md) ·
[Track overview](index.md) ·
Next: [The codec layer](0005-the-codec-layer.md) →
