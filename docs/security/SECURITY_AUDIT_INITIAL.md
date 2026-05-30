# Security Audit — Phase 1: Initial Repository Inspection

**Date:** 2026-05-26
**Auditor role:** Multi-discipline security team (simulated)
**Scope:** Full repository — Firestore, Storage, Cloud Functions, KMP client, Auth, App Check
**Branch:** `chore/audit-fase-1`

---

## 1. Executive Summary

MindOS is a Kotlin Multiplatform social application (Android/Desktop/iOS) backed by Firebase (Firestore, Storage, Auth, App Check, Cloud Functions). It includes a virtual wallet/currency system currently in sandbox mode.

**Overall security posture: STRONG — with specific hardening items identified below.**

The project demonstrates mature defensive engineering:
- 1720-line Firestore rules with deny-by-default and catch-all deny
- 382-line Storage rules with App Check enforcement and MIME validation
- Consistent security envelope across all 18+ Cloud Functions callables
- 704 backend tests + 165 KMP tests, all passing
- Append-only audit logging with PII redaction
- RBAC with maker/checker pattern for admin operations
- Fraud detection with auto-freeze capabilities

However, this audit identifies **3 HIGH**, **6 MEDIUM**, and **5 LOW** issues requiring attention.

---

## 2. Architecture Map

```
┌─────────────────────────────────────────────────────┐
│                   KMP Client                         │
│  (Android / Desktop / iOS)                           │
│                                                      │
│  ┌─────────┐ ┌──────────┐ ┌──────────┐ ┌─────────┐ │
│  │ Features│ │Validators│ │   DTOs   │ │  Repos  │ │
│  │  (UI)   │→│(SafeInput│→│(typed)   │→│(Firebase│ │
│  │         │ │ .kt)     │ │          │ │  SDK)   │ │
│  └─────────┘ └──────────┘ └──────────┘ └────┬────┘ │
└──────────────────────────────────────────────┼──────┘
                                               │
              ┌────────────────────────────────┼──────────────┐
              │          Firebase Platform      │              │
              │                                 ▼              │
              │  ┌──────────┐  ┌───────────────────────────┐  │
              │  │   Auth   │  │     Cloud Functions        │  │
              │  │+ App     │  │  (requireAppCheck →        │  │
              │  │  Check   │  │   requireAuth →            │  │
              │  └────┬─────┘  │   validateInput →          │  │
              │       │        │   checkRateLimit →          │  │
              │       │        │   business logic)           │  │
              │       │        └──────────┬────────────────┘  │
              │       │                   │                    │
              │  ┌────▼───────────────────▼────────────────┐  │
              │  │           Cloud Firestore               │  │
              │  │  (29 collections, deny-by-default)      │  │
              │  └────────────────────────────────────────┘  │
              │  ┌────────────────────────────────────────┐  │
              │  │         Firebase Storage                │  │
              │  │  (3 path trees, deny-by-default)       │  │
              │  └────────────────────────────────────────┘  │
              └───────────────────────────────────────────────┘
```

---

## 3. Collection Map — Cloud Firestore

### 3.1 Client-writable collections (rules allow client writes)

| Collection | Doc ID | Client can | Access control |
|---|---|---|---|
| `privacy/{userId}` | UID | create, update | Owner only |
| `usernames/{username}` | handle | create, update, delete | Owner of claim |
| `profile_visuals/{userId}` | UID | create, update | Owner + observer counter |
| `profile_visits/{visitId}` | `{viewer}_{owner}` | create, update | Viewer only |
| `observations/{relationId}` | `{observer}_{observed}` | create | Observer only (atomic with counter + notification) |
| `notifications/{notificationId}` | `{actor}_{recipient}` | create, update(isRead) | Actor creates, recipient updates |
| `posts/{postId}` | safe ID | create, delete | Owner only |
| `user_posts/{userId}/posts/{postId}` | safe ID | create, delete | Owner only |
| `dm_chats/{chatId}` | conversation ID | create, update | Participants only |
| `dm_chats/{chatId}/messages/{messageId}` | message ID | create, update | Participants only |

