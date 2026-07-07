# IOS_RELEASE_GATE — MindBoost · Emociones (POSTPONED)

**Status: `POSTPONED — P1 for Release Candidate (multiplatform) / MVP público`.**
Owner decision (MC8u-E): iOS verification is **out of the current operational scope** because the team
has no Mac to compile, run the simulator, or test on-device. iOS is **not** a blocker for the
Android / internal-beta / local-first track. It **is** a hard blocker for any *multiplatform* release
candidate and for the Android+iOS MVP público required by the PRD.

Do **not** mark any iOS item green until it has actually been executed on macOS.

## Blocking matrix
| Level | iOS blocks? |
|---|---|
| Módulo aislado (Android/local-first) | ❌ no |
| Android internal beta | ❌ no |
| Android release candidate (if Android-only is chosen) | ❌ no |
| Release candidate **multiplatform** | ✅ **yes** |
| MVP público (PRD = Android + iOS) | ✅ **yes** |

## Prerequisite
- A macOS machine with Xcode (matching the project's Kotlin/Compose Multiplatform + AGP toolchain) and
  CocoaPods installed. Windows/Linux **cannot** satisfy this gate.

## Gate checklist (run on macOS; each must be executed, not assumed)
1. **KMP framework builds for iOS** — `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` (and device arch) green.
2. **Xcode app builds** — `cd iosApp && pod install`, then build the `iosApp` scheme for a Simulator destination.
3. **Local encrypted store works on iOS** — `SecureKeyValueStore` (Keychain-backed
   `shared/src/iosMain/.../focus/data/local/SecureKeyValueStore.ios.kt` + `IosKeychainBackend`): a written
   emotion summary persists, reads back, and survives app relaunch; `clearNamespace` erases.
4. **Monotonic clock / RT integrity** — the iOS `MonotonicClock` actual yields realistic latencies (not ~0ms),
   so the anti-gaming impossible-RT / near-chance detectors behave as on Android. Verify a live EM1/EM3 session
   produces plausible RTs and is NOT falsely flagged.
5. **Session save on iOS** — complete an EM1 and an EM3 session; confirm a valid session persists and, after
   the 5th valid session, calibration unlocks the official range (same semantics as desktop/Android).
6. **Result screen renders** — `EmotionResultScreen` and the safety UX render correctly under Compose
   Multiplatform on iOS (no broken layout, safety bar reachable).
7. **Calibration-progress surface renders** — `EmotionCalibrationProgressCard` / progress model render on iOS
   (0/5→5/5, provisional vs official, non-clinical disclaimer, not color-only).
8. **Analytics wrapper works on iOS** — the real analytics sink (see the app-wide analytics adapter) initializes
   on iOS; `emotion_*` events are emitted and PII-free; `NoOp` is rejected in a release build.
9. **Sync wrapper works on iOS** (only if cloud sync is enabled) — `EmotionCloudSyncSink` implementation runs on
   iOS: local-first save is durable, `PENDING_SYNC → SYNCED`, a failed push flips to `SYNC_FAILED` + emits
   `emotion_sync_failed`, and NEVER deletes local data.
10. **No progress loss on iOS** — kill mid-session and after terminal; on relaunch the completed/pending session
    is preserved (write-ahead pending + reconciler), and a retry does not duplicate (idempotent by `sessionId`).
11. **Exposure guard on iOS** — EM3/EM10 recent-exposure persistence round-trips on Keychain-backed storage.

## Criteria to mark iOS GREEN
All 11 items above executed on macOS with evidence attached (build logs + a short screen recording or
screenshots of: a persisted-and-reopened session, the result screen, the progress surface), AND the
Android gates already green (desktopTest + detekt + scanners + secrets). Only then may a **multiplatform**
release candidate be declared.

## Current evidence
- Android/desktop: green (see the MC8u-D/MC8u-E evidence bundles).
- iOS: **not executed — no Mac available.** Classified `POSTPONED — P1 RC(multiplatform)/MVP`.
