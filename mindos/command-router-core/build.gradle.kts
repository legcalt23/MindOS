plugins {
    id("mindos.kmp-serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":command-router-actions"))
            implementation(project(":command-router-patterns"))
            implementation(project(":command-router-resolver"))
            implementation(project(":mind-security"))
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.kotlinx.serializationCore)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutinesTest)
            implementation(project(":action-executor"))
            implementation(project(":navigation-contracts"))
        }
    }
}