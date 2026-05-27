# PARITY_AUDIT_INITIAL.md

**Date:** 2026-05-27
**Auditor:** Elite KMP Parity Team (automated)
**Scope:** Full iOS/Android parity assessment of MindOS

---

## 1. Resumen ejecutivo

MindOS is a Kotlin Multiplatform social app targeting Android, iOS, and Desktop (JVM). The codebase demonstrates a **strong shared-first architecture** with 182 common Kotlin files, 21 Android-specific, 18 iOS-specific, and 20 desktop-specific files. The majority of business logic, domain models, UI (Compose Multiplatform), and navigation already live in `commonMain`.

**Key strengths:**
- Single `shared` module contains >90% of code in `commonMain`
- Compose Multiplatform UI renders identically on all platforms
- Domain models, value objects, and validation live in shared code
- Firebase integration via `dev.gitlive` KMP SDK (cross-platform)
- `expect/actual` pattern used correctly for platform-specific implementations
- Detekt static analysis configured with baseline (2961 existing findings)
- CI pipeline exists for backend (functions), but **no Kotlin/Android/iOS CI jobs**
- Comprehensive security documentation already in place

**Critical findings:**
1. **No `iosApp` module exists** — iOS targets are declared in `shared/build.gradle.kts` but there is no standalone iOS application project (no Xcode project, no SwiftUI entry point)
2. **No Kotlin/Android CI** — CI only covers Cloud Functions (Node.js), not the KMP app
3. **3 expect/actual implementations missing on iOS** — `DMListScreenIcon`, `AndroidRawImageLoader`, `WaveMailboxIcon` exist on Android/Desktop but not on iOS
4. **Design system exists but is minimal** — Only dark color scheme and basic typography; no spacing tokens, radius tokens, elevation tokens, component library
5. **Hardcoded colors scattered across UI** — Many `Color(0xFF...)` literals outside the theme
6. **No formal error mapping layer** — DomainError exists but no UI-facing error message centralization
7. **No light mode support** — Only `darkColorScheme` defined
8. **Strings are hardcoded in Compose UI** — No shared string resource system

---

## 2. Arquitectura actual

### Stack tecnologico

| Component | Version |
|---|---|
| Kotlin | 2.3.10 |
| Gradle | 9.3.1 |
| Android Gradle Plugin | 9.1.0 |
| Compose Multiplatform | 1.10.3 |
| Material 3 | 1.9.0 |
| Firebase KMP SDK (dev.gitlive) | 2.4.0 |
| Firebase Android BOM | 33.15.0 |
| Min SDK Android | 29 |
| Target SDK Android | 36 |
| Compile SDK Android | 36 |
| iOS Deployment Target | 15.0 |
| JVM Target | 17 |
| kotlinx-coroutines | 1.10.2 |
| kotlinx-serialization | 1.9.0 |
| kotlinx-datetime | 0.6.0 |
| Detekt | 1.23.7 |

### Targets configurados

- **Android** (androidApp module) -- Fully functional
- **iOS** (iosArm64, iosSimulatorArm64) -- Framework builds declared, CocoaPods on macOS, **NO iosApp**
- **Desktop/JVM** (desktopApp module) -- Functional via Compose Desktop

### Arquitectura de modulos

```
MindOS/
  settings.gradle.kts      -- includes :shared, :androidApp, :desktopApp
  shared/                   -- KMP library module
    src/commonMain/         -- 182 files (domain, data, presentation, UI)
    src/androidMain/        -- 21 files (platform implementations)
    src/iosMain/            -- 18 files (platform implementations)
    src/desktopMain/        -- 20 files (platform implementations)
    src/commonTest/         -- 20 test files
    src/desktopTest/        -- 5 UI smoke test files
  androidApp/               -- Android application module (5 files)
  desktopApp/               -- Desktop application module (1 file)
  functions/                -- Firebase Cloud Functions (Node.js backend)
  docs/                     -- Security documentation
```

### Patron arquitectonico

- **Feature-based packaging** dentro de `shared/src/commonMain`:
  - `core/` -- firebase, logging, media, navigation, security, session, ui
  - `features/auth/` -- Authentication UI
  - `features/dm/` -- Direct messages (data/domain/presentation)
  - `features/element_selection/` -- Element selection flow (domain/presentation/ui)
  - `features/home/` -- Home screen
  - `features/intro/` -- Intro/onboarding flow
  - `features/privacy/` -- Privacy overlay and settings
  - `features/search/` -- Username search
  - `features/social/` -- Social graph, profiles, publishing, feed, media, notifications
  - `features/wallet/` -- Virtual currency
  - `features/waves/` -- Wave requests (DM invitation system)
