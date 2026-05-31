plugins {
    id("mindos.kmp-serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":mind-action-core"))
            implementation(project(":mind-action-patterns"))
            implementation(project(":mind-entity-extraction"))
            implementation(project(":mind-normalization"))
            implementation(project(":mind-observability"))
            implementation(project(":mind-security"))
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.kotlinx.serializationCore)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutinesTest)
            implementation(libs.kotlinx.serializationJson)
            implementation(project(":mind-testkit"))
            // Test-only: prove end-to-end bridge into the foundation
            // (understanding -> MindIntent -> CommandAction -> executor owns navigation).
            implementation(project(":command-router-actions"))
            implementation(project(":action-executor"))
            implementation(project(":navigation-contracts"))
        }
    }
}