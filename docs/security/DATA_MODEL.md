# Data Model — MindOS

**Date:** 2026-05-26

---

## 1. Design Principles Applied

1. **No organization/tenant layer** — MindOS is a user-centric social app. All data isolation is per-UID.
2. **Public data separated from private data** — `profile_visuals` (public-facing) vs `privacy` (owner-only) vs `users` (server-only account).
3. **Server-only writes for sensitive collections** — follows, blocks, followRequests, counters, wallet, ledger, audit, admin.
4. **Client writes validated exhaustively** — every client-writable field has type, length, format, and business rule validation in Firestore rules.
5. **Immutable fields enforced** — `createdAt`, `userId`, `ownerId`, `schemaVersion` locked after creation.
6. **No privileged fields in client-writable documents** — no `role`, `isAdmin`, `isPremium`, `billingStatus` fields exist in any client-writable collection.
7. **Atomic operations where required** — username claims, observation + counter + notification, post dual-write.
8. **Composite document IDs for uniqueness** — follows, blocks, observations use `{userA}_{userB}` pattern.
9. **Deny-by-default with catch-all** — `match /{document=**} { allow read, write: if false; }` at the end.

---

## 2. Collection Architecture

```
User Identity Layer (server-managed)
├── users/{uid}                    — Account status (server-only writes)
├── profiles/{uid}                 — User profile (server-only writes)
└── usernames/{handle}             — Username reservation (client CRUD, validated)

User Configuration Layer (owner-managed)
├── privacy/{uid}                  — Privacy settings (owner R/W)
└── profile_visuals/{uid}          — Rich profile layout (owner R/W, public read)

Social Graph Layer (server-managed)
├── follows/{followerId_followeeId}     — Follow edges (CF only)
├── followRequests/{requestId}          — Follow request state machine (CF only)
├── blocks/{blockerId_blockedId}        — Block edges (CF only)
├── counters/{uid}                      — Derived counters (CF only)
└── observations/{observerId_observedId} — Observer edges (client create, atomic)

Content Layer (client + server)
├── posts/{postId}                      — Global post index
├── user_posts/{uid}/posts/{postId}     — Per-user posts
├── feeds/{viewerId}/posts/{postId}     — Materialized feed (server-only)
└── notifications/{actorId_recipientId} — In-app notifications

Messaging Layer (client-managed, participant-gated)
├── dm_chats/{chatId}                   — DM conversations
└── dm_chats/{chatId}/messages/{msgId}  — DM messages

Behavioral Layer (client-managed)
└── profile_visits/{viewerId_ownerId}   — Profile visit tracking

Financial Layer (server-only, deny all client R/W except owner get)
├── wallets/{uid}                       — Wallet status
├── balance_snapshots/{uid}             — Balance state
├── ledger_transactions/{txId}          — Transaction log
├── ledger_entries/{entryId}            — Ledger entries
├── idempotency_keys/{keyId}            — Replay protection
├── purchases/{purchaseId}              — Purchase records
├── purchase_token_index/{indexId}      — Purchase dedup
├── payment_provider_events/{eventId}   — Payment events
├── fraud_signals/{signalId}            — Fraud signals
├── wallet_audit_logs/{logId}           — Wallet audit
└── admin_actions/{actionId}            — Admin action log

System Layer (fully server-only)
└── auditEvents/{eventId}              — System audit trail
```

---

## 3. Alternatives Considered and Rejected

### 3.1 Subcollection for private profile data
**Proposal:** `/users/{uid}/private/profile` instead of top-level `/privacy/{uid}`.
**Rejected because:** Firestore rules for subcollections inherit wildcard matches. Top-level collections provide clearer access boundaries and avoid accidental permission leakage from parent document rules.

### 3.2 Single document for profile + privacy
**Proposal:** Merge `profile_visuals` and `privacy` into one document.
**Rejected because:** Privacy settings must be owner-only readable. Profile visuals must be publicly readable (when searchable). Mixing them in one document would require field-level access control, which Firestore rules don't support.

### 3.3 Client-writable follow/block documents
**Proposal:** Let clients create follow/block documents directly.
**Rejected because:** Follow and block operations involve multi-document cascades (counter updates, request invalidation, bidirectional cleanup). These require transactions with Admin SDK. Client-side atomic batches cannot span the required read-modify-write patterns.

### 3.4 Server-side observation creation
**Proposal:** Move observation creation to Cloud Functions.
**Rejected because:** Observations are lightweight one-click actions. The client-side atomic batch (observation + counter increment + notification) is validated by Firestore rules, avoiding CF cold-start latency for a time-sensitive interaction. Rules enforce all invariants.

---

## 4. Security Properties by Layer

| Layer | Client write? | Client read? | Key protection |
|---|---|---|---|
| Identity | NO | Owner get only | Server-only writes |
| Configuration | Owner only | Owner only | Schema validation |
| Social graph | NO (except observations) | Participant only | CF transactions |
| Content | Owner create/delete | Privacy-gated | Schema + privacy + blocks |
| Messaging | Participant only | Participant only | Participant validation |
| Behavioral | Viewer only | Viewer only | Ownership enforcement |
| Financial | NO | Owner get (wallet/balance only) | Full deny + CF envelope |
| System | NO | NO | Complete deny |
