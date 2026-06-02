plugins {
    id("mindos.kmp-serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":mind-kmp-contracts"))
            implementation(project(":mind-action-core"))
            implementation(project(":mind-action-matching"))
            implementation(project(":mind-action-patterns"))
            implementation(project(":mind-entity-extraction"))
            implementation(project(":mind-normalization"))
            implementation(project(":mind-security"))
            implementation(project(":mind-observability"))
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.kotlinx.serializationCore)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutinesTest)
        }
    }
}
