# Lesson 3 — Schema-Directed Serde

> **smithy-java · protocol track · lesson 3**
> **Goal:** Understand *why* protocols and generated code are decoupled.
> **Time:** ~12 min · **Builds on:** Lesson 2 (the codec is the pivot).

Lesson 2 ended on `codec().serialize(input)`. *How* does that produce JSON or CBOR from a generated
shape — with no reflection, and without the shape knowing which protocol is active? This is the
single most important idea in the framework.

---

## 1. The problem it solves

You have generated shape classes (`PutItemInput`, etc.) and protocols (JSON, CBOR, XML). The naive
design couples them: each shape knows how to write itself as JSON. Then adding a protocol means
regenerating every shape, and each shape carries N serializers. smithy-java refuses that.

The insight: put a **`Schema`** between them. The shape knows its schema and pushes its members;
the protocol reads schemas and decides wire format. **Neither imports the other.**

> **One line to remember:** the `Schema` is the first argument to *every* serde method. That's not
> a style choice — it's the entire decoupling mechanism. The schema is the contract; the protocol
> and the shape only ever talk *through* it.

---

## 2. The two sides: push and interpret

**① The shape pushes (generated code):**

```java
public void serializeMembers(ShapeSerializer ser) {
    ser.writeString($SCHEMA_NAME, name);   // each member pushed WITH its schema
    ser.writeInteger($SCHEMA_AGE, age);    // the shape has no idea if this is JSON or CBOR
}
```

**② The codec interprets:** it implements `ShapeSerializer` and reads the schema to decide bytes.

```java
void writeString(Schema s, String v) {
    // JSON codec: look up the precomputed field-name bytes for s, arraycopy, write v
}
```

The interface that connects them — every method takes a `Schema` first:

```java
public interface ShapeSerializer extends Flushable, AutoCloseable {
    void writeStruct(Schema schema, SerializableStruct struct);
    void writeString(Schema schema, String value);
    void writeInteger(Schema schema, int value);
    void writeTimestamp(Schema schema, Instant value);
    <T> void writeList(Schema schema, T state, int size, BiConsumer<T, ShapeSerializer> consumer);
    // ... every write takes the member's Schema first
}
```

> Source: [`core/.../core/serde/ShapeSerializer.java`](https://github.com/smithy-lang/smithy-java/blob/main/core/src/main/java/software/amazon/smithy/java/core/serde/ShapeSerializer.java)

---

## 3. Deserialization inverts it: a callback by index

Reading is the mirror image. The codec reads wire bytes, resolves them to a member schema, and
calls back. The generated builder dispatches on `memberIndex()`, which the compiler turns into a
`tableswitch` — no string compares:

```java
decoder.readStruct($SCHEMA, this, (builder, member, de) -> {
    switch (member.memberIndex()) {        // JVM tableswitch — O(1)
        case 0 -> builder.name(de.readString(member));
        case 1 -> builder.age(de.readInteger(member));
    }
});
```

The codec owns the field loop; the shape owns the per-field action. Again: they meet only at the
schema.

---

## 4. Why no trait lookups on the hot path

"The schema decides the wire format" sounds like it'd inspect traits (`@jsonName`,
`@timestampFormat`) at write time. It doesn't — that would be slow. Instead, each protocol
**precomputes** what it needs onto the schema *once*, into a *schema extension*:

| Extension | Precomputes | So `writeString` can… |
|---|---|---|
| `SmithyJsonSchemaExtensions` | `byte[][]` field-name tables (with quotes+colon, honoring `@jsonName`) | do `table[memberIndex]` → `System.arraycopy` |
| `CborSchemaExtensions` | `byte[][]` CBOR-encoded field names | same O(1) array index |
| `HttpBindingSchemaExtensions` | binding location (HEADER/QUERY/LABEL/BODY), timestamp formats | route the member to the right HTTP place |

At serialization there are *no* trait lookups, no hashing — just an array index and a memory copy.
The trait reading happened once, at schema construction. (You'll meet the extension SPI properly in
Lesson 5.)

> **The protocol-author payoff:** because the same `serializeMembers()` works for any codec,
> *adding a protocol never requires regenerating shapes*. You write a codec (or reuse one) + a
> schema extension to precompute your wire data. The generated code is already protocol-agnostic
> and waiting for you.

---

## 5. Quick check

**Q1.** Why is `Schema` the first argument to every `ShapeSerializer` method?
- a) For logging/debugging — it identifies the field by name.
- b) Java requires interface methods to share a first parameter.
- c) It's the decoupling contract: the shape pushes a member + its schema; the codec reads the schema to decide wire format.

**Q2.** Why not read the `@jsonName` trait inside `writeString` to get the field name?
- a) Traits aren't available at runtime.
- b) It's on the hot path; the field-name bytes are precomputed into a schema extension once — `writeString` just indexes + arraycopies.
- c) `@jsonName` only exists for XML protocols.

**Q3.** Why `switch(member.memberIndex())` instead of matching field names on deserialize?
- a) The compiler turns it into a `tableswitch` — O(1) dispatch, no string comparisons.
- b) Field names aren't known until runtime.
- c) It's required by the `ServiceLoader` SPI.

<details><summary><strong>Answers</strong></summary>

- **Q1 → c.** The schema is the meeting point — that's what lets one generated shape serialize as JSON, CBOR, or XML unchanged.
- **Q2 → b.** Precompute-once, index-many. Per-field trait inspection would be death by a thousand cuts (Tenet 3: Performance).
- **Q3 → a.** The codec resolves wire bytes → member schema (with a speculative fast path), then the shape dispatches by dense integer index. Fast and allocation-free.

</details>

---

## 6. What you now know

You can explain *why* a protocol and a generated shape never know about each other, and how the
`Schema`-as-first-argument design makes one shape serialize to any format. Wire decisions are
precomputed into schema extensions, not read per-write.

**Next — Lesson 4:** the `Codec` itself — the object your protocol returns from `payloadCodec()`,
and the decision of whether to reuse or write one.

## Reference
- [Glossary](../reference/glossary.html)
- [`ShapeSerializer.java`](https://github.com/smithy-lang/smithy-java/blob/main/core/src/main/java/software/amazon/smithy/java/core/serde/ShapeSerializer.java)
