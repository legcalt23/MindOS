plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":mind-normalization"))
            implementation(libs.kotlinx.serializationCore)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
