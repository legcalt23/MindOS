# Threat Model — MindOS

**Date:** 2026-05-26
**Methodology:** STRIDE + Firebase/Mobile-specific analysis
**Scope:** Firestore, Storage, Cloud Functions, KMP Client, Auth, App Check

---

## 1. Actors

| # | Actor | Trust level |
|---|---|---|
| A1 | Unauthenticated user | None |
| A2 | Authenticated legitimate user | Low (client-side) |
| A3 | Authenticated malicious user | None (hostile intent) |
| A4 | User with modified app (repackaged APK) | None |
| A5 | User with reverse-engineered APK/IPA | None |
| A6 | User with stolen auth token | None |
| A7 | Suspended user | None |
| A8 | User attempting admin escalation | None |
| A9 | User attempting cross-account access | None |
| A10 | Legitimate admin | Medium |
| A11 | Compromised admin | None |
| A12 | Automated attacker (bot) | None |
| A13 | Cost-amplification attacker | None |
| A14 | Data exfiltration attacker | None |
| A15 | Malicious file upload attacker | None |

---

## 2. Threat Catalog

### T01 — IDOR (Insecure Direct Object Reference)

| Property | Value |
|---|---|
| **Category** | Authorization bypass |
| **Description** | Attacker manipulates document/path IDs to access another user's data |
| **Actors** | A3, A4, A5, A6 |
| **Precondition** | Authenticated with valid token |
| **Impact** | HIGH — unauthorized data access |
| **Probability** | MEDIUM |
| **Severity** | HIGH |
| **Mitigations** | Firestore rules validate `uid() == userId` on all owner-scoped reads/writes. Path-based ownership enforced. Block checks prevent cross-user social access. |
| **Controls** | `isOwner(userId)`, `canReadProfile(ownerId)`, `isDMParticipant(chatId)`, `noBlockBetween()` |
| **Tests** | User A cannot read User B's privacy doc; User A cannot read User B's DM messages; User A cannot write to User B's profile_visual |
| **Residual risk** | None for direct access. Enumeration of existence via timing side-channel is theoretically possible but impractical. |

### T02 — BOLA (Broken Object Level Authorization)

| Property | Value |
|---|---|
| **Category** | Authorization bypass |
| **Description** | API-level authorization checks missing, allowing access to objects belonging to other users |
| **Actors** | A3, A4 |
| **Impact** | HIGH |
| **Probability** | LOW |
| **Mitigations** | Cloud Functions use `requireSelf()` and `requireAuth()`. Firestore rules enforce ownership on every operation. |
| **Controls** | Dual-layer: CF guards + Firestore rules |
| **Residual risk** | None identified. Both layers enforce independently. |

### T03 — BFLA (Broken Function Level Authorization)

| Property | Value |
|---|---|
| **Category** | Privilege escalation |
| **Description** | Regular user invokes admin-only functions |
| **Actors** | A3, A8 |
| **Impact** | CRITICAL |
| **Probability** | LOW |
| **Mitigations** | Admin functions require custom claims (`admin_role`). `requireAdmin(permission)` checks specific RBAC permission. All Firestore rules deny client writes to admin collections. |
| **Controls** | `requireAdmin()` in CF, `allow write: if false` in rules |
| **Residual risk** | If custom claims are misconfigured, escalation is possible. Mitigated by A7 (claims set server-side only). |

### T04 — Mass Assignment

| Property | Value |
|---|---|
| **Category** | Data integrity |
| **Description** | Client sends extra fields not intended to be writable, bypassing validation |
| **Actors** | A3, A4 |
| **Impact** | HIGH |
| **Probability** | MEDIUM |
| **Mitigations** | Firestore rules use `keys().hasOnly(allowedKeys)` on every create/update. Cloud Functions use strict input validation with `validateXInput()`. |
| **Controls** | `hasOnly()` in rules, schema validation in CF |
| **Residual risk** | None. Both layers reject unknown fields. |

### T05 — Privilege Escalation via Role Tampering

