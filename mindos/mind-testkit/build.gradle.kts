plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvm()

    sourceSets {
        // Reusable test infrastructure lives in commonMain so any module can depend on it
        // as a testImplementation. Pure Kotlin stdlib + kotlin.time — zero production deps.
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
