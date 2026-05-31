plugins {
    id("mindos.kmp-serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":mind-normalization"))
            implementation(libs.kotlinx.serializationCore)
        }
    }
}