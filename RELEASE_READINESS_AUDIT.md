# RELEASE_READINESS_AUDIT.md

**Date:** 2026-05-27

---

## 1. Build status

| Build | Status | Notes |
|---|---|---|
| Android debug | PASSES | Verified via desktopTest compilation |
| Android release | CONFIGURED | R8/ProGuard enabled, needs signing |
| iOS simulator | FRAMEWORK ONLY | No iosApp module |
| iOS device | FRAMEWORK ONLY | No iosApp module, needs macOS |
| Desktop | PASSES | Main.kt entry point works |

---

## 2. Release configuration

| Item | Status | Action needed |
|---|---|---|
| applicationId | `com.example.mindos` | Change to production ID |
| versionCode | 1 | Implement auto-increment |
| versionName | "1.0" | Implement semver strategy |
| Signing (Android) | NOT CONFIGURED | Add keystore + signing config |
| Signing (iOS) | NOT CONFIGURED | No Xcode project |
| ProGuard/R8 | CONFIGURED (broad rules) | Tighten rules |
| App Check (Android) | CONFIGURED | Play Integrity (release), Debug (debug) |
| App Check (iOS) | NOT CONFIGURED | Needs iOS app |
| Firebase config (Android) | google-services.json | In repo (security risk) |
| Firebase config (iOS) | GoogleService-Info.plist | In iosMain/resources |
| Crashlytics | NOT CONFIGURED | Add dependency |
| Analytics | NOT CONFIGURED | Add dependency |

---

## 3. Store readiness

### Google Play Store

| Requirement | Status |
|---|---|
| Unique applicationId | NOT MET (com.example.*) |
| Signed release APK/AAB | NOT MET (no signing) |
| Target API level | MET (36) |
| 64-bit support | MET (default) |
| Privacy policy | NOT ASSESSED |
| Store listing | NOT PREPARED |

### Apple App Store

| Requirement | Status |
|---|---|
| iOS app target | NOT MET (no iosApp) |
| Xcode project | NOT MET |
| Signing/provisioning | NOT MET |
| App Store Connect | NOT PREPARED |

---

## 4. Pre-release checklist

- [ ] Change applicationId
- [ ] Configure signing
- [ ] Set up version management
- [ ] Create iosApp module
- [ ] Configure Crashlytics
- [ ] Configure Analytics
- [ ] Tighten ProGuard rules
- [ ] Remove debug endpoints/flags
- [ ] Test release build on device
- [ ] Prepare store listings
- [ ] Privacy policy
- [ ] Terms of service
