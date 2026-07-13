# Mission: How to add a new protocol to smithy-java

## Why
A developer lands on smithy.io/learn wanting to work with smithy-java but has *never
touched code generation* — the word "codegen" is a wall, not a door. They need to go
from "I don't know what a protocol or a codec is" to confidently reading, and eventually
adding, a protocol in smithy-java. The track exists to make an intimidating corner of the
framework (codegen, schema-directed serde, SPI wiring) feel approachable and testable.

## Success looks like
- A total newcomer can explain, in their own words, what "code generation" means in Smithy and why the framework separates *shapes* from *protocols*.
- The learner can read `ClientProtocol.java` and name what each method is for without prior Java-SDK experience.
- The learner can point to the smallest real protocol (`RpcV2CborProtocol`) and explain how it satisfies the contract mostly by inheritance.
- The learner can list the concrete steps to add a new protocol — base class, codec reuse-vs-write, `SchemaExtensionProvider`, `META-INF/services` registration, compliance test — and knows *why* each step matters.
- Every claim in every lesson is backed by a citation to the KT doc or a real file path/signature in the codebase (no hand-waving).
- The artifact is publishable: self-contained HTML lessons with quizzes, plain-Markdown twins, a glossary, and a landing page, living under `docs/learn/` so it can become a smithy.io/learn PR.

## Constraints
- **Audience floor is low:** assume no codegen background and no AWS SDK familiarity. Fluency in general programming only.
- **Anti-parametric:** knowledge comes from the KT doc (`resources/smithy-java-kt.md`) and the real code in this repo — never from the model's memory of how SDKs "usually" work.
- **Publishable bar:** lessons must be beautiful (Tufte-clean), self-contained (vanilla JS, no network), and consistent (one shared stylesheet component).
- **Desirable difficulty:** knowledge is taught gently (difficulty is the enemy for acquisition); skills are drilled with retrieval-practice quizzes (difficulty is the tool for retention).

## Out of scope
- Server-side protocols and MCP (both `@SmithyUnstableApi` per the KT doc — moving targets).
- Deep performance internals (SWAR, Schubfach, BDD VM) beyond a one-line "why it's fast" mention.
- Actually authoring a novel production protocol end-to-end in code — the track teaches the *path*, it is not a merge-ready protocol implementation.
