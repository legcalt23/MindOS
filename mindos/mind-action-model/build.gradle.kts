plugins {
    id("mindos.kmp-serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serializationCore)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.serializationJson)
        }
    }
}
