plugins {
    id("mindos.kmp-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":mind-kmp-contracts"))
            api(project(":mind-action-model"))
            api(project(":mind-action-execution"))
            implementation(libs.kotlinx.coroutinesCore)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutinesTest)
            implementation(project(":mind-navigation"))
            implementation(project(":mind-action-validation"))
        }
    }
}