### 3.2 Server-only collections (rules deny all client writes)

| Collection | Purpose |
|---|---|
| `users/{userId}` | User account (status, handle) |
| `profiles/{userId}` | User profile (display name, bio, avatar) |
| `follows/{relationId}` | Follow relationships |
| `followRequests/{requestId}` | Follow request state machine |
| `blocks/{relationId}` | Block relationships |
| `counters/{userId}` | Social graph counters |
| `feeds/{viewerId}/posts/{postId}` | Materialized feed entries |
| `auditEvents/{eventId}` | Audit trail |
| `wallets/{walletId}` | Wallet status |
| `balance_snapshots/{snapshotId}` | Balance state |
| `ledger_transactions/{txId}` | Ledger transactions |
| `ledger_entries/{entryId}` | Ledger entries |
| `idempotency_keys/{keyId}` | Replay protection |
| `purchases/{purchaseId}` | Purchase records |
| `purchase_token_index/{indexId}` | Purchase dedup |
| `payment_provider_events/{eventId}` | Payment events |
| `fraud_signals/{signalId}` | Fraud signals |
| `wallet_audit_logs/{logId}` | Wallet audit |
| `admin_actions/{actionId}` | Admin action log |

### 3.3 Client-readable collections

| Collection | get | list | Conditions |
|---|---|---|---|
| `users/{userId}` | Owner | NO | Owner only |
| `profiles/{userId}` | Privacy-gated | Bounded (12) | canReadProfile + block check |
| `privacy/{userId}` | Owner | NO | Owner only |
| `usernames/{username}` | Authenticated | NO | signedIn |
| `profile_visuals/{userId}` | Privacy-gated | Bounded (12) | canReadProfileVisual / search |
| `profile_visits/{visitId}` | Viewer | NO | Viewer only |
| `follows/{relationId}` | Participant | NO | Follower or followee |
| `observations/{relationId}` | Authenticated | NO | signedIn |
| `notifications/{id}` | Participant | Bounded (100) | Recipient or actor |
| `followRequests/{id}` | Participant | NO | Requester or target |
| `blocks/{relationId}` | Blocker | NO | Blocker only |
| `counters/{userId}` | Privacy-gated | NO | canReadProfile |
| `posts/{postId}` | Privacy-gated | Bounded (100) | canReadPostData |
| `user_posts/{uid}/posts/{id}` | Privacy-gated | **UNBOUNDED** | Privacy-gated |
| `feeds/{viewerId}/posts/{id}` | Owner | **UNBOUNDED** | Owner only |
| `dm_chats/{chatId}` | Participant | **UNBOUNDED** | Participant only |
| `dm_chats/.../messages/{id}` | Participant | **UNBOUNDED** | Participant only |
| `wallets/{walletId}` | Owner | NO | Owner only |
| `balance_snapshots/{id}` | Owner | NO | Owner only |

---

## 4. Storage Path Map

| Path | Operations | Access |
|---|---|---|
| `profile_visuals/{uid}/{module}/{file}` | get, create, delete | Owner write/delete; public/private read via metadata |
| `post_attachments/{uid}/{postId}/{id}` | get, create, delete | Owner write/delete; visibility-gated read |
| `dm_media/{chatId}/{file}` | get, create | Participant access via metadata; no delete |

---

## 5. Cloud Functions Map

### 5.1 Callable functions (18)

