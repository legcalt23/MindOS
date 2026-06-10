import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.crashlytics)
}

kotlin {
    target {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    dependencies {
        implementation(projects.shared)
        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.core.splashscreen)
        implementation(libs.compose.uiToolingPreview)
    }
}

android {
    namespace = "com.mindos.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.mindos.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()

        // versionCode / versionName are derived from the build environment so every
        // CI release produces a strictly increasing, traceable version without
        // hand-editing this file (a fixed versionCode=1 cannot be published twice).
        //
        // Resolution order (first non-null wins):
        //   versionCode  → MINDOS_VERSION_CODE  → GITHUB_RUN_NUMBER → 1 (local dev)
        //   versionName  → MINDOS_VERSION_NAME  → "1.0"             (local dev)
        //
        // CI sets MINDOS_VERSION_CODE (e.g. from the run number or a release tag) and
        // MINDOS_VERSION_NAME (e.g. the semantic tag). Local builds fall back to 1/"1.0".
        val resolvedVersionCode =
            (System.getenv("MINDOS_VERSION_CODE") ?: System.getenv("GITHUB_RUN_NUMBER"))
                ?.toIntOrNull()
                ?: 1
        val resolvedVersionName = System.getenv("MINDOS_VERSION_NAME") ?: "1.0"

        versionCode = resolvedVersionCode
        versionName = resolvedVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Values injected via environment variables in CI or properties on disk.
            // See gradle.properties.example for full documentation. To build a signed
            // release locally, set these in ~/.gradle/gradle.properties (NEVER the
            // repo-root gradle.properties):
            //   MINDOS_KEYSTORE_FILE=/path/to/mindos-release.jks
            //   MINDOS_KEYSTORE_PASSWORD=****
            //   MINDOS_KEY_ALIAS=mindos
            //   MINDOS_KEY_PASSWORD=****
            val keystorePath = findProperty("MINDOS_KEYSTORE_FILE") as? String
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = findProperty("MINDOS_KEYSTORE_PASSWORD") as? String
                keyAlias = findProperty("MINDOS_KEY_ALIAS") as? String ?: "mindos"
                keyPassword = findProperty("MINDOS_KEY_PASSWORD") as? String
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }

        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Pick the real release signing config when the keystore exists on
            // disk. Otherwise fall back to debug signing so CI can still run R8 /
            // ProGuard validation without holding the keystore. Local devs see a
            // warning in the build log when fallback kicks in so a debug-signed
            // APK never gets shipped to Play Store by accident.
            val releaseSigning = signingConfigs.findByName("release")
                ?.takeIf { it.storeFile?.exists() == true }
            if (releaseSigning != null) {
                signingConfig = releaseSigning
            } else {
                signingConfig = signingConfigs.getByName("debug")
                logger.warn(
                    "⚠️  androidApp:release is being signed with the DEBUG keystore " +
                        "because MINDOS_KEYSTORE_FILE is not set or the file is missing. " +
                        "This APK is NOT publishable to Play Store. " +
                        "See gradle.properties.example to configure release signing."
                )
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        // Gradle Managed Devices: CI can run instrumented tests on a provisioned
        // emulator with `./gradlew :androidApp:pixel6Api34DebugAndroidTest` (no
        // manual device needed). aosp-atd images are headless/CI-friendly.
        managedDevices {
            localDevices {
                create("pixel6Api34") {
                    device = "Pixel 6"
                    apiLevel = 34
                    systemImageSource = "aosp-atd"
                }
            }
        }
    }
}

dependencies {
    implementation(platform(libs.firebase.androidBom))
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    // Performance Monitoring — SDK only (no firebase-perf Gradle plugin). The SDK
    // alone collects automatic app-start / foreground-background traces and exposes
    // the custom-trace API. The Gradle plugin (automatic network/HTTP instrumentation)
    // is deferred pending AGP 9.x compatibility verification — see PHASE6_NOTES.md.
    implementation(libs.firebase.perf)
    // Remote Config — backs the FeatureFlags kill switches (privacy overlay, waves, wallet).
    implementation(libs.firebase.config)

    debugImplementation(libs.compose.uiTooling)
    debugImplementation(libs.firebase.appcheck.debug)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
