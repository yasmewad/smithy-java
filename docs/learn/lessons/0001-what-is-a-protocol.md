# Lesson 1 · What a “protocol” even is

*On-ramp — ≈ 6 min, no code yet, zero prerequisites.*

> Track: [How to add a new protocol to smithy-java](index.md) · Next: [The `ClientProtocol` contract →](0002-the-clientprotocol-contract.md)

You are here to work with **smithy-java**, and somewhere you hit the word *codegen* and it felt like a locked door. This track opens it — one concrete task, *“add a new protocol,”* used as a guided tour of the whole framework. This first lesson assumes you know *nothing* about code generation. That is the right starting point.

## Start with the pain

Imagine two programs talking over a network — your app and a service. They must agree on *exactly* how a message looks on the wire: is it JSON or binary? Where does the operation name go — in the URL, or a header? How is a date encoded? Every service that gets this slightly different means every client has to be hand-written again. That is thousands of near-identical, bug-prone lines.

## Smithy’s move: describe once, generate the rest

[Smithy](https://smithy.io/2.0/) is a language for *describing* a service — its operations, its inputs and outputs — in the abstract, without saying anything about wire format. That description is a **model**. The pieces inside it — a structure, a field, an operation — are called **shapes**.

**Code generation** (“codegen”) is the step that reads that model and *writes Java code for you*: builder classes, field accessors, the plumbing to send and receive each operation. You describe `GetItem` once; codegen emits the `GetItemInput` class, its builder, and the wiring. That is all “codegen” means here — a program that writes the boilerplate you would otherwise type by hand.

> **The one idea to hold onto.** A *shape* says **what** the data is (“a structure with a required string `name`”). A *protocol* says **how** that data looks on the wire (“JSON, with the name in a `"name"` field”). Smithy keeps these two apart on purpose — that separation is the whole reason adding a protocol is a bounded task and not a rewrite.

## So what is a “protocol,” concretely?

A protocol is the set of rules for turning an operation call into bytes on the wire and turning the response bytes back into an object. smithy-java already ships several, named in the KT doc:

| Protocol | Wire shape | How the operation is routed |
|---|---|---|
| restJson1 | JSON + HTTP bindings | URI + HTTP method |
| restXml | XML + HTTP bindings | URI + HTTP method |
| AWS JSON 1.0/1.1 | full JSON body | `X-Amz-Target` header |
| AWS Query | URL-encoded form | `Action` form parameter |
| rpcv2Cbor | CBOR (compact binary) | fixed URI path |

Notice they all carry the *same* shapes — the difference is purely *how* those shapes hit the wire. Adding a protocol means teaching smithy-java one new set of “how” rules, *without* touching the generated shape code at all.

> **Why this decoupling matters.** The KT doc calls it *protocol agnosticism*: “Switching protocols at runtime requires only a config change.” The same generated class serializes to JSON, CBOR, or XML unchanged. You are adding a plug-in, not editing the engine.

## Where you’re headed

Over the next seven lessons you will meet the one interface a protocol must satisfy (`ClientProtocol`), see the smallest real protocol satisfy it in ~45 lines, understand the schema-directed serde trick behind the decoupling, learn when to reuse a codec versus write one, wire it up for discovery, pick the right base class, and finally prove it with compliance tests. By the end, “add a protocol” will be a checklist, not a mystery.

## Self-check

**In Smithy, what is the division of labor between a shape and a protocol?**

- Shape picks the wire bytes; protocol names the field
- **✅ Shape says what the data is; protocol says how it looks**
- Shape signs the request; protocol retries the failed call
- Shape stores the model; protocol compiles the Java code

Shape = *what* (structure, fields, required-ness). Protocol = *how* (JSON vs CBOR, field placement). That split is why a protocol is a bounded add-on.

## Primary source

Read the model-and-shapes overview at [smithy.io/2.0](https://smithy.io/2.0/). Everything in this track is grounded in the internal *Smithy Java Knowledge Transfer* doc and the real smithy-java source.

---

*Stuck? Ask your teacher — the agent that generated this track is your tutor.*
