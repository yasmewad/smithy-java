$version: "2.0"

namespace test.smithy.java.codegen.server

service TestService {
    version: "today"
    operations: [Echo, GetPersonImage, EmptyOp]
}

operation EmptyOp {
    input: Unit
    output: Unit
}

operation Echo {
    input: EchoInput
    output: EchoOutput
}

operation GetPersonImage {
    input: GetPersonImageInput,
    output: GetPersonImageOutput
}

structure EchoInput {
    String: String
}

structure EchoOutput {
    string: String
}

structure GetPersonImageInput {
    name: String
}

structure GetPersonImageOutput {
    @required
    image: Stream
}

@streaming
blob Stream


