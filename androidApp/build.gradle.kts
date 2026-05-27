import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
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
        implementation(libs.compose.uiToolingPreview)
    }
}

android {
    namespace = "com.example.mindos"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.mindos.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            // Values injected via environment variables in CI or local.properties.
            // To build a signed release locally, set these in ~/.gradle/gradle.properties:
            //   MINDOS_KEYSTORE_FILE=/path/to/mindos-release.jks
            //   MINDOS_KEYSTORE_PASSWORD=****
            //   MINDOS_KEY_ALIAS=mindos
            //   MINDOS_KEY_PASSWORD=****
            val keystoreFile = findProperty("MINDOS_KEYSTORE_FILE") as? String
            if (keystoreFile != null) {
                storeFile = file(keystoreFile)
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
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseKeystore = signingConfigs.findByName("release")
                ?.takeIf { it.storeFile?.exists() == true }
            signingConfig = releaseKeystore ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(platform(libs.firebase.androidBom))
    // App Check artifacts are version-managed by firebase-bom (no explicit version).
    implementation(libs.firebase.appcheck.playintegrity)

    debugImplementation(libs.compose.uiTooling)
    debugImplementation(libs.firebase.appcheck.debug)
}
