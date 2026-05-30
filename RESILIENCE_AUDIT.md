# RESILIENCE_AUDIT.md

**Date:** 2026-05-27

---

## 1. Error handling matrix

| Scenario | Handled? | How | Parity |
|---|---|---|---|
| No internet | PARTIAL | Firebase SDK returns error, caught in try/catch | Shared |
| Slow internet | NO | No timeout UI or retry | Gap |
| Timeout | NO | Firebase SDK timeout, no user notification | Gap |
| Auth error (401) | YES | AuthDiagnostics maps to user message | Shared |
| Permission denied (403) | PARTIAL | DomainError.ActionNotAuthorized exists | Shared |
| Not found (404) | YES | DomainError hierarchy handles | Shared |
| Rate limit (429) | NO | No specific handling | Gap |
| Server error (500) | NO | Generic catch | Gap |
| Firebase unavailable | PARTIAL | Offline cache may serve stale data | Shared |
| Session expired | YES | authState Flow redirects to Auth | Shared |
| Token invalid | YES | Auth redirect | Shared |
| Permission denied (OS) | PARTIAL | Camera/mic permissions declared | Platform |
| Empty data | PARTIAL | Some screens show empty, not standardized | Gap |
| Corrupt data | NO | No defensive parsing | Gap |

---

## 2. Resilience patterns in codebase

### Present

- `runCatching` + `fold` in auth flow
- `try/catch` in Firebase operations
- Firebase offline persistence (Firestore)
- Auth state monitoring via Flow
- DomainError sealed hierarchy

### Missing

- Standardized error display (now available via StateComponents — not yet adopted)
- Retry with backoff
- Offline detection and notification
- Pull-to-refresh error recovery
- Graceful degradation (show cached data with warning)
- Loading timeout with fallback

---

## 3. Edge cases

| Edge Case | Handled? | Notes |
|---|---|---|
| App killed and restarted | PARTIAL | Auth state persisted by Firebase; nav stack lost |
| Background -> foreground | YES | Compose lifecycle handles |
| Screen rotation | YES | Compose recomposition handles |
| Back press from root | YES | Sealed route prevents invalid state |
| Sign out during operation | YES | authState Flow triggers redirect |
| Concurrent Firebase writes | PARTIAL | Firestore handles at SDK level |

---

## 4. Fixes applied

| Fix | Impact |
|---|---|
| ErrorMapper created | Standardized exception-to-user-message mapping |
| UiState with Error/Offline states | Standardized error state model |
| ErrorScreen/OfflineScreen components | Ready-to-use error rendering |

---

## 5. Recommendations

| Priority | Action |
|---|---|
| P1 | Adopt ErrorScreen in all screens' catch blocks |
| P1 | Add loading timeout (show error after 15s) |
| P2 | Add retry button to all error states |
| P2 | Add offline detection abstraction |
| P3 | Add pull-to-refresh where applicable |
| P3 | Add data validation/defensive parsing for Firestore documents |
