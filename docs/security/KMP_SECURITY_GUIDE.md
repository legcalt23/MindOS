# KMP Client Security Guide

**Date:** 2026-05-26

---

## 1. Architecture Overview

The KMP shared module (`shared/src/commonMain/kotlin/com/example/mindos/`) follows a layered security architecture:

```
UI Layer (Screens, StateHolders)
    ↓ calls
Domain Layer (Use cases, Policy engine, Models)
    ↓ calls
Data Layer (Repositories, DTOs, Mappers)
    ↓ calls
Firebase SDK (Firestore, Storage, Auth)
```

**Security principle:** UI never writes directly to Firestore or Storage. All Firebase operations go through typed repositories with validation.

## 2. Input Validation — SafeInput.kt

All user-provided text is validated and normalized before any Firebase operation.

| Function | Purpose | Rules |
|---|---|---|
| `isSafeDocumentId()` | Firestore doc ID validation | `^[A-Za-z0-9_-]{3,128}$` |
| `normalizeUsername()` | Username normalization | Lowercase, sanitize, trim to 30 chars |
| `normalizeSingleLineHumanText()` | Display text | Strip control chars, trim, length limit |
| `normalizeMultilineHumanText()` | Body text | Allow newlines, strip other control chars |
| `normalizeSearchQuery()` | Search input | Sanitize for Firestore range queries |

**Blocked:** Control characters (0x00-0x1F, 0x7F) except newline/tab when explicitly allowed.

## 3. Logging — SafeDebugLogging.kt

| Rule | Implementation |
|---|---|
| Debug-only logging | `safeDebugLog()` only emits in debug builds (platform `expect/actual`) |
| No passwords in logs | `passwordPresenceForLog()` returns `"password_present=true/false"` only |
| No tokens in logs | Auth tokens never logged; only exception class names |
| No PII in error logs | Exception messages sanitized to class name only |
| Production safety | Release builds suppress debug logging entirely |

## 4. Session Management — SessionRepository

- `AuthenticatedUser` data class: `uid`, `email`, `displayName`, `isEmailVerified`
- Password minimum: 8 characters (validated before Firebase call)
- Email normalization: `.trim()` applied
- Error mapping: Firebase errors → user-friendly Spanish messages
- No token caching or manual token management

## 5. Repository Security Patterns

### Ownership enforcement
Every repository validates `auth.uid` matches the target user:
```kotlin
// Pattern used in all repositories
val currentUid = auth.currentUser?.uid ?: throw NotAuthenticated
if (currentUid != targetUserId) throw PermissionDenied
```

### Typed DTOs (not generic maps)
- `DMChatDto`, `DMMessageDto` — serializable with known fields
- `ProfileVisualPersistenceMapper` — explicit field mapping
- No `Map<String, Any>` writes from UI

### Transactional operations
- Chat creation: `runTransaction { check existence, create if absent }`
- Observation: atomic batch (observation + counter increment + notification)
- Profile save: transactional with privacy doc coordination

### Path builders
- Storage paths built from typed enums (`ProfileModuleId.storageFolder`)
- Firestore paths built from repository constants, never from UI strings

## 6. Prohibited Patterns

| Pattern | Status | Enforcement |
|---|---|---|
| `Map<String, Any>` writes from UI | NOT FOUND | Typed DTOs used |
| Manual Firestore paths in ViewModels | NOT FOUND | Path builders in repos |
| Manual Storage paths in ViewModels | NOT FOUND | Path builders in repos |
| Direct Firestore writes from screens | NOT FOUND | Repository layer |
| Global collection reads | NOT FOUND | Bounded queries |
| Unbounded queries | NOT FOUND | Limits in queries |
| Token logging | NOT FOUND | SafeDebugLogging |
| PII logging | NOT FOUND | Sanitized logging |
| Internal error display to user | NOT FOUND | Error mapping |
| Client-side permission decisions | NOT FOUND | Policy engine reads server state |
| Client-side role/flag writes | NOT FOUND | No such fields in DTOs |

## 7. Media Upload Validation

Before uploading to Firebase Storage, the client validates:
1. File MIME type against allowlist
2. File size against per-type limits
3. File name safety (no path traversal characters)
4. Generated safe ID for storage path

Size limits defined in `ProfileVisualModels.kt`:
- Images: 10 MB
- Videos: 75 MB
- Audio: 25 MB
- Thumbnails: 1 MB
- Cover images: 15 MB

## 8. Error Handling

- Domain errors mapped to user-friendly messages
- Firebase permission errors → generic "permission denied"
- Network errors → "connection unavailable"
- App Check failures → specific error type
- Internal state never exposed to user

## 9. Recommendations

1. **Align username search limit constant** — KMP has `UsernameSearchMaxResults = 50` but Firestore rules enforce `limit <= 12`. Change KMP constant to 12.
2. **Add client-side rate limiting** — complement server-side limits with UI-level throttling for UX (prevent rapid taps from hitting rate limiter).
3. **Certificate pinning** — consider for production builds to prevent MITM.
4. **Root/jailbreak detection** — consider for high-security scenarios.
