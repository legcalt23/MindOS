# FUNCTIONAL_PARITY_REPORT.md

> ⚠️ **OBSOLETO PARA iOS (2026-05-29).** Decisión de alcance: **Android-first, iOS aplazado.**
> Este report afirma paridad iOS que **no existe en runtime** (iOS usa sesión/feed/privacidad
> falsos y no compila en CI). Fuente de verdad: [`IOS_STATUS.md`](IOS_STATUS.md).

**Date:** 2026-05-27

---

## 1. Feature parity matrix

| Feature | Android | iOS | Desktop | Parity |
|---|---|---|---|---|
| Auth (email/password) | FULL | FULL (shared) | FULL (shared) | 100% |
| Element Selection | FULL | FULL (shared) | FULL (shared) | 100% |
| Home / Hub | FULL | FULL (shared) | FULL (shared) | 100% |
| Profile view/edit | FULL | FULL (shared) | FULL (shared) | 100% |
| Social Graph (follow/block) | FULL | FULL (shared) | FULL (shared) | 100% |
| Publishing | FULL | FULL (shared) | FULL (shared) | 100% |
| Global Feed | FULL | FULL (shared) | FULL (shared) | 100% |
| DM / Chat | FULL | FULL (shared) | FULL (shared) | 100% |
| DM Audio Recording | FULL | ACTUAL | ACTUAL | ~95% |
| Waves | FULL | FULL (shared) | FULL (shared) | 100% |
| Search | FULL | FULL (shared) | FULL (shared) | 100% |
| Privacy overlay | ACTUAL | ACTUAL | ACTUAL | ~95% |
| Media Viewer | ACTUAL | ACTUAL | ACTUAL | ~90% |
| File Picking | ACTUAL | ACTUAL | ACTUAL | ~90% |
| Camera (face detection) | Android only | N/A | N/A | 0% |
| Wallet | FULL | FULL (shared) | FULL (shared) | 100% |
| Notifications | FULL | FULL (shared) | FULL (shared) | 100% |
| Settings | FULL | FULL (shared) | FULL (shared) | 100% |

**Legend:**
- FULL = 100% shared Compose code
- ACTUAL = expect/actual with platform implementation
- Android only = dependency only in androidMain

### Camera note

CameraX + ML Kit Face Detection are Android-only dependencies. There is no iOS camera implementation. This is a known gap — the camera feature would need a native iOS implementation or a KMP camera library.

---

## 2. Validation parity

All validation lives in commonMain:

| Validator | Location | Tests |
|---|---|---|
| SafeInput (IDs, usernames, text) | `core/security/SafeInput.kt` | SafeInputTest (commonTest) |
| Handle validation | `domain/valueobject/SocialValueObjects.kt` | via SocialGraphRulesTest |
| DisplayName validation | `domain/valueobject/SocialValueObjects.kt` | via SocialGraphRulesTest |
| Bio validation | `domain/valueobject/SocialValueObjects.kt` | via SocialGraphRulesTest |
| UserId validation | `domain/valueobject/SocialValueObjects.kt` | via SocialGraphRulesTest |
| Auth input validation | `core/session/FirebaseSessionRepository.kt` | via AuthDiagnosticsTest |
| DM message validation | `dm/domain/usecase/SendDMMessageUseCase.kt` | SendDMMessageUseCaseTest |
| Feed input validation | `social/domain/feed/` | GlobalFeedInputValidationTest |

**Result: 100% validation parity** — all validators are platform-independent.

---

## 3. Error handling parity

### Auth errors

Mapped via `AuthDiagnostics.kt` (shared) to user-facing messages. Test coverage: `AuthDiagnosticsTest`.

### Domain errors

`DomainError` sealed hierarchy in commonMain provides structured error codes.

### General errors

New `ErrorMapper` (created this session) provides standardized mapping from exceptions to `UserFacingError`.

**Gap:** Screens don't consistently use ErrorMapper yet. Many catch blocks show raw error messages or generic text.

---

## 4. State model parity

### Current state

State is managed via `mutableStateOf` in state holders. No standardized state model.

### New state model

`UiState<T>` sealed interface created (this session) with: Idle, Loading, Success, Empty, Error, Offline, PermissionDenied, Unauthorized, Refreshing, Submitting.

**Gap:** Existing screens don't use `UiState<T>` yet. Migration recommended.

---

## 5. Navigation parity

All navigation defined in `App.kt` via `AppRoute` sealed interface. 100% shared.

---

## 6. Offline parity

Firebase Firestore SDK provides built-in offline persistence on both Android and iOS. The behavior is transparent to the app code.

**Gap:** No explicit offline detection or user notification. No "offline mode" UI.

---

## 7. Fixes aplicados

| Fix | Files |
|---|---|
| UiState model created | `core/ui/UiState.kt` |
| ErrorMapper created | `core/ui/UserFacingError.kt` |
| State components created | `design/components/StateComponents.kt` |
| Tests for UiState | `core/ui/UiStateTest.kt` |
| Tests for ErrorMapper | `core/ui/ErrorMapperTest.kt` |