| Function | Auth | AppCheck | Validation | RateLimit | Transactions |
|---|---|---|---|---|---|
| verifyClientIntegrity | ✅ | ✅ | — | — | — |
| followUser | ✅ | ✅ | ✅ | 30/min | ✅ |
| unfollowUser | ✅ | ✅ | ✅ | 30/min | ✅ |
| sendFollowRequest | ✅ | ✅ | ✅ | 20/min | ✅ |
| cancelFollowRequest | ✅ | ✅ | ✅ | 20/min | ✅ |
| acceptFollowRequest | ✅ | ✅ | ✅ | 20/min | ✅ |
| denyFollowRequest | ✅ | ✅ | ✅ | 20/min | ✅ |
| blockUser | ✅ | ✅ | ✅ | 20/min | ✅ |
| unblockUser | ✅ | ✅ | ✅ | 20/min | ✅ |
| recordProfileVisit | ✅ | ✅ | ✅ | 60/min | ✅ |
| createPost | ✅ | ✅ | ✅ | 10/min | ✅ |
| deletePost | ✅ | ✅ | ✅ | 30/min | ✅ |
| getWalletBalance | ✅ | ✅ | ✅ | 30/min | ✅ |
| spendCoins | ✅ | ✅ | ✅ | 20/min(FC) | ✅ |
| getWalletTransactions | ✅ | ✅ | ✅ | 30/min | — |
| initiatePurchase | ✅ | ✅ | ✅ | 10/min(FC) | ✅ |
| verifyPurchase | ✅ | ✅ | ✅ | 10/min(FC) | ✅ |
| adminRequestWalletAdjustment | ✅ | ✅ | ✅ | 20/min(FC) | ✅ |
| adminApproveWalletAdjustment | ✅ | ✅ | ✅ | 30/min(FC) | ✅ |

*(FC = fail-closed rate limiting)*

### 5.2 Triggers (3)

| Trigger | Type | Purpose |
|---|---|---|
| enforceStorageQuotas | Storage onFinalize | Quota + rate limit + auto-suspend |
| reduceStorageQuotasOnDelete | Storage onDelete | Decrement usage ledger |
| onPostDeleted | Firestore onDelete | Purge attachments + feed entries |

### 5.3 Webhooks (3)

| Endpoint | Provider | Status |
|---|---|---|
| webhookGooglePlay | Google Play | Stub (fail-closed 403) |
| webhookAppStore | Apple IAP | Stub (fail-closed 403) |
| webhookStripe | Stripe | Stub (fail-closed 403) |

---

## 6. Risk Assessment

### 6.1 HIGH Risks

#### H1 — Unbounded `list` on `dm_chats` collection
- **Location:** `firestore.rules:1639-1640`
- **Issue:** `dm_chats` list rule has no `request.query.limit` check. An attacker with many DM conversations could execute unbounded queries, causing excessive reads and cost amplification.
- **Rule:** `allow list: if signedIn() && uid() in resource.data.participants;`
- **Impact:** Cost amplification, potential DoS via Firestore read billing
- **Fix:** Add `request.query.limit != null && request.query.limit <= 50`

#### H2 — Unbounded `list` on `user_posts` subcollection
- **Location:** `firestore.rules:1610-1615`
- **Issue:** `user_posts/{userId}/posts` list rule has no query limit enforcement.
- **Impact:** Cost amplification via unbounded reads of another user's posts (if privacy allows)
- **Fix:** Add `request.query.limit != null && request.query.limit <= 100`

#### H3 — Unbounded `list` on `feeds` subcollection
- **Location:** `firestore.rules:1627`
- **Issue:** `feeds/{viewerId}/posts` list rule has no query limit. Owner-only access, but a compromised token could cause cost amplification.
- **Impact:** Cost amplification via unbounded feed reads
- **Fix:** Add `request.query.limit != null && request.query.limit <= 100`

### 6.2 MEDIUM Risks

#### M1 — Unbounded `list` on `dm_chats/{chatId}/messages`
- **Location:** `firestore.rules:1648`
- **Issue:** DM messages list has no query limit. Participant-only, but unbounded.
- **Fix:** Add `request.query.limit != null && request.query.limit <= 100`