| Property | Value |
|---|---|
| **Category** | Privilege escalation |
| **Description** | Client attempts to set role/permission/admin flags on their own documents |
| **Actors** | A3, A4, A8 |
| **Impact** | CRITICAL |
| **Probability** | LOW |
| **Mitigations** | No client-writable collection contains role, permission, or admin fields. `usernameSearchTokens`, `observersCount`, and `isVerified` are immutable or controlled. Custom claims are server-only. |
| **Controls** | Schema validation, immutable field enforcement, server-only writes for sensitive fields |
| **Residual risk** | None. No path exists for client to modify privilege-bearing fields. |

### T06 — Cross-Tenant Data Leak

| Property | Value |
|---|---|
| **Category** | Data leakage |
| **Description** | User accesses data belonging to another user's private scope |
| **Actors** | A3, A9 |
| **Impact** | HIGH |
| **Probability** | LOW |
| **Mitigations** | All private data is scoped to UID. Privacy settings gate profile reads. Block relationships prevent visibility. DM access requires participant membership. |
| **Controls** | `isOwner()`, `canReadProfile()`, `isDMParticipant()`, `noBlockBetween()` |
| **Residual risk** | Public profiles intentionally expose minimal data. Users control their privacy settings. |

### T07 — Query-Based Data Leak

| Property | Value |
|---|---|
| **Category** | Data leakage |
| **Description** | Attacker crafts queries to extract private data via list operations |
| **Actors** | A3, A14 |
| **Impact** | MEDIUM |
| **Probability** | LOW |
| **Mitigations** | All list queries require limit. Private collections deny list. Searchable profiles are opt-in. DM list requires participant membership. |
| **Controls** | `request.query.limit` enforcement, `resource.data` ownership checks on list |
| **Residual risk** | Searchable profiles are intentionally discoverable. |

### T08 — Collection/Document Enumeration

| Property | Value |
|---|---|
| **Category** | Information disclosure |
| **Description** | Attacker enumerates document IDs to discover users or content |
| **Actors** | A3, A12 |
| **Impact** | LOW |
| **Probability** | MEDIUM |
| **Mitigations** | UIDs are opaque Firebase-generated values. Username lookup requires authentication. Rate limiting on search. Composite IDs (e.g., follow/block) require knowledge of both UIDs. |
| **Controls** | Opaque IDs, rate limiting, auth required |
| **Residual risk** | Username existence can be inferred via registration failure or search. This is inherent to any username system. |

### T09 — Timestamp Tampering

| Property | Value |
|---|---|
| **Category** | Data integrity |
| **Description** | Client provides manipulated timestamps to bypass ordering or deduplication |
| **Actors** | A3, A4 |
| **Impact** | LOW |
| **Probability** | MEDIUM |
| **Mitigations** | `temporalNotBefore()` enforces monotonic `updatedAt`. `temporalOrder()` enforces `createdAt <= updatedAt`. Profile visit dedup uses server-side 10-min window in CF. |
| **Controls** | Monotonic timestamp validation in rules |
| **Residual risk** | Client can set initial `createdAt` to any valid past/future time. This is acceptable because ordering is by server timestamps in critical paths (CF uses `Date.now()`). |

### T10 — Owner Spoofing

| Property | Value |
|---|---|
| **Category** | Authorization bypass |
| **Description** | Client sets `ownerId`/`userId` field to another user's UID |
| **Actors** | A3, A4 |
| **Impact** | HIGH |
| **Probability** | LOW |
| **Mitigations** | All client-writable documents validate `userId == uid()` or `ownerId == uid()`. Storage metadata validates `ownerUid == uid()`. CF uses `callerUid` from auth context. |
| **Controls** | `isOwner()`, `request.resource.data.userId == uid()` |
| **Residual risk** | None. |

### T11 — Storage Abuse / Malicious Upload

