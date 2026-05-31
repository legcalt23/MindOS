plugins {
    id("mindos.kmp-serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":mind-action-model"))
            implementation(project(":mind-navigation"))
            implementation(project(":mind-reference-resolver"))
            implementation(project(":mind-action-validation"))
            implementation(project(":mind-observability"))
            implementation(project(":mind-action-core"))
            implementation(project(":mind-entity-extraction"))
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.kotlinx.serializationCore)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutinesTest)
            // Test-only: full pipeline text -> understanding (ZIP02) -> action -> execution (ZIP03).
            implementation(project(":mind-action-matching"))
            implementation(project(":mind-action-patterns"))
            implementation(project(":mind-testkit"))
        }
    }
}