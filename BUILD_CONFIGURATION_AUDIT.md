# BUILD_CONFIGURATION_AUDIT.md

**Date:** 2026-05-27

---

## 1. Build system overview

| Component | Value |
|---|---|
| Build system | Gradle 9.3.1 |
| Kotlin | 2.3.10 |
| AGP | 9.1.0 |
| Compose Multiplatform | 1.10.3 |
| JVM Target | 17 |
| Version catalog | `gradle/libs.versions.toml` |

---

## 2. Module configuration

### settings.gradle.kts

- TYPESAFE_PROJECT_ACCESSORS enabled (good)
- FAIL_ON_PROJECT_REPOS (good — prevents repo leaks)
- Includes: :shared, :androidApp, :desktopApp

### Root build.gradle.kts

- All plugins declared with `apply false` (good — applied per module)
- Plugins: androidApplication, androidMultiplatformLibrary, composeCompiler, composeMultiplatform, googleServices, kotlinJvm, kotlinMultiplatform, kotlinNativeCocoapods, detekt

---

## 3. Version catalog analysis

### Versions centralized: YES

All versions in `libs.versions.toml`. No scattered version declarations.

### Dependencies audit

| Dependency | Version | Status |
|---|---|---|
| Kotlin | 2.3.10 | Current |
| Compose Multiplatform | 1.10.3 | Current |
| Firebase BOM (Android) | 33.15.0 | Current |
| Firebase KMP SDK | 2.4.0 | Current |
| kotlinx-coroutines | 1.10.2 | Current |
| kotlinx-serialization | 1.9.0 | Current |
| kotlinx-datetime | 0.6.0 | Current |
| CameraX | 1.3.3 | Current |
| ML Kit Face Detection | 16.1.6 | Current |
| Coil | 2.7.0 | Current |
| Media3 ExoPlayer | 1.5.1 | Current |
| Detekt | 1.23.7 | Current |

**No obsolete dependencies detected.**

---

## 4. Android configuration

### androidApp/build.gradle.kts

| Setting | Value | Assessment |
|---|---|---|
| applicationId | com.mindos.app | NEEDS CHANGE for production |
| minSdk | 29 | OK (Android 10+) |
| targetSdk | 36 | Current |
| compileSdk | 36 | Current |
| versionCode | 1 | NEEDS increment strategy |
| versionName | "1.0" | NEEDS versioning strategy |
| isMinifyEnabled (release) | true | Good |
| isShrinkResources (release) | true | Good |

### ProGuard/R8

Rules in `androidApp/proguard-rules.pro` are overly broad:
- `-keep class androidx.compose.** { *; }` — keeps ALL Compose classes (R8 can shrink more)
- `-keep class com.google.firebase.** { *; }` — keeps ALL Firebase classes
- `-keep class kotlinx.coroutines.** { *; }` — keeps ALL coroutines

**Recommendation:** Tighten rules to only keep what's needed. Current rules prevent effective shrinking.

### Signing

No signing config defined in build.gradle.kts. The `release` build type lacks signingConfig.

**Recommendation:** Add signing config referencing keystore from environment variables (not committed).

---

## 5. iOS configuration

### CocoaPods (only on macOS)

- deploymentTarget: 15.0
- Pods: FirebaseCore, FirebaseAuth
- Framework: Shared (static)

### iOS targets

- iosArm64 (device)
- iosSimulatorArm64 (simulator)

**Gap:** No iosApp module exists. Framework is built but no consumer app.

---

## 6. Desktop configuration

### desktopApp/build.gradle.kts

- mainClass: com.mindos.app.desktop.MainKt
- Target formats: DMG, MSI, DEB
- Package version: 1.0.0

---

## 7. Build quality

| Check | Status | Notes |
|---|---|---|
| Version catalog | PASS | All versions centralized |
| Dependency duplication | PASS | No duplicates detected |
| Configuration cache | NOT ENABLED | Gradle suggests enabling |
| Build cache | DEFAULT | Not explicitly configured |
| Parallel build | COMMENTED OUT | Enable for faster builds |
| JVM args | Xmx2048m | Adequate |
| Secrets in repo | google-services.json committed | RISK (see security audit) |
| Debug/Release separation | PASS | Different App Check providers |

---

## 8. Recommendations

| Priority | Change | Risk |
|---|---|---|
| P1 | Change applicationId from com.mindos.app | LOW |
| P1 | Add version management strategy | LOW |
| P1 | Add signing configuration (env-based) | LOW |
| P2 | Tighten ProGuard rules | MEDIUM (may cause runtime issues) |
| P2 | Enable Gradle parallel builds | LOW |
| P2 | Enable configuration cache | LOW |
| P3 | Add CI jobs for Kotlin builds | LOW |
| P3 | Consider moving google-services.json to CI secrets | LOW |
