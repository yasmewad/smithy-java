# How to add a new protocol to smithy-java — Resources

## Knowledge

- [KT Doc: "Smithy Java, Knowledge Transfer" — internal Pippin artifact](https://pippin.amazon.dev/reviews/smithy_musings/artifacts/Y84gPFVTqbKJ)
  The authoritative framework rundown: 16 subsystems, tiered reading order, design principles,
  and the "Adding a new codec / protocol / HTTP-bound trait" workflows. **Primary source for every lesson.**
  A verbatim copy is stashed at [`resources/smithy-java-kt.md`](./resources/smithy-java-kt.md) for offline grounding.
- The smithy-java codebase itself (this repo). Verified real anchors for the track:
  - `client/client-core/.../ClientProtocol.java` + `ClientProtocolFactory.java` — the contract.
  - `client/client-rpcv2-cbor/.../RpcV2CborProtocol.java` + `client/client-rpcv2/.../AbstractRpcV2ClientProtocol.java` — smallest real protocol.
  - `client/client-http/.../HttpClientProtocol.java` (RPC base) and `client/client-http-binding/.../HttpBindingClientProtocol.java` (REST base).
  - `aws/client/aws-client-restjson/.../RestJsonClientProtocol.java`, `.../restxml/RestXmlClientProtocol.java`, `.../awsjson/AwsJson*Protocol.java`, `.../awsquery/AwsQueryClientProtocol.java` — the family.
  - `codecs/` (`Codec`, `ShapeSerializer`, `ShapeDeserializer`), `core/.../schema/` (Schema, extensions), `META-INF/services/` registrations.
  - `protocol-test-harness/` — the authoritative compliance-test bar.
- [Smithy codegen guide (public)](https://smithy.io/2.0/guides/building-codegen/index.html)
  Referenced by the KT doc for the codegen subsystem. Use for the "what is codegen" on-ramp.
- [Smithy client-error-correction spec](https://smithy.io/2.0/spec/aggregate-types.html#client-error-correction)
  Grounds the deserialization-safety lesson content.

## Wisdom (Communities)

- [smithy-lang/smithy-java GitHub Discussions & Issues](https://github.com/smithy-lang/smithy-java)
  Where real protocol/codegen questions get answered by maintainers. Use for: design intent, "is this the blessed pattern?"
- The Smithy team (internal) — the KT doc authors (`sugmanue`, `yefrig`). Use for: verifying non-obvious design decisions before publishing.

## Gaps
- No public, newcomer-facing "add a protocol" walkthrough exists yet on smithy.io — that gap is exactly what this track fills.
- Prior internal run: PR yasmewad/smithy-java#3 built a 7-lesson track for a *contributor-onboarding* mission. This track's mission is lower-floor (zero-codegen newcomer) and publishable, so it re-derives the on-ramp rather than copying #3.
