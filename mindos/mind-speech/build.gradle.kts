plugins {
    id("mindos.kmp-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":mind-command-runtime"))
            implementation(project(":mind-kmp-contracts"))
            implementation(libs.kotlinx.coroutinesCore)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutinesTest)
            implementation(project(":mind-action-execution"))
            implementation(project(":mind-action-model"))
            implementation(project(":mind-kmp-core"))
            implementation(project(":mind-navigation"))
        }
    }
}
