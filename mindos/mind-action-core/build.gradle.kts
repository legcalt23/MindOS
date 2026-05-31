plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            // Public contracts only. ActionMatcher.understand is pure/deterministic
            // (not suspend), so this module needs no coroutines.
            implementation(project(":mind-entity-extraction"))
            implementation(project(":mind-observability"))
            implementation(project(":mind-security"))
            implementation(libs.kotlinx.serializationCore)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
