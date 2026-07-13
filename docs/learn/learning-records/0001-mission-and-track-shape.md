# Mission set: publishable smithy.io/learn track for codegen newcomers

The mission is a **publishable** smithy.io/learn track teaching "how to add a new protocol to
smithy-java" to people with **zero code-generation background** — not internal contributor
onboarding. This is a lower audience floor than the prior run (PR yasmewad/smithy-java#3, which
targeted contributor onboarding), so the track re-derives a gentle on-ramp (Lesson 1: "what a
protocol even is" — explains model/shape/codegen from scratch) before any interface or code.

**Why it matters for future sessions:** the anti-parametric constraint is hard here — the KT doc
(stashed at `resources/smithy-java-kt.md`) and the real repo are the only allowed knowledge
sources. Every lesson cites a verified file path or KT section. The 8-lesson arc is fixed:
on-ramp → ClientProtocol contract → smallest real protocol (RpcV2CborProtocol, ~45 lines) →
schema-directed serde → codec layer → extensions & SPI discovery → REST-vs-RPC base class →
compliance tests + checklist. Server/MCP are explicitly out of scope (`@SmithyUnstableApi`).

**Implication:** if the mission ever shifts toward "actually implement a novel protocol in code,"
that's a new track (the current one teaches the *path*, not a merge-ready implementation) and
warrants a superseding learning record.
