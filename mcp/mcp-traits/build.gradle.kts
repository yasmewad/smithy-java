plugins {
    id("smithy-java.module-conventions")
    alias(libs.plugins.smithy.gradle.jar)
}

description = "MCP Traits"

extra["displayName"] = "Smithy :: Java :: MCP Traits"
extra["moduleName"] = "software.amazon.smithy.java.mcp.traits"

dependencies {
    api(libs.smithy.model)
    compileOnly(libs.smithy.traitcodegen)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.assertj.core)
}

sourceSets {
    val traitsPath = smithy.getPluginProjectionPath(smithy.sourceProjection.get(), "trait-codegen")
    sourceSets {
        main {
            java {
                srcDir(traitsPath)
                include("software/**")
            }

            smithy {
                srcDir("$traitsPath/model")
            }

            resources {
                srcDir(traitsPath)
                exclude("**/*.java")
            }
        }
    }
}

tasks.sourcesJar {
    dependsOn("smithyJarStaging")
}

spotbugs {
    ignoreFailures = true
}

java.sourceSets["main"].java {
    srcDirs("model", "src/main/smithy")
}
