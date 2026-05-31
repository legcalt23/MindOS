plugins {
    id("mindos.kmp-serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":mind-action-model"))
            implementation(libs.kotlinx.coroutinesCore)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutinesTest)
        }
    }
}