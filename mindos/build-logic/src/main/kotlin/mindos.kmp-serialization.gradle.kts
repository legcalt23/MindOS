/*
 * Convention: a KMP library (see mindos.kmp-library) with kotlinx.serialization enabled.
 * Modules add the serialization-core/json runtime dependency they need; this only applies the
 * compiler plugin so @Serializable works.
 */
plugins {
    id("mindos.kmp-library")
    id("org.jetbrains.kotlin.plugin.serialization")
}
