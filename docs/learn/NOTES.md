# Teaching Notes — protocol track

## User preferences / context
- User (yasmewad) discovered Matt Pocock's `teach` skill; this is the first real run of it.
- Mission is PUBLISHABLE for smithy.io/learn, not just personal learning. Audience floor: zero codegen knowledge.
- Inspiration + prior art: PR yasmewad/smithy-java#3 (7-lesson contributor-onboarding track). We are NOT copying it —
  new mission needs a gentler "what even is codegen" on-ramp before protocol internals.
- User is open to a screen recording / demo of the work (a la the walkthrough.mp4 in PR #3), and mused about smithy.io someday.

## Design decisions for this workspace
- Lessons live under docs/learn/ (PR-ready path) — HTML + Markdown twin per lesson, shared stylesheet in assets/.
- Quizzes: retrieval practice; per the skill, every option in a question is the SAME word/char count so formatting leaks no answer.
- Every claim cites the KT doc (resources/smithy-java-kt.md) or a real file path/signature.
- Track shape (draft): on-ramp (what is Smithy/codegen/protocol) → ClientProtocol contract → smallest real protocol →
  schema-directed serde → codec layer → extensions & SPI discovery → choosing a base class → proving it (compliance tests).
