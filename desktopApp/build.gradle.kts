import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

java {
    // Align Java compilation with Kotlin target so the JVM plugin does not fail the task-pair check.
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        // Aligned with :shared jvm("desktop") and :androidApp to keep bytecode level uniform.
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(projects.shared)
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)
}

compose.desktop {
    application {
        mainClass = "com.example.neuv3.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "NEUv3"
            packageVersion = "1.0.0"
        }
    }
}
