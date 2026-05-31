plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":mind-navigation"))
            implementation(project(":mind-action-model"))
            implementation(libs.decompose)
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.kotlinx.serializationCore)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutinesTest)
            implementation(libs.essenty.lifecycle)
            // Full pipeline integration tests: understanding → execution → coordinator → Decompose.
            implementation(project(":mind-action-execution"))
            implementation(project(":mind-reference-resolver"))
            implementation(project(":mind-action-validation"))
            implementation(project(":mind-action-matching"))
            implementation(project(":mind-action-patterns"))
            implementation(project(":mind-action-core"))
            implementation(project(":mind-entity-extraction"))
        }
    }
}
