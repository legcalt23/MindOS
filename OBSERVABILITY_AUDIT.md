# OBSERVABILITY_AUDIT.md

**Date:** 2026-05-27

---

## 1. Current state

| Capability | Status | Implementation |
|---|---|---|
| Logging | YES | `SafeDebugLogging` expect/actual (debug only) |
| Error logging | YES | `safeErrorLog()` in commonMain |
| Crash reporting | NO | No Crashlytics or equivalent |
| Analytics | NO | No analytics SDK |
| Performance monitoring | NO | No Firebase Performance or equivalent |
| Screen tracking | NO | No automatic screen tracking |
| Event tracking | PARTIAL | AuditEvent model exists in domain |
| Breadcrumbs | NO | -- |

---

## 2. Logging analysis

### SafeDebugLogging

- `isPlatformDebugLoggingEnabled()` — returns false in release (Android checks debuggable flag)
- `platformDebugLog(tag, message)` — Android: Log.d, iOS: print, Desktop: println
- `safeDebugLog()` — guarded wrapper, only logs in debug
- `safeErrorLog()` — always logs errors

### PII safety

- `passwordPresenceForLog()` — logs presence, not value (good)
- Auth diagnostic messages may contain email addresses in log output
- Firebase operation logs may contain UIDs

**Risk:** Email addresses logged in auth error diagnostics. UIDs in various debug logs.

---

## 3. Recommendations

| Priority | Action |
|---|---|
| P1 | Add Crashlytics (Firebase) for crash reporting |
| P1 | Ensure no PII (emails) in production error logs |
| P2 | Add Analytics for screen tracking and key events |
| P2 | Add breadcrumb trail for crash diagnosis |
| P3 | Add Firebase Performance monitoring |
| P3 | Add structured event logging (not just debug prints) |

### Analytics abstraction (recommended)

```kotlin
// core/analytics/AnalyticsTracker.kt
expect interface AnalyticsTracker {
    fun trackScreen(screenName: String)
    fun trackEvent(name: String, params: Map<String, String> = emptyMap())
}
```

---

## 4. Parity

Logging is shared via expect/actual. Any analytics/crash reporting added to commonMain will be equally available on both platforms.