| Property | Value |
|---|---|
| **Category** | Abuse |
| **Description** | Attacker uploads malicious, oversized, or harmful files |
| **Actors** | A3, A15 |
| **Impact** | MEDIUM |
| **Probability** | MEDIUM |
| **Mitigations** | MIME type whitelist (no HTML/JS/SVG/EXE). Size limits per type. Filename regex validation. Storage quota enforcement (500 MB, 2000 files per user). Rate limiting (20 files/min). Auto-suspension on quota breach. |
| **Controls** | Storage rules MIME validation, size checks, CF `enforceStorageQuotas` trigger |
| **Residual risk** | **No malware scanning.** Valid image/video files could contain steganographic payloads or exploit parser vulnerabilities. Recommended: add Cloud Function trigger for file scanning before production. |

### T12 — MIME Spoofing

| Property | Value |
|---|---|
| **Category** | Content bypass |
| **Description** | Attacker uploads file with wrong Content-Type header to bypass MIME restrictions |
| **Actors** | A3, A4 |
| **Impact** | MEDIUM |
| **Probability** | MEDIUM |
| **Mitigations** | Storage rules validate `request.resource.contentType` against file extension. Extension and MIME must match. No `application/octet-stream` allowed. |
| **Controls** | Extension-MIME pairing validation |
| **Residual risk** | Storage rules validate declared MIME, not actual file content magic bytes. A file declaring `image/jpeg` but containing non-JPEG data will be accepted. Most image processors will reject it on read. |

### T13 — Cost Amplification

| Property | Value |
|---|---|
| **Category** | Abuse |
| **Description** | Attacker generates excessive Firestore reads/writes or Storage operations to inflate billing |
| **Actors** | A3, A12, A13 |
| **Impact** | HIGH |
| **Probability** | MEDIUM |
| **Mitigations** | All list queries bounded by `request.query.limit`. Rate limiting on all CF callables. Storage quotas with auto-suspension. No global collection scans possible via rules. |
| **Controls** | Query limits in rules, per-UID rate limiting in CF, storage quotas |
| **Residual risk** | An attacker with many accounts could amplify costs across UIDs. Firebase budget alerts and App Check enforcement reduce this risk. |

### T14 — Token Replay / Stolen Token

| Property | Value |
|---|---|
| **Category** | Authentication bypass |
| **Description** | Attacker uses a stolen Firebase Auth ID token to impersonate a user |
| **Actors** | A6 |
| **Impact** | HIGH |
| **Probability** | LOW |
| **Mitigations** | Firebase ID tokens expire after 1 hour. App Check provides device attestation. Rate limiting bounds damage. No admin-only operations possible without custom claims. |
| **Controls** | Token expiry, App Check, rate limits |
| **Residual risk** | Within the 1-hour token window, full user impersonation is possible. Token revocation via `revokeRefreshTokens()` can invalidate sessions but requires detection of compromise. |

### T15 — Sensitive Data in Logs

| Property | Value |
|---|---|
| **Category** | Information disclosure |
| **Description** | PII, tokens, or passwords leak through application logs |
| **Actors** | Internal (log access) |
| **Impact** | MEDIUM |
| **Probability** | LOW |
| **Mitigations** | `SafeDebugLogging.kt` only logs in debug builds. Password logged as presence only (`password_present=true`). Audit logs sanitize `token|secret|password|credential|apikey|authorization|sessionid` keys. Exception class names only (no messages). |
| **Controls** | KMP SafeDebugLogging, CF AuditLogService sanitization |
| **Residual risk** | Third-party library logging is not controlled. Firebase SDK may log request data. Production should use structured logging with PII filters. |

### T16 — Insecure Direct Storage URL

| Property | Value |
|---|---|
| **Category** | Information disclosure |
| **Description** | Firebase Storage download URLs include long-lived tokens that can be shared or leaked |
| **Actors** | A3 |
| **Impact** | MEDIUM |
| **Probability** | MEDIUM |
| **Mitigations** | Storage rules enforce access control. Private files require authenticated reads. DM media metadata includes `visibility=private`. |
| **Controls** | Storage rules, metadata-based access |
| **Residual risk** | Firebase Storage download URLs with embedded tokens bypass rules. If a URL is shared, the recipient can access the file without authentication. Consider using signed URLs with short TTL for sensitive content. |

### T17 — Admin SDK Bypass

