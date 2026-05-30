# Auth & App Check Security Review

**Date:** 2026-05-26

---

## 1. Firebase Authentication

### 1.1 Current configuration
- **Provider:** Email/Password only
- **Minimum password:** 8 characters (enforced client-side in `FirebaseSessionRepository`)
- **Email normalization:** `.trim()` applied before auth calls
- **Auth state:** Reactive `Flow<AuthenticatedUser?>` in `SessionRepository`
- **Token type:** Firebase ID Token (1-hour expiry, auto-refresh)

### 1.2 Security assessment

| Control | Status | Notes |
|---|---|---|
| Email verification | ✅ Available | `isEmailVerified` exposed in `AuthenticatedUser` |
| Email verification enforcement | ⚠️ Not enforced | Firestore rules don't check `request.auth.token.email_verified` |
| Password policy | ✅ Min 8 chars | Firebase default; consider stronger policy |
| Session management | ✅ Firebase-managed | Token auto-refresh, 1-hour ID token TTL |
| Token revocation | ✅ Available | `revokeRefreshTokens()` via Admin SDK |
| Multi-device sessions | ✅ Firebase-managed | Each device gets independent refresh token |
| Brute-force protection | ✅ Firebase-managed | Firebase Auth rate-limits failed attempts |

### 1.3 Recommendations
1. **Enforce email verification** — add `request.auth.token.email_verified == true` to sensitive write rules if business requires verified emails
2. **Reauth for sensitive actions** — consider requiring recent authentication for profile deletion or email change
3. **Password complexity** — consider requiring uppercase + number + special character

---

## 2. Custom Claims

### 2.1 Current usage
Custom claims are used exclusively for admin RBAC:

| Claim | Values | Set by | Used by |
|---|---|---|---|
| `admin_role` | `super_admin`, `admin`, `support` | Admin SDK (server-only) | `requireAdmin()` in CF |

### 2.2 Security assessment

| Control | Status |
|---|---|
| Claims set server-side only | ✅ No client path to set claims |
| Deny by default | ✅ Missing claim = denied |
| Closed set of roles | ✅ Unknown roles rejected |
| Escalation prevention | ✅ Only `super_admin` has `admin.manage_roles` |
| Client cannot read claims | ✅ Claims only checked server-side in CF context |

### 2.3 Recommendations
1. **Implement claim rotation** — quarterly review of admin role assignments
2. **Log claim changes** — audit log when claims are modified
3. **Consider role expiry** — time-bounded admin access for temporary roles

---

## 3. User Suspension

### 3.1 Current implementation
- **Account status:** `users/{uid}.status` field (Active/Suspended/Deleted) — server-only
- **Storage suspension:** `storage_usage/{uid}.isSuspended` — set by `enforceStorageQuotas` CF
- **Wallet freeze:** Wallet `status` field — set by fraud detection CF
- **Enforcement points:**
  - `requireNotStorageSuspended()` in `createPost` CF
  - Wallet status check in all wallet CFs
  - Firestore rules don't check suspension (by design — CF handles this)

### 3.2 Security assessment
| Control | Status |
|---|---|
| Suspension is server-controlled | ✅ Client cannot modify status |
| Suspended users cannot post | ✅ CF enforces |
| Suspended users cannot spend | ✅ Wallet CF enforces |
| Suspended users can still read own data | ✅ By design (data access != action permission) |
| Self-unsuspension prevented | ✅ No client write path to status field |

---

## 4. Firebase App Check

### 4.1 Current configuration

| Platform | Provider | Auto-refresh |
|---|---|---|
| Android (debug) | `DebugAppCheckProviderFactory` | ✅ |
| Android (release) | `PlayIntegrityAppCheckProviderFactory` | ✅ |
| iOS | Expected: DeviceCheck or App Attest | Not audited |
| Desktop | Not configured | Expected: not applicable for JVM |

### 4.2 Enforcement points

| Layer | Enforcement | Status |
|---|---|---|
| Cloud Functions | `requireAppCheck(context)` | ✅ All callables |
| Storage Rules | `isAppChecked()` in `isOwner()` | ✅ Profile + post uploads |
| Firestore Rules | Not enforced (Firebase design) | ✅ N/A — project-level setting |
| Firebase Console | Audit mode vs enforcement mode | ⚠️ Currently audit mode |

### 4.3 Recommendations
1. **Switch to enforcement mode** before production — block unenforced requests
2. **Register debug tokens** — ensure all developer devices have registered debug tokens
3. **Monitor App Check metrics** — track unenforced request volume in Firebase Console
4. **iOS App Attest** — use App Attest for iOS 14+ instead of DeviceCheck for stronger attestation

---

## 5. Reauth for Sensitive Actions

### Current state
No reauth is currently required for any action. All operations use the cached ID token.

### Recommendation
Consider requiring recent authentication (`reauthenticateWithCredential()`) for:
- Account deletion
- Email change
- Password change
- Wallet operations above a threshold (if real money)
- Admin role changes

---

## 6. Summary

| Area | Status | Action needed |
|---|---|---|
| Firebase Auth | ✅ Strong | Consider email verification enforcement |
| Custom claims | ✅ Excellent | Add rotation runbook |
| Suspension | ✅ Server-controlled | None |
| App Check | ⚠️ Audit mode | Switch to enforcement before production |
| Reauth | ❌ Not implemented | Implement for sensitive actions |
