$version: "2.0"

namespace smithy.java.codegen.server.test

service TestService {
    version: "today"
    operations: [GetBeer, Echo]
}

operation GetBeer {
    input: GetBeerInput
    output: GetBeerOutput
}

operation Echo {
    input: EchoInput
    output: EchoOutput
}

structure EchoInput {
    string: String
}

structure EchoOutput {
    string: String
}

structure Beer {
    name: String
    id: Long
}

list BeerList {
    member: Beer
}

structure GetBeerInput {
    @required
    id: Long
}

structure GetBeerOutput {
    @required
    value: BeerList
}


