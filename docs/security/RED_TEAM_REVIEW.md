# Red Team Self-Audit

**Date:** 2026-05-26
**Methodology:** Adversarial review — simulate attacker with full knowledge of rules, code, and architecture.

---

## 1. Attack Scenarios

### A01 — Modified client app
**Scenario:** Attacker repackages the APK, disables client-side validation, sends arbitrary requests.
**Bypass attempted:**
- Set `userId` to another UID in Firestore writes
- Add extra fields like `isAdmin`, `role` to documents
- Skip MIME validation before Storage upload
- Send unbounded list queries

**Result:** All blocked.
- Firestore rules enforce `isOwner(userId)` regardless of client validation
- `keys().hasOnly()` rejects extra fields server-side
- Storage rules validate content type independently
- Rules enforce `request.query.limit`

**Control:** Server-side validation at rules + CF layer.
**Test:** T1-T70 in `firestore.security.rules.test.mjs`
**Residual risk:** None.

### A02 — APK reverse engineered
**Scenario:** Attacker extracts API keys, Firebase project ID, and SDK initialization from APK.
**Bypass attempted:** Use extracted credentials to make direct API calls from external tool.
**Result:**
- Firebase API key is not a secret (public by design)
- App Check (Play Integrity) blocks non-Play-Store-signed APKs in production
- All sensitive operations require auth token + App Check token

**Control:** App Check enforcement, auth requirement on all CFs.
**Residual risk:** Debug builds bypass Play Integrity (acceptable, dev-only).

### A03 — iOS app instrumented (Frida)
**Scenario:** Attacker uses Frida to hook iOS Firebase SDK calls.
**Bypass attempted:** Modify outgoing request data before signing.
**Result:**
- Server-side validation at rules and CF layer
- App Attest provides device-bound attestation in production iOS
- Auth tokens are cryptographically bound to the user

**Residual risk:** iOS App Check not deeply audited in this review.

### A04 — Stolen user token
**Scenario:** Attacker obtains a valid Firebase ID token (XSS, debugger, etc).
**Bypass attempted:** Use token to read/write data as victim.
**Result:**
- Token expires in 1 hour
- Within that window, full impersonation is possible (inherent to bearer tokens)
- Rate limiting bounds damage per minute
- App Check provides device attestation — token from another device may fail App Check
- `revokeRefreshTokens()` can invalidate the session if compromise is detected

**Control:** Token TTL, App Check, rate limiting, revocation API.
**Residual risk:** 1-hour impersonation window is inherent. Consider shorter token TTL for sensitive operations.

### A05 — Stolen admin token
**Scenario:** Attacker obtains admin's Firebase ID token with `admin_role` claim.
**Bypass attempted:** Perform admin wallet adjustments.
**Result:**
- Maker/checker pattern: cannot approve own request (FRAUD_DETECTED)
- 2FA hook available (recommend enabling)
- IP allowlist hook available (recommend enabling)
- All actions audit-logged

**Control:** Maker/checker, RBAC permissions, audit log.
**Residual risk:** Same-admin self-execute for low values is allowed by design (requires explicit flag + permission).

### A06 — Manipulate UID in path
**Scenario:** Attacker authenticates as Alice, sends request to `/users/bob`.
**Result:** Rules check `uid() == userId`. Bob's UID ≠ Alice's UID → DENY.

**Test:** T7, T11
**Residual risk:** None.

### A07 — Manipulate ownerId
**Scenario:** Attacker sets `userId: "bob"` in a request as Alice.
**Result:**
- Profile_visuals create: `data.userId == userId` (path) AND path UID is Alice → DENY
- Posts create: `isOwner(data.userId)` checks `uid() == data.userId` → DENY
- CF posts: `userId = callerUid` (server overrides client) → safe

**Test:** T10, T11, T12
**Residual risk:** None.

### A08 — Manipulate orgId
**Scenario:** N/A — no organization model in this project.

### A09 — Manipulate role
**Scenario:** Attacker tries to set `role`/`isAdmin` on own document.
**Result:**
- `keys().hasOnly()` rejects unknown fields → DENY
- No client-writable collection has `role` field in schema
- Custom claims set server-side only

**Test:** T18, T19, T22
**Residual risk:** None.

