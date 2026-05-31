plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

// Kotlin Gradle plugins on the classpath so the precompiled convention plugins can apply them.
// Versions are pinned here (build infrastructure); the main catalog pins the same Kotlin version.
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.10")
    implementation("org.jetbrains.kotlin:kotlin-serialization:2.3.10")
}