#### M2 — Client-driven `observersCount` increment
- **Location:** `firestore.rules:571-582`
- **Issue:** `validObserverCounterUpdate` allows any authenticated user (not self) to increment `observersCount` by exactly 1 atomically with observation creation. While the atomic constraint with `existsAfter(observationPath(...))` prevents blind increment, this is a client-trusted counter operation.
- **Mitigation:** Already requires simultaneous observation creation; risk is limited to atomic batch manipulation.
- **Residual risk:** A user with a modified app could theoretically craft valid atomic batches faster than intended.

#### M3 — Webhook signature validation not implemented
- **Location:** `functions/src/webhooks.ts`
- **Status:** Intentionally stubbed; real providers fail-closed (403)
- **Risk:** If accidentally enabled in production without real validation, unverified payment events could be processed.
- **Mitigation:** `SANDBOX_PAYMENTS_ENABLED` flag; fail-closed by default.

#### M4 — Admin 2FA and IP allowlist disabled
- **Location:** `functions/src/lib/adminAuth.ts`
- **Issue:** Configuration hooks exist but are disabled. Compromised admin token could perform wallet operations.
- **Mitigation:** RBAC + maker/checker pattern provides defense in depth.

#### M5 — No `.firebaserc` committed
- **Issue:** Missing `.firebaserc` means deploy target must be specified manually. Risk of accidental deployment to wrong project.
- **Fix:** Create `.firebaserc` with project alias.

#### M6 — Rate limiter fail-open for social graph actions
- **Location:** `functions/src/lib/rateLimiter.ts`
- **Issue:** Non-economic actions (follow, block, profileVisit) proceed if rate limiter infrastructure fails.
- **Mitigation:** Acceptable trade-off; economic actions fail-closed. Global Firestore outage is the only trigger.

### 6.3 LOW Risks

#### L1 — Username search limit mismatch (KMP: 50, Rules: 12)
- **Issue:** KMP `UsernameSearchMaxResults = 50` but Firestore rules enforce `limit <= 12`. The rules are authoritative so this is safe, but the mismatch could confuse developers.
- **Fix:** Align KMP constant to match rules.

#### L2 — No dedicated Firestore Rules tests for wallet collections
- **Issue:** Wallet collections are all deny-by-default (`if false`), but no test explicitly verifies this.
- **Fix:** Add wallet rules test cases.

#### L3 — Reconciliation job not scheduled
- **Issue:** `ReconciliationService.reconcileWallet` exists but no Cloud Scheduler trigger.
- **Deferral:** Acceptable for sandbox; required before real-money.

#### L4 — Storage quota rate limit window allows boundary bursts
- **Issue:** 20 files/60s window resets on first upload after expiry, allowing bursts at boundary.
- **Mitigation:** Global 500 MB + 2000 files cap still enforced.

#### L5 — Firestore rules don't enforce App Check
- **Issue:** Unlike Storage rules, Firestore rules don't check `request.app`. App Check enforcement relies on Firebase Console settings, not rules.
- **Note:** This is by Firebase design — Firestore App Check is enforced at the project level, not in rules. Storage rules support `request.app` as an additional layer.

---

## 7. Assumptions

Documented separately in `ASSUMPTIONS.md`.

---

## 8. Immediate Recommendations

1. **FIX NOW:** Add `request.query.limit` checks to `dm_chats` list, `user_posts` list, `feeds` list, and `dm_messages` list rules. (H1, H2, H3, M1)
2. **FIX NOW:** Create `.firebaserc` with correct project alias. (M5)
3. **PLAN:** Enable App Check enforcement mode before production. (L5)
4. **PLAN:** Implement webhook signature validation before real payment integration. (M3)
5. **PLAN:** Enable admin 2FA and IP allowlist before production. (M4)
6. **PLAN:** Schedule reconciliation job before real-money. (L3)
7. **PLAN:** Add wallet Firestore rules tests. (L2)
8. **PLAN:** Align KMP username search limit constant with rules. (L1)
