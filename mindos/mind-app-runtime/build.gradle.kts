plugins {
    id("mindos.kmp-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":mind-action-execution"))
            api(project(":mind-action-validation"))
            api(project(":mind-command-runtime"))
            api(project(":mind-kmp-contracts"))
            api(project(":mind-navigation"))
            api(project(":mind-observability"))
            api(project(":mind-reference-resolver"))
            api(project(":mind-security"))
            api(project(":mind-speech"))
            implementation(project(":mind-kmp-core"))
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutinesTest)
            implementation(project(":mind-action-model"))
        }
    }
}
