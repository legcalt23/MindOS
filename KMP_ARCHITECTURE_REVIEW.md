# KMP_ARCHITECTURE_REVIEW.md

**Date:** 2026-05-27

---

## 1. Estado actual

### Modulo shared — source set distribution

| Source Set | Files | Purpose |
|---|---|---|
| commonMain | 182 | Domain, data, presentation, UI (Compose Multiplatform) |
| androidMain | 21 | Platform implementations (camera, audio, media, logging) |
| iosMain | 18 | Platform implementations (mirror of androidMain) |
| desktopMain | 20 | Platform implementations (mirror of androidMain) |
| commonTest | 20 | Unit tests |
| desktopTest | 5 | UI smoke tests (Compose Desktop) |

### Architecture pattern

- **Clean Architecture**: domain (models, value objects, policies, use cases) / data (repositories, mappers) / presentation (state holders, Compose screens)
- **Feature-based packaging**: auth, dm, element_selection, home, intro, privacy, search, social, wallet, waves
- **Navigation**: Custom `AppRoute` sealed interface with manual state-based routing
- **DI**: Manual constructor injection via `remember {}` — no DI framework
- **State**: `mutableStateOf` + `rememberCoroutineScope` (no external state library)

### Shared code ratio: ~90%

Almost all logic lives in commonMain. Platform-specific code handles:
- Firebase Storage `Data` wrapper
- Audio recording/playback
- File picking
- Media viewing (image/video rendering)
- Debug logging
- Privacy overlay
- Media preloading

---

## 2. Estado objetivo

### Target: 95%+ shared code

The remaining platform-specific code is justified:
- Audio/Video playback requires platform APIs (ExoPlayer on Android, AVFoundation on iOS)
- Camera access requires platform APIs
- File picking requires platform UI
- Debug logging needs platform log APIs

### Ideal improvements

1. Shared `UiState<T>` model (DONE - created `core.ui.UiState`)
2. Shared `ErrorMapper` (DONE - created `core.ui.ErrorMapper`)
3. Shared design tokens (DONE - created `design.AppColors`, `AppSpacing`, `AppShapes`, `AppElevation`, `AppAnimation`)
4. Shared string constants (DONE - created `design.AppStrings`)
5. Shared state components (DONE - created `design.components.StateComponents`)

---

## 3. expect/actual inventory

### 18 expect declarations, all with Android/iOS/Desktop actuals

| Declaration | Android | iOS | Desktop | Category |
|---|---|---|---|---|
| `isPlatformDebugLoggingEnabled()` | Y | Y | Y | Logging |
| `platformDebugLog()` | Y | Y | Y | Logging |
| `platformErrorLog()` | Y | Y | Y | Logging |
| `rememberPlatformFilePicker()` | Y | Y | Y | Media |
| `dmStorageData()` | Y | Y | Y | Firebase |
| `DMAudioRecorder` | Y | Y | Y | Audio |
| `PrivacyScreenOverlay` | Y | Y | Y | Privacy |
| `rememberPrivacyEnabled()` | Y | Y | Y | Privacy |
| `rememberGlobalFeedRepository()` | Y | Y | Y | Feed |
| `ProfileRemoteWriteAccessGuard` | Y | Y | Y | Profile |
| `ProfileVisualStorageData` | Y | Y | Y | Storage |
| `GlobalFeedTime` | Y | Y | Y | Time |
| `FeedAttachmentPicker` | Y | Y | Y | Media |
| `FeedAudioPlayer` | Y | Y | Y | Audio |
| `AppMediaViewer` | Y | Y | Y | Media |
| `MediaPreloadManager` | Y | Y | Y | Media |
| `CoverClubArtwork` | Y | Y | Y | Media |
| `ProfileMediaPlatform` | Y | Y | Y | Media |
| `PublicationAddArtwork` | Y | Y | Y | Media |
| `PublisherPlatformStore` | Y | Y | Y | Storage |

### Android-only files (no iOS counterpart)

| File | Risk | Action |
|---|---|---|
| `AndroidRawImageLoader.kt` | MEDIUM | Android utility, used internally. iOS doesn't need equivalent (Skiko handles rendering). |
| `DMListScreenIcon.android.kt` | LOW | Has desktop equivalent. iOS has icon rendering via Compose. |
| `WaveMailboxIcon.android.kt` | LOW | Has desktop equivalent. iOS has icon rendering via Compose. |

**Note:** These files are NOT expect/actual pairs — they are additional Android helpers. The actual expect/actual pairs are complete across all platforms.

---

## 4. Refactors aplicados (this session)

| Refactor | File(s) | Purpose |
|---|---|---|
| Centralized color tokens | `design/AppColors.kt` | Replace 275 scattered Color() literals |
| Centralized spacing tokens | `design/AppSpacing.kt` | Replace 624+ scattered dp literals |
| Centralized shape tokens | `design/AppShapes.kt` | Standardize corner radii |
| Centralized elevation tokens | `design/AppElevation.kt` | Standardize shadows |
| Centralized animation tokens | `design/AppAnimation.kt` | Standardize durations |
| Centralized string constants | `design/AppStrings.kt` | Replace hardcoded UI strings |
| Shared UiState model | `core/ui/UiState.kt` | Standardize screen states |
| Shared error mapper | `core/ui/UserFacingError.kt` | Standardize error messages |
| Shared state components | `design/components/StateComponents.kt` | Loading/Error/Empty/Offline |
| ErrorMapper tests | `core/ui/ErrorMapperTest.kt` | 10 test cases |
| UiState tests | `core/ui/UiStateTest.kt` | 10 test cases |

---

## 5. Refactors recomendados (futuro)

| Priority | Refactor | Effort | Impact |
|---|---|---|---|
| P1 | Replace hardcoded Color() literals with AppColors references | HIGH | Visual parity |
| P1 | Replace hardcoded dp literals with AppSpacing references | HIGH | Visual parity |
| P1 | Replace hardcoded strings with AppStrings references | HIGH | Localization readiness |
| P2 | Create iosApp module with SwiftUI entry point | HIGH | iOS runnable app |
| P2 | Add CI jobs for Kotlin/Android builds and tests | MEDIUM | Regression prevention |
| P3 | Introduce DI framework (Koin KMP) for testability | MEDIUM | Testing |
| P3 | Add navigation library (Voyager/Decompose) for type-safe nav | MEDIUM | Navigation parity |
| P4 | Add light theme support | MEDIUM | User preference |
| P4 | Full i18n with compose-resources | HIGH | Localization |

---

## 6. Diferencias inevitables

| Difference | Platform | Reason | Mitigation |
|---|---|---|---|
| Audio codec/format | Android vs iOS | Platform API constraints | Abstract via expect/actual |
| Camera API | Android vs iOS | CameraX vs AVCaptureSession | Abstract via expect/actual |
| File picker UI | Android vs iOS | Native system picker | Accept native appearance |
| Video playback | Android vs iOS | ExoPlayer vs AVPlayer | Abstract via expect/actual |
| Scroll physics | Android vs iOS | Overscroll glow vs bounce | Compose Multiplatform handles |
| System chrome | Android vs iOS | Material vs Cupertino | Accept native appearance |

---

## 7. Riesgos residuales

| Risk | Severity | Mitigation |
|---|---|---|
| No iosApp module exists | HIGH | Cannot test iOS parity without macOS + Xcode |
| Detekt baseline has 2961 findings | MEDIUM | Baseline prevents new issues; gradual cleanup |
| Manual DI limits testability | MEDIUM | State holders testable; repository injection manual |
| No navigation type safety | LOW | AppRoute sealed interface provides compile-time safety |