| Property | Value |
|---|---|
| **Category** | Privilege escalation |
| **Description** | Cloud Functions using Admin SDK bypass Firestore Rules. If CF validation is incomplete, unauthorized writes can occur. |
| **Actors** | A11 |
| **Impact** | CRITICAL |
| **Probability** | LOW |
| **Mitigations** | All CF enforce auth, App Check, input validation, rate limiting independently. RBAC for admin functions. Maker/checker pattern for wallet adjustments. Audit logging on all admin operations. |
| **Controls** | Defense-in-depth in CF |
| **Residual risk** | A compromised admin with `super_admin` role could bypass maker/checker for low-value adjustments (by design). All actions are audit-logged. |

### T18 — Notification Identity Spoofing

| Property | Value |
|---|---|
| **Category** | Data integrity |
| **Description** | Client creates notification with fake actor username/displayName |
| **Actors** | A3, A4 |
| **Impact** | LOW |
| **Probability** | LOW |
| **Mitigations** | `validActorNotificationIdentity()` checks actor's profile_visual data against supplied values. If profile exists, must match. Only bypasses if profile doesn't exist (new user, no profile yet). |
| **Controls** | Cross-document validation in rules |
| **Residual risk** | A user who hasn't created their profile_visual can create a notification with arbitrary display name. This is a bootstrap edge case. |

### T19 — DM Participant Key Manipulation

| Property | Value |
|---|---|
| **Category** | Authorization bypass |
| **Description** | Attacker crafts DM chat ID to gain access to conversations between other users |
| **Actors** | A3, A4 |
| **Impact** | HIGH |
| **Probability** | LOW |
| **Mitigations** | Chat ID format encodes both participant UIDs. `validDMChatIdForParticipants()` validates ID format against participant array. `isDMParticipant()` checks `uid() in get(dmChatPath).data.participants`. Create requires `uid() in participants`. |
| **Controls** | ID format validation, participant membership check |
| **Residual risk** | None. The chat ID encoding prevents access to conversations the user is not part of. |

### T20 — Wallet Balance Manipulation

| Property | Value |
|---|---|
| **Category** | Financial fraud |
| **Description** | Client attempts to modify wallet balance, spend without funds, or credit without purchase |
| **Actors** | A3, A4 |
| **Impact** | CRITICAL (if real money) |
| **Probability** | LOW |
| **Mitigations** | All wallet collections deny client writes. Balance computed server-side from ledger. Spend operations use optimistic locking with version checks. Idempotency prevents replay. Fraud scoring with auto-freeze. |
| **Controls** | Deny-by-default rules, CF-only writes, ledger integrity, idempotency |
| **Residual risk** | None in current sandbox. Real payment integration will require webhook signature validation (currently stubbed as fail-closed). |

---

## 3. STRIDE Summary

| Category | Threats | Status |
|---|---|---|
| **Spoofing** | T10, T14, T18 | Mitigated |
| **Tampering** | T04, T05, T09, T20 | Mitigated |
| **Repudiation** | Audit logging covers all sensitive operations | Mitigated |
| **Information Disclosure** | T06, T07, T08, T15, T16 | Mitigated (residual: Storage URL sharing) |
| **Denial of Service** | T13 | Partially mitigated (rate limits + quotas; multi-account amplification residual) |
| **Elevation of Privilege** | T01, T02, T03, T05, T17, T19 | Mitigated |

---

## 4. Residual Risk Summary

| Risk | Severity | Accepted? |
|---|---|---|
| No malware scanning on uploaded files | MEDIUM | Yes (for sandbox; recommend Cloud Function scanner before production) |
| Storage download URLs bypass rules once shared | MEDIUM | Yes (consider signed URLs with TTL for private content) |
| Multi-account cost amplification | MEDIUM | Yes (Firebase budget alerts, App Check enforcement) |
| Stolen token impersonation within 1-hour window | MEDIUM | Yes (inherent to token-based auth) |
| Third-party library logging may leak data | LOW | Yes (monitor in production) |
| Username existence inference | LOW | Yes (inherent to username systems) |