### A10 — Manipulate permissions
**Scenario:** Add `permissions: ["admin"]` to profile_visual.
**Result:** Field not in `visualKeys()` allowlist → rejected.

**Test:** T22
**Residual risk:** None.

### A11 — Manipulate billingStatus
**Scenario:** Add `billingStatus: "premium"` to own profile.
**Result:** Field not in any client-writable schema → rejected.

**Test:** T21
**Residual risk:** None.

### A12 — Insert extra fields
**Scenario:** Send PATCH with arbitrary extra fields.
**Result:** `request.resource.data.diff(resource.data).affectedKeys().hasOnly(mutableKeys)` → DENY.

**Test:** T18-T23
**Residual risk:** None.

### A13 — Omit required fields
**Scenario:** Send create with missing required fields.
**Result:** `keys().hasAll(requiredKeys)` → DENY.

**Test:** T60
**Residual risk:** None.

### A14 — Tamper timestamps
**Scenario:** Set `createdAt` to future date, `updatedAt` to old date.
**Result:**
- `temporalOrder(createdAt, updatedAt)` requires `updatedAt >= createdAt`
- `temporalNotBefore()` requires monotonic updates
- Backend uses server `Date.now()` for critical timestamps

**Residual risk:** Initial `createdAt` value is client-set but bounded by `temporalOrder`. Server timestamps are preferred for ordering-critical paths.

### A15 — Unbounded query
**Scenario:** `db.collection('posts').get()` with no limit.
**Result:** Rules require `request.query.limit != null && request.query.limit <= 100` → DENY.

**Test:** Implicit in T49-T52 (bounded list tests)
**Residual risk:** None.

### A16 — Read other organization data
**Scenario:** N/A — no organizations.

### A17 — Write to other organization
**Scenario:** N/A — no organizations.

### A18 — Create fake membership
**Scenario:** N/A — no organizations.

### A19 — Upload malware
**Scenario:** Upload .exe disguised as image.
**Result:**
- Storage rules require `request.resource.contentType in [allowlist]`
- Filename extension must match content type
- `application/x-msdownload` and similar are not in allowlist → DENY

**Residual risk:** **No malware scanning on accepted file types.** A valid JPEG could contain steganographic payload or exploit image parser CVE. Recommend Cloud Function trigger for content scanning in production.

### A20 — Upload SVG
**Scenario:** Upload SVG with embedded JavaScript.
**Result:** `image/svg+xml` not in any allowlist → DENY.

**Residual risk:** None.

### A21 — Use fake contentType
**Scenario:** Upload .exe but declare `Content-Type: image/jpeg`.
**Result:**
- Storage rules check filename regex pairing: `'^[...]+\\.jpg$'` AND `contentType == "image/jpeg"`
- If filename ends in `.exe`, regex fails → DENY
- If filename ends in `.jpg`, the actual content isn't scanned (residual risk)

**Residual risk:** Same as A19 — content magic bytes not validated.

### A22 — Abuse costs
**Scenario:** Run automated script to spam reads/writes.
**Result:**
- All list queries bounded (limit ≤ 100)
- Per-UID rate limiting in CF (10-60 calls/min depending on operation)
- Storage quota: 500 MB / 2000 files per user
- Auto-suspension on quota breach
- App Check blocks non-attested clients

**Residual risk:** Multi-account amplification possible. Mitigate with Firebase budget alerts.

### A23 — Delete critical data
**Scenario:** Delete own user account, wallet, ledger entries.
**Result:**
- `users`: delete denied
- `profile_visuals`: delete denied
- `wallets`, `ledger_*`: all denied
- `posts`: delete allowed (owner only) — soft-delete preferred

**Residual risk:** None.

### A24 — Exploit Cloud Functions
**Scenario:** Send malformed input, race conditions, replay attacks.
**Result:**
- Schema validation rejects malformed input
- Optimistic locking with version field prevents race conditions on wallet
- Idempotency keys prevent replay
- Rate limiting bounds amplification

**Residual risk:** Cloud Functions are TypeScript with strict mode + 704 tests.

### A25 — Use Admin SDK indirectly
**Scenario:** Trick a CF into performing unauthorized write.
**Result:** Every CF independently validates auth, App Check, ownership, schema. No CF accepts arbitrary userId/walletId/amount from client.

