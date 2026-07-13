# How to add a new protocol to smithy-java

*An interactive learn track · 8 short lessons · start from zero code-generation background.*

Start from zero and finish able to read, and add, a protocol in smithy-java. One concrete task, used as a guided tour of the whole framework. Each lesson is ~5–8 minutes and ends with a self-check.

Prefer the interactive version with quizzes? Open [`index.html`](index.html). Keep the [glossary](../reference/glossary.md) handy.

## The track

1. [What a “protocol” even is](0001-what-is-a-protocol.md) — the pain Smithy solves, what codegen means, why adding a protocol is bounded.
2. [The `ClientProtocol` contract](0002-the-clientprotocol-contract.md) — one interface, six methods, and where each fires in `client.call(...)`.
3. [The smallest real protocol](0003-the-smallest-real-protocol.md) — `RpcV2CborProtocol` in ~45 lines, satisfied mostly by inheritance.
4. [Schema-directed serde](0004-schema-directed-serde.md) — the trick that lets one generated class serialize to any protocol.
5. [The codec layer](0005-the-codec-layer.md) — what a `Codec` is, and reuse-vs-write (protocol axis vs codec axis).
6. [Extensions & discovery](0006-extensions-and-discovery.md) — `SchemaExtensionProvider` + `META-INF/services`.
7. [Choosing your base class](0007-choosing-your-base-class.md) — RPC (`HttpClientProtocol`) vs REST (`HttpBindingClientProtocol`).
8. [Proving it — compliance tests](0008-proving-it-compliance-tests.md) — the authoritative correctness bar + the full checklist.

## Grounding

Every claim is anchored to the real [smithy-lang/smithy-java](https://github.com/smithy-lang/smithy-java) source and the internal *Smithy Java Knowledge Transfer* doc — signatures and paths quoted verbatim, no hand-waving.
