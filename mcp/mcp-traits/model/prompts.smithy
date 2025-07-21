$version: "2"

namespace amazon.smithy.llm

// Prompt template trait - applied at operation level to provide guidance to LLMs
@trait(selector: "service, resource, operation")
map prompts {
    /// Name of the prompt template
    key: String

    /// Definition of the prompt template
    value: PromptTemplateDefinition
}

/// Defines the structure of the prompt
structure PromptTemplateDefinition {
    /// Description of when to use this operation
    @required
    description: String

    /// Template text with placeholders for parameters. A team can also define workflows such as calling multiple API operation.
    @required
    template: String

    /// Optional parameter descriptions to help LLMs understand parameter usage
    arguments: ArgumentShape

    /// Optional condition that describes when this operation should be preferred
    preferWhen: String
}

@idRef(failWhenMissing: true, selector: "structure")
string ArgumentShape