- **Clean Architecture layers**: domain (models, value objects, policies, use cases, repositories interfaces) / data (Firebase implementations, mappers, in-memory stores) / presentation (state holders, view models, Compose screens)
- **State management**: Compose `mutableStateOf` + `rememberCoroutineScope` (no external state management library)
- **Navigation**: Custom `AppRoute` sealed interface with manual routing in `MindOSApp` composable
- **DI**: Manual constructor injection via `remember {}` blocks (no DI framework)

---

## 3. Mapa de modulos y features

### Features identificadas

| Feature | Domain | Data | Presentation | Screens |
|---|---|---|---|---|
| Auth | SessionRepository | FirebaseSessionRepository | AuthScreen | Login/Register |
| Element Selection | ElementMaster, ElementType | Catalog | StateHolder | ElementSelectionScreen |
| Home | -- | -- | HomeScreen | Hub, tabs |
| Profile | UserModels, ProfileVisual, Policies | Firebase repos, InMemory | StateHolder, Screen | ProfileScreen, ProfileEntry, ProfileEdit, ProfileSettings, ProfileCover |
| Social Graph | SocialRelations, Follow, Block | FirebaseSocialInteraction, InMemory | StateHolder, Screen | SocialGraphScreen, FollowRequests |
| Publishing | PublishingModels, Commands, Events | InMemoryPublishing | StateHolder | PublisherScreen, Composer |
| Global Feed | GlobalFeedModels | GlobalFeedRepository | StateHolder, Screen | GlobalFeedScreen |
| DM / Chat | DMChat, DMMessage | FirebaseDMRepository | StateHolder, Screen | DMList, DMChat, DMChatUserInfo |
| Waves | WaveRequest, WaveRelationship | InMemoryWaveRequest | ViewModel | WaveMailbox |
| Cajonera | -- | -- | Screen | CajoneraScreen |
| Search | UsernameSearchModels | FirebaseUsernameSearch | ViewModel, Screen | UsernameSearchScreen |
| Privacy | PrivacyModels | -- | SettingsScreen, Overlay | SettingsScreen |
| Media | AppMediaContent | -- | MediaViewer, Thumbnails | AppMediaViewer |
| Wallet | WalletModels | WalletApiClient (Firebase) | StateHolder, Screen | MoneyScreen |
| Notifications | InAppNotification | -- | StateHolder, Screen | NotificationsScreen |
| Intro | IntroConfig, Controller | -- | Screen | MindOSIntroScreen |

---

## 4. Estado de paridad iOS/Android

### Paridad de codigo compartido: ALTA

| Layer | commonMain | Android-specific | iOS-specific | Desktop-specific |
|---|---|---|---|---|
| Files | 182 | 21 | 18 | 20 |
| Percentage | ~69% | ~8% | ~7% | ~8% |

### expect/actual implementations

18 expect declarations found. Each has actual implementations on Android, iOS, and Desktop:

| expect | Android | iOS | Desktop | Notes |
|---|---|---|---|---|
| SafeDebugLogging | Y | Y | Y | Platform logging |
| PlatformFilePicker | Y | Y | Y | File picking |
| DMStorageData | Y | Y | Y | Firebase Storage Data wrapper |
| DMAudioRecorder | Y | Y | Y | Audio recording |
| PrivacyScreenOverlay | Y | Y | Y | Screen privacy |
| PrivacySettings (rememberPrivacyEnabled) | Y | Y | Y | Privacy state |
| GlobalFeedRepositoryProvider | Y | Y | Y | Feed repo factory |
| ProfileRemoteWriteAccessGuard | Y | Y | Y | Write guard |
| ProfileVisualStorageData | Y | Y | Y | Storage data |
| GlobalFeedTime | Y | Y | Y | Time abstraction |
| FeedAttachmentPicker | Y | Y | Y | Attachment picking |
| FeedAudioPlayer | Y | Y | Y | Audio playback |
| AppMediaViewer | Y | Y | Y | Media viewer |
| MediaPreloadManager | Y | Y | Y | Media preloading |
| CoverClubArtwork | Y | Y | Y | Cover art |
| ProfileMediaPlatform | Y | Y | Y | Profile media |
| PublicationAddArtwork | Y | Y | Y | Publication art |
| PublisherPlatformStore | Y | Y | Y | Draft persistence |

### Archivos que existen solo en Android (sin counterpart iOS)

| File | Risk |
|---|---|
| `AndroidRawImageLoader.kt` | MEDIUM -- Android-only image loading utility |
| `DMListScreenIcon.android.kt` | LOW -- Has desktop equivalent, missing iOS |
| `WaveMailboxIcon.android.kt` | LOW -- Has desktop equivalent, missing iOS |

---

## 5. Diferencias detectadas

### 5.1 Criticas

| # | Diferencia | Impacto | Plataforma |
|---|---|---|---|
| C1 | **No existe iosApp module** | iOS builds a framework but has no runnable application | iOS |
| C2 | **No CI para Kotlin/Android/iOS builds** | No automated verification of compilacion or tests | Both |
| C3 | **3 expect/actual missing on iOS** | DMListScreenIcon, AndroidRawImageLoader, WaveMailboxIcon | iOS |

