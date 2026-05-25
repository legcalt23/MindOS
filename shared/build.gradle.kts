import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    // CocoaPods is declared with apply=false so the plugin is resolvable on every host,
    // but it is only actually applied on macOS (see `if (isMacOs)` block below) because
    // CocoaPods integration requires Xcode/pod which only exist on Mac. This lets Windows
    // and Linux developers build the Android/Desktop targets without iOS tooling.
    alias(libs.plugins.kotlinNativeCocoapods) apply false
}

val isMacOs = System.getProperty("os.name") == "Mac OS X"

if (isMacOs) {
    apply(plugin = "org.jetbrains.kotlin.native.cocoapods")
}

kotlin {
    android {
        namespace = "com.example.neuv3.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }

    if (isMacOs) {
        listOf(
            iosArm64(),
            iosSimulatorArm64(),
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "Shared"
                isStatic = true
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.materialIconsExtended)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            // material3 is consumed through the compose-multiplatform plugin DSL so its
            // version is always aligned with the active compose-multiplatform release,
            // instead of being independently pinned in the version catalog.
            implementation(compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.kotlinx.serializationCore)
            api(libs.firebase.app)
            implementation(libs.firebase.auth)
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.functions)
            implementation(libs.firebase.storage)
            implementation(libs.kotlinx.datetime)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutinesTest)
        }

        androidMain.dependencies {
            api(project.dependencies.platform(libs.firebase.androidBom))
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.camera.core)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
            implementation(libs.mlkit.face.detection)
            implementation(libs.coil.compose)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.ui)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

if (isMacOs) {
    extensions.configure<CocoapodsExtension>("cocoapods") {
        summary = "Shared Kotlin Multiplatform module for NEUv3"
        homepage = "https://example.com/neuv3"
        version = "1.0"
        ios.deploymentTarget = "15.0"
        pod("FirebaseCore")
        pod("FirebaseAuth")
        extraSpecAttributes["resources"] = "['src/iosMain/resources/GoogleService-Info.plist']"

        framework {
            baseName = "Shared"
            isStatic = true
        }
    }
}