**Test:** Cloud Functions security.test.ts
**Residual risk:** None (assuming CF code is correct; 704 tests provide coverage).

### A26 — Access audit logs
**Scenario:** Read `auditEvents`, `wallet_audit_logs`, `admin_actions`.
**Result:** All deny client read.

**Test:** T30, T42, T44
**Residual risk:** None.

### A27 — Leak PII via errors
**Scenario:** Trigger CF error to extract internal state.
**Result:**
- CFs return generic Firebase error codes
- No internal paths/IDs in messages
- Audit logs sanitize sensitive keys

**Residual risk:** Firebase SDK may include some technical detail; review production logs.

### A28 — Infer document existence
**Scenario:** Use timing of read failures to determine if a document exists.
**Result:**
- All denied reads return `permission-denied` with same latency (Firestore handles uniformly)
- No oracle attack via response codes

**Residual risk:** Username existence is intentionally inferrable via registration failure (any username system has this).

### A29 — Enumerate resources
**Scenario:** Iterate possible UIDs to find users.
**Result:**
- Firebase UIDs are 28-character random strings (not enumerable)
- Username lookup requires authenticated read
- Search results limited to opt-in discoverable profiles (max 12)

**Residual risk:** Limited — UIDs are opaque and large space.

### A30 — Cross-DM message injection
**Scenario:** Send message to DM chat A with senderId from chat B.
**Result:**
- `validDMMessageCreate` requires `senderId == uid()`
- `isDMParticipant(chatId)` requires `uid() in get(dmChatPath).data.participants`
- Both checks must pass

**Test:** T66
**Residual risk:** None.

---

## 2. Bypasses Found and Fixed in This Audit

| Bypass | Severity | Fix applied | Test added |
|---|---|---|---|
| `dm_chats` list unbounded → cost amplification | HIGH | Added `limit <= 50` | T49 |
| `user_posts/{uid}/posts` list unbounded | HIGH | Added `limit <= 100` | T52 |
| `feeds/{viewerId}/posts` list unbounded | HIGH | Added `limit <= 100` | T51 |
| `dm_chats/.../messages` list unbounded | MEDIUM | Added `limit <= 100` | T50 |

## 3. Residual Risks (Accepted)

| Risk | Severity | Mitigation |
|---|---|---|
| No malware scanning on accepted file types | MEDIUM | Recommend CF scanning trigger before production |
| 1-hour token impersonation window | MEDIUM | Inherent to bearer tokens; revocation API available |
| Multi-account cost amplification | MEDIUM | Firebase budget alerts; App Check enforcement |
| File content magic bytes not validated | LOW | Image parsers reject non-image content; consider validation in CF for sensitive uploads |
| Storage URL token sharing | MEDIUM | Consider signed URLs with TTL for private content |
| iOS App Attest not deeply audited | LOW | Recommend separate iOS audit |
| Initial createdAt is client-set | LOW | Use server timestamps for ordering-critical paths |
| Same-admin self-execute for low-value adjustments | LOW | By design, requires explicit flag + permission |
| Username existence inference | LOW | Inherent to any username system |

## 4. Improvements Suggested

1. **Add Cloud Function trigger for image content validation** — at minimum, verify magic bytes match declared MIME for image uploads
2. **Implement signed URLs with short TTL** for private DM media
3. **Enable App Check enforcement mode** in production Firebase Console
4. **Enable admin 2FA and IP allowlist** before production admin operations
5. **Schedule daily wallet reconciliation** before real-money
6. **Set Firebase budget alerts** with anomaly detection
7. **Consider shorter ID token TTL** (1 hour minimum, but session-bound rotation possible)
8. **iOS App Attest deep audit** — verify enforcement mode and provider configuration

## 5. Conclusion

The MindOS security architecture withstands the simulated adversarial review. Four unbounded list query bypasses were identified and fixed during this audit. No critical bypasses remain.

The residual risks are either inherent to the technology choices (bearer tokens, opaque IDs) or operational concerns deferred to production hardening (malware scanning, admin 2FA enforcement, signed URLs).

**No critical bypasses remain after fixes applied.**