### 5.2 Altas

| # | Diferencia | Impacto |
|---|---|---|
| H1 | No light mode -- only darkColorScheme | Visual parity incomplete for users who prefer light |
| H2 | No shared string resources -- all UI strings hardcoded | Localization impossible, consistency not guaranteed |
| H3 | Hardcoded colors outside theme (Color(0xFF...) literals in screens) | Visual parity risk |
| H4 | No spacing/radius/elevation tokens | Visual consistency depends on manual discipline |
| H5 | No formal error-to-user-message mapping beyond auth | Error messages may differ across flows |
| H6 | Navigation is state-based with no animation/transition system | UX parity risk on transitions |

### 5.3 Medias

| # | Diferencia | Impacto |
|---|---|---|
| M1 | No accessibility semantics audited | Screen reader behavior may differ |
| M2 | No offline state handling documented | Resilience parity unknown |
| M3 | Manual DI via remember{} -- no testable injection graph | Testing parity limited |
| M4 | ProGuard rules overly broad (-keep class ... { *; }) | Release binary may be larger than needed |
| M5 | No versioning strategy (versionCode=1, versionName="1.0") | Release readiness gap |
| M6 | Detekt baseline has 2961 findings | Code quality debt |

### 5.4 Bajas

| # | Diferencia | Impacto |
|---|---|---|
| L1 | Android theme.xml colorAccent (#C8B273) differs from Compose primary (#13ECA4) | Splash/system dialog color mismatch |
| L2 | Desktop entry uses PreviewSessionRepository (not real Firebase) | Desktop parity limited |
| L3 | media3-exoplayer/CameraX only in androidMain | Android-only features expected |
| L4 | CocoaPods only applied on macOS hosts | Expected behavior |

---

## 6. Riesgos criticos

| Risk | Severity | Description |
|---|---|---|
| **No iOS app** | CRITICAL | Cannot verify iOS parity without a runnable iOS application |
| **No KMP CI** | CRITICAL | Regressions in shared code or Android build go undetected |
| **Hardcoded UI values** | HIGH | Changes require grep-and-replace instead of token update |
| **No string centralization** | HIGH | Localization blocked, text parity unverifiable |
| **Missing expect/actuals on iOS** | HIGH | iOS framework compilation may fail for those declarations |

---

## 7. Oportunidades de refactorizacion

1. **Create design system tokens** (colors, spacing, radius, elevation, animation) in `shared/commonMain`
2. **Extract shared string constants** as a baseline before full i18n
3. **Create error mapping layer** from DomainError to user-facing messages
4. **Add common UiState sealed class** (Loading/Success/Error/Empty/Offline)
5. **Add common NavigationEvent** one-shot effect pattern
6. **Centralize component library** (buttons, cards, text fields, dialogs, etc.)
7. **Add CI jobs** for `./gradlew :shared:allTests` and Android debug build
8. **Create iosApp module** with SwiftUI entry point calling shared Compose framework
9. **Add light theme** support
10. **Add baseline accessibility semantics** to all interactive components

---

## 8. Supuestos tecnicos

1. The iOS framework is intended to be consumed by a native iOS app (SwiftUI or UIKit) that does not yet exist
2. The app is dark-mode-only by design intent (not an oversight)
3. Desktop target is secondary; primary targets are Android and iOS
4. Firebase is the sole backend (Firestore, Auth, Storage, Functions)
5. The app is pre-production (versionCode=1, no store listing)
6. All features in commonMain are expected to work identically on all platforms
7. InMemory repositories (social graph, waves, publishing) are development placeholders that will be replaced by Firebase implementations

---

## 9. Plan de actuacion recomendado

### Fase inmediata (blockers)

1. **Verify iOS framework compilation** on macOS or document limitation on Windows
2. **Add missing expect/actual iOS implementations** (DMListScreenIcon, WaveMailboxIcon)
3. **Add CI jobs** for shared tests and Android build

### Fase 1: Design system (1-2 days)

4. Create `DesignTokens.kt` with spacing, radius, elevation, animation constants
5. Create `AppColors.kt` with semantic color tokens beyond Material3 defaults
6. Audit and replace hardcoded Color() literals
7. Create basic component library (buttons, cards, text fields)

### Fase 2: Normalization (1-2 days)

8. Create `UiState<T>` sealed class in commonMain
9. Create `UserFacingError` mapper from DomainError
10. Extract hardcoded strings to constants object
11. Standardize loading/error/empty state rendering

### Fase 3: Quality (1-2 days)

12. Add accessibility semantics to interactive components
13. Add unit tests for error mapping
14. Add UI smoke tests for critical flows
15. Add performance guards (stable parameters, lazy lists audit)

### Fase 4: Release readiness (1 day)

16. Configure signing (or document)
17. Version strategy
18. Release checklist
19. Store preparation documentation
