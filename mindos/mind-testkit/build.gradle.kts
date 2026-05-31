plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm()

    sourceSets {
        // Reusable test infrastructure lives in commonMain so any module can depend on it
        // as a testImplementation. Pure Kotlin stdlib + kotlin.time + kotlinx-serialization (for
        // machine-readable dataset export) — zero engine/production deps.
        commonMain.dependencies {
            implementation(libs.kotlinx.serializationJson)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
