plugins {
    id("mindos.kmp-serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":mind-action-core"))
            implementation(project(":mind-entity-extraction"))
            implementation(libs.kotlinx.serializationCore)
        }
    }
}