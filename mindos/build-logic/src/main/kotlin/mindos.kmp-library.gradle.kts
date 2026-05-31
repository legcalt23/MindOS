/*
 * Convention: a KMP library module targeting the JVM with a common test source set.
 * Centralizes the boilerplate every MindOS module repeated (apply KMP, jvm() target,
 * kotlin("test") in commonTest). Module-specific dependencies stay in each module.
 */
plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    jvm()

    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
