# Architecture Decision Record — Virtual Currency System

Date: 2026-05-24
Phase: 0 — Preparation
Prompt: 00_PREPARACION_02_DECISIONES_ARQUITECTURA
Status: Accepted

---

## ADR-01: Backend Framework — Firebase Cloud Functions (TypeScript)

**Decision**: All wallet, ledger, purchase, and admin endpoints will be implemented as Firebase Cloud Functions callable (`functions.https.onCall`).

**Context**: The plan assumes a dedicated backend server (Ktor / Spring Boot). The actual project uses Firebase Cloud Functions exclusively. All existing features (social graph, posts, storage) follow this pattern.

**Trade-offs**:
- (+) Zero additional infrastructure — same deploy pipeline, same auth model.
- (+) Consistent security envelope already proven: `requireAppCheck → requireAuth → validateInput → checkRateLimit → business logic`.
- (+) Admin SDK bypasses Firestore Rules — server is authoritative.
- (-) Cold starts add latency to first wallet call (~200-500ms).
- (-) Max execution time 60s (sufficient for wallet operations).
- (-) No persistent connections or WebSocket support — polling or Firestore snapshots for real-time balance.

**Security justification**: Cloud Functions run server-side. Client never has direct write access to wallet collections. All mutations go through the validated function pipeline. App Check ensures only legitimate app binaries can invoke functions.

---

## ADR-02: Database — Firestore (NoSQL) instead of PostgreSQL

**Decision**: All wallet, ledger, purchase, and audit data will be stored in Firestore collections. No PostgreSQL, no SQL migrations.

**Context**: The plan assumes PostgreSQL with SQL constraints, foreign keys, and row-level locking. The project uses Firestore for all data. Introducing PostgreSQL would require infrastructure (Cloud SQL), new connection management, and break the single-backend-service model.

**Adaptation strategy**:

| SQL Concept | Firestore Equivalent |
|---|---|
| Table | Collection |
| Row | Document |
| Primary key | Document ID |
| `UNIQUE` constraint | Composite document ID (e.g., `{walletId}_{idempotencyKey}`) |
| Foreign key | Application-level enforcement in Cloud Functions |
| `AUTO_INCREMENT` | UUID v4 or Firestore auto-ID |
| `SELECT FOR UPDATE` | `db().runTransaction()` (optimistic concurrency) |
| SQL migration | N/A (schemaless), enforce via `schemaVersion` field |
| `CHECK` constraint | Cloud Functions validation + Firestore Rules |
| `NOT NULL` | Cloud Functions validation (reject missing fields) |

**Trade-offs**:
- (+) No infrastructure changes, no connection pooling, no migrations.
- (+) Firestore transactions are ACID within a single document/transaction scope.
- (+) Automatic scaling, no capacity planning.
- (-) No relational joins — denormalization required for some queries.
- (-) Optimistic transactions may fail under high contention — requires retry with backoff.
- (-) No native `SUM()` aggregation — balance must be maintained as a snapshot updated in transactions.

**Security justification**: Firestore Rules provide defense-in-depth (deny all by default for new collections via catch-all rule). All write operations go through Cloud Functions with Admin SDK. The `/{document=**}` catch-all rule ensures new collections are locked until explicitly opened.

---

## ADR-03: Wallet Collections — Schema Design

**Decision**: Adapt the plan's 12 tables to Firestore collections with the following mapping:

### Core Wallet Collections

```
wallets/{walletId}
  - userId: string (indexed, unique via doc ID = userId)
  - status: "active" | "frozen" | "suspended"
  - createdAt: number (millis)
  - updatedAt: number (millis)
  - schemaVersion: 1
```

Document ID strategy: `walletId = userId` (one wallet per user, enforced by using userId as document ID).

```
balance_snapshots/{walletId}
  - walletId: string
  - availableBalance: number (integer, >= 0)
  - purchasedBalance: number (integer, >= 0)
  - promotionalBalance: number (integer, >= 0)
  - earnedBalance: number (integer, >= 0)
  - version: number (monotonically increasing, used for optimistic locking)
  - lastTransactionId: string
  - updatedAt: number (millis)
  - schemaVersion: 1
```

Document ID: `snapshotId = walletId` (one active snapshot per wallet).

```
ledger_transactions/{transactionId}
  - transactionId: string (UUID v4)
  - walletId: string
  - type: "purchase_credit" | "spend" | "refund" | "admin_adjustment" | "promotional_credit"
  - idempotencyKey: string
  - status: "confirmed" | "failed" | "reversed"
  - metadata: map (structured, type-specific)
  - createdAt: number (millis)
  - schemaVersion: 1
```

Uniqueness of `(walletId, idempotencyKey)`: enforced via a separate idempotency collection (see ADR-05).

```
ledger_entries/{entryId}
  - entryId: string (UUID v4)
  - transactionId: string
  - walletId: string
  - entryType: "credit" | "debit"
  - bucket: "purchased" | "promotional" | "earned"
  - amount: number (integer, != 0, positive for credit, negative for debit)
  - runningBalance: number (integer, >= 0, balance in this bucket after this entry)
  - createdAt: number (millis)
  - schemaVersion: 1
```

### Purchase Collections

```
purchases/{purchaseId}
  - purchaseId: string (UUID v4)
  - walletId: string
  - userId: string
  - provider: "sandbox" | "google_play" | "apple_iap" | "stripe"
  - providerPurchaseTokenHash: string (SHA-256, indexed)
  - providerTransactionId: string | null
  - packId: "coins_1" | "coins_10" | "coins_100"
  - coinsAmount: number (integer)
  - priceCents: number (integer, 0 for sandbox)
  - currency: string ("USD", "EUR", etc., "SANDBOX" for sandbox)
  - status: "initiated" | "verified" | "credited" | "failed" | "refunded"
  - ledgerTransactionId: string | null
  - createdAt: number (millis)
  - updatedAt: number (millis)
  - schemaVersion: 1
```

Uniqueness of `(provider, providerPurchaseTokenHash)`: enforced via composite document ID for the token lookup collection.

```
purchase_token_index/{provider}_{tokenHash}
  - purchaseId: string
  - provider: string
  - tokenHash: string
  - createdAt: number (millis)
```

Document ID = `{provider}_{tokenHash}` guarantees uniqueness.

```
payment_provider_events/{eventId}
  - eventId: string
  - provider: string
  - providerEventId: string
  - purchaseId: string | null
  - eventType: string
  - rawPayload: map (sanitized, no secrets)
  - processedAt: number (millis)
  - schemaVersion: 1
```

Uniqueness of `(provider, providerEventId)`: enforced via document ID = `{provider}_{providerEventId}`.

### Security & Audit Collections

```
idempotency_keys/{keyId}
  - key: string
  - walletId: string
  - action: string
  - requestHash: string (SHA-256 of canonical request body)
  - responseStatus: string
  - transactionId: string | null
  - createdAt: number (millis)
  - expiresAt: number (millis, TTL for cleanup)
  - schemaVersion: 1
```

Document ID = `{walletId}_{key}` guarantees per-wallet uniqueness.

```
fraud_signals/{signalId}
  - signalId: string
  - walletId: string
  - userId: string
  - signalType: string
  - riskScore: number
  - metadata: map
  - createdAt: number (millis)
```

```
wallet_audit_logs/{logId}
  - logId: string
  - actorId: string (userId or "system")
  - actorType: "user" | "admin" | "system"
  - action: string
  - targetWalletId: string
  - details: map
  - ip: string | null
  - createdAt: number (millis)
```

```
admin_actions/{actionId}
  - actionId: string
  - adminId: string
  - action: string
  - targetWalletId: string
  - amount: number | null
  - justification: string
  - status: "pending" | "approved" | "executed" | "rejected"
  - approverId: string | null
  - createdAt: number (millis)
  - resolvedAt: number (millis) | null
```

**Security justification**: All balance-related fields are integers (no floating point). Document IDs enforce uniqueness constraints that SQL would provide via `UNIQUE` indexes. The `purchase_token_index` collection prevents double-crediting the same purchase token. The `idempotency_keys` collection prevents replaying the same request.

---

## ADR-04: Transactions and Concurrency — Firestore Transactions with Optimistic Locking

**Decision**: All balance-mutating operations will run inside a Firestore transaction that:
1. Reads the `balance_snapshots` document.
2. Validates sufficient balance (for spend) or validates purchase state.
3. Checks `version` field for optimistic lock.
4. Writes `ledger_transaction`, `ledger_entries`, and updated `balance_snapshot` atomically.
5. Increments `version` by 1.

**Retry strategy**: Firestore transactions automatically retry on contention (up to 5 retries by default). The Cloud Function will catch `ABORTED` errors and return a retryable error code.

**Double-spend prevention**: The `version` field on `balance_snapshots` acts as a sequence number. Two concurrent spends both read version N, both try to write version N+1 — only one transaction commits, the other retries and re-reads the updated balance.

**Trade-offs**:
- (+) ACID within the transaction scope.
- (+) No external lock service needed.
- (+) Proven pattern (same as existing counter updates in social graph).
- (-) Under extreme contention (>100 concurrent ops on same wallet), transactions may exhaust retries.
- (-) Cross-document transactions are limited to 500 writes.

**Security justification**: Optimistic locking prevents double-spending without external coordination. The `version` field makes phantom reads impossible within a transaction. 100 simultaneous spends cannot deplete more than the actual balance — this is a testable invariant.

---

## ADR-05: Idempotency — Key-based Deduplication

**Decision**: Every balance-mutating endpoint requires an `Idempotency-Key` header (or field). Before executing, the function:
1. Looks up `idempotency_keys/{walletId}_{key}`.
2. If it exists: compares `requestHash` to SHA-256 of current request body.
   - Same hash → return cached result (no re-execution).
   - Different hash → reject with `409 Conflict` (key reuse with different body).
3. If not found: proceed, then write the key document atomically within the transaction.

**Key format**: Client-generated UUID v4. Max 128 characters. Validated with safe ID regex.

**TTL**: Idempotency keys expire after 24 hours. A scheduled Cloud Function will purge expired keys.

**Trade-offs**:
- (+) Prevents replay attacks and accidental double-submissions.
- (+) Allows safe client retries after network failures.
- (-) Adds one extra read per wallet operation.
- (-) Requires cleanup job for expired keys.

**Security justification**: Invariant #8 from the security spec: "A idempotency key cannot be reused with a different body." This prevents an attacker from using a known key to replay a different operation.

---

## ADR-06: Ledger — Append-Only with Snapshot Reconciliation

**Decision**: The ledger is strictly append-only:
- `ledger_transactions` and `ledger_entries` documents are never updated or deleted.
- Corrections are modeled as new reverse transactions (e.g., refund = new debit entry).
- `balance_snapshots` is the only mutable balance state, updated atomically in each transaction.
- Reconciliation: `snapshot.availableBalance == sum(ledger_entries.amount WHERE status=confirmed)`. This is a testable invariant.

**Firestore Rules enforcement**: `ledger_transactions` and `ledger_entries` collections will have `allow create, update, delete: if false` for client access. Only Cloud Functions (Admin SDK) can write.

**Trade-offs**:
- (+) Complete audit trail — every balance change has a ledger entry.
- (+) Reconstructible — snapshot can be rebuilt from entries.
- (+) Forensics — no data loss from overwrites.
- (-) Storage grows linearly with transactions (acceptable for current scale).
- (-) Reconciliation check requires reading all entries (periodic batch job, not real-time).

**Security justification**: Invariants #3, #4, #5 from the security spec. The append-only model ensures no historical manipulation. Even with Admin SDK access, the Cloud Functions code never calls `update()` or `delete()` on ledger documents.

---

## ADR-07: Payment Providers — Strategy Pattern with Fail-Closed Stubs

**Decision**: Implement a provider abstraction in TypeScript:

```typescript
interface PaymentProvider {
  readonly name: 'sandbox' | 'google_play' | 'apple_iap' | 'stripe';
  initiatePurchase(userId: string, packId: string, idempotencyKey: string): Promise<PurchaseInitiation>;
  verifyPurchase(purchaseId: string, providerToken: string): Promise<PurchaseVerification>;
}
```

Implementations:
- `SandboxPaymentProvider` — immediately returns `verified` with a deterministic test token. Active.
- `GooglePlayPaymentProvider` — **stub that throws** `'not-configured'` error. Fail-closed.
- `AppleIAPPaymentProvider` — **stub that throws** `'not-configured'` error. Fail-closed.
- `StripePaymentProvider` — **stub that throws** `'not-configured'` error. Fail-closed.

Provider selection: based on a `provider` field in the request. Only `sandbox` is routable. Others reject immediately.

**Pack catalog** (server-side, not client-supplied):

| packId | coins | priceCents | currency |
|---|---|---|---|
| `coins_1` | 1 | 0 | SANDBOX |
| `coins_10` | 10 | 0 | SANDBOX |
| `coins_100` | 100 | 0 | SANDBOX |

The `coinsAmount` and `priceCents` are **never accepted from the client**. The server looks up the pack catalog by `packId`.

**Trade-offs**:
- (+) Ready for real providers without structural changes.
- (+) Fail-closed stubs prevent accidental activation of unfinished payment flows.
- (+) Server-authoritative pricing eliminates client-side price tampering.
- (-) Sandbox provider can "mint coins" freely — must be behind rate limiting + fraud checks.

**Security justification**: Invariants #6, #14, #19 from the security spec. The client cannot send `amount`, `newBalance`, or price. The server determines coin quantity from the pack catalog. Real provider stubs throw errors, preventing any code path from accidentally crediting real-money purchases.

---

## ADR-08: API Contract — Callable Cloud Functions

**Decision**: Adapt the REST API contract to Firebase callable functions:

| Plan Endpoint | Cloud Function Name | Type |
|---|---|---|
| `GET /wallet/balance` | `getWalletBalance` | `onCall` |
| `GET /wallet/transactions` | `getWalletTransactions` | `onCall` |
| `POST /wallet/spend` | `spendCoins` | `onCall` |
| `POST /purchases/sandbox/initiate` | `initiateSandboxPurchase` | `onCall` |
| `POST /purchases/sandbox/verify` | `verifySandboxPurchase` | `onCall` |
| (future) webhook endpoints | `onGooglePlayWebhook`, etc. | `onRequest` (HTTP) |

Every callable follows the existing security envelope:
```
requireAppCheck → requireAuth → validateInput → checkRateLimit → business logic
```

Idempotency key is passed as a field in the `data` payload (not an HTTP header, since `onCall` doesn't expose custom headers directly).

**Client-side interaction** (KMP):
- Use `dev.gitlive:firebase-functions` KMP SDK to call functions.
- Balance displayed is always fetched from `getWalletBalance`, never computed locally.
- After purchase, client calls `verifySandboxPurchase` and then refreshes balance.

**Trade-offs**:
- (+) Same SDK on Android, iOS, and Desktop via firebase-functions KMP.
- (+) Auth token attached automatically by Firebase SDK.
- (-) No custom HTTP headers — idempotency key must be in request body.
- (-) No streaming responses — client must poll or use Firestore snapshot listeners for real-time updates.

**Security justification**: Invariant #9: no endpoint accepts arbitrary userId for balance modification. The `callerUid` is always extracted from `context.auth.uid`, never from request body. Invariant #18: no public endpoint allows arbitrary balance modification — all mutations go through validated functions with rate limiting.

---

## ADR-09: KMP Client UI — "Moneda" Screen

**Decision**: Add a new feature module at `shared/src/commonMain/kotlin/com/example/neuv3/features/wallet/`. Structure:

```
features/wallet/
  domain/
    WalletModels.kt          — data classes (WalletBalance, Transaction, PurchasePack)
  data/
    WalletRepository.kt      — interface
    FirebaseWalletRepository.kt — Firebase Functions calls
  presentation/
    WalletStateHolder.kt     — state management
    WalletScreen.kt           — Compose UI
```

Navigation:
- Add `AppRoute.Wallet(user: AuthenticatedUser)` to `AppRoute.kt`.
- Add navigation entry in `HomeScreen` (new "Moneda" button/card).
- Wire in `App.kt` routing.

**Balance display rule**: The client fetches balance from `getWalletBalance` on screen open and after every purchase. The UI shows a loading state until the server responds. No local balance cache is used as source of truth.

**Purchase flow**:
1. User taps "Buy 10 coins" button.
2. Client calls `initiateSandboxPurchase({ packId: "coins_10", idempotencyKey: uuid })`.
3. Server validates, creates purchase record, immediately verifies (sandbox), credits ledger, returns result.
4. Client refreshes balance from `getWalletBalance`.

**Trade-offs**:
- (+) Balance always reflects server state.
- (+) Simple UX — sandbox purchase is instant.
- (-) Requires network for every balance check (no offline mode for wallet).
- (-) Purchase flow is slightly different for real providers (async verification).

**Security justification**: Invariant #1: client never stores authoritative balance. Invariant #13: no local balance that could diverge from server. The UI can show an optimistic update (immediately increment displayed balance) but must reconcile with server on next fetch.

---

## ADR-10: Testing Strategy

**Decision**: Three test layers:

### Layer 1: Cloud Functions unit tests (Jest)
- New test file: `functions/src/wallet.test.ts`
- Tests: auth enforcement, input validation, idempotency, balance integrity, concurrent spending, purchase token uniqueness, rate limiting, fraud signal generation.
- Pattern: same mock structure as existing `security.test.ts`.

### Layer 2: KMP shared tests (kotlin-test)
- New test file: `shared/src/commonTest/.../wallet/WalletModelsTest.kt`
- Tests: model validation, state transitions, amount parsing, pack catalog correctness.

### Layer 3: Integration / invariant tests
- Reconciliation test: `snapshot.available == sum(ledger entries)`.
- Double-spend test: 100 concurrent spend attempts cannot exceed balance.
- Purchase dedup test: 100 concurrent verifications of same token credit once.
- Idempotency test: same key + same body = same result; same key + different body = 409.

**Trade-offs**:
- (+) Covers security invariants explicitly.
- (+) Catches concurrency bugs before deploy.
- (-) Integration tests need Firestore emulator (adds setup complexity).
- (-) Concurrency tests are inherently non-deterministic (mitigated by high iteration count).

---

## ADR-11: Firestore Rules — Defense in Depth

**Decision**: New collections will have the following Firestore Rules:

```
// All wallet/ledger collections: locked for clients
match /wallets/{walletId} {
  allow get: if isOwner(walletId);  // read own wallet status
  allow list, create, update, delete: if false;
}

match /balance_snapshots/{snapshotId} {
  allow get: if isOwner(snapshotId);  // read own balance
  allow list, create, update, delete: if false;
}

match /ledger_transactions/{txId} {
  allow read, write: if false;  // only via Cloud Functions
}

match /ledger_entries/{entryId} {
  allow read, write: if false;  // only via Cloud Functions
}

match /purchases/{purchaseId} {
  allow read, write: if false;  // only via Cloud Functions
}

// ... same pattern for all other wallet collections
```

The existing catch-all `/{document=**} → deny` already covers new collections by default. Explicit rules are added only for read access where the client needs it (balance, wallet status).

**Security justification**: Even if a bug in Cloud Functions accidentally exposed a write path, Firestore Rules would block direct client writes. Belt and suspenders.

---

## ADR-12: Rate Limiting for Wallet Operations

**Decision**: Extend the existing `RATE_LIMITS` in `rateLimiter.ts`:

| Action | maxCalls | windowMs | Rationale |
|---|---|---|---|
| `getBalance` | 30 | 60,000 | Prevent polling abuse |
| `spendCoins` | 20 | 60,000 | Economic operation |
| `initiatePurchase` | 10 | 60,000 | Purchase initiation |
| `verifyPurchase` | 10 | 60,000 | Purchase verification |

**Security justification**: Rate limiting prevents automated coin minting via sandbox and brute-force spending attempts.

---

## ADR-13: Audit Logging for Economic Operations

**Decision**: Every balance-mutating Cloud Function will create a `wallet_audit_logs` document with:
- Actor identity (from `context.auth.uid`)
- Action name
- Target wallet
- Amount (if applicable)
- Idempotency key
- Timestamp
- Result status

Audit logs are append-only (Cloud Functions only, no client access). They are separate from `ledger_transactions` — the ledger is the economic truth, audit logs capture the access/intent pattern.

**Security justification**: Invariant #10 (admin audit) and general forensic requirement. Audit logs allow detection of anomalous patterns (e.g., user calling `spendCoins` 100 times in a minute despite rate limiting passing).

---

## Summary of Decisions

| # | Decision | Key Trade-off |
|---|---|---|
| ADR-01 | Cloud Functions backend | No new infra, but cold starts |
| ADR-02 | Firestore instead of PostgreSQL | No migrations, but no SQL features |
| ADR-03 | Document-based wallet schema | Composite IDs for uniqueness |
| ADR-04 | Optimistic locking via version field | Automatic retry, but contention limit |
| ADR-05 | Idempotency key collection | Replay protection, extra read per op |
| ADR-06 | Append-only ledger + snapshot | Full audit trail, linear storage growth |
| ADR-07 | Strategy pattern with fail-closed stubs | Ready for real providers, sandbox risk |
| ADR-08 | Callable functions, not REST | Same SDK everywhere, no custom headers |
| ADR-09 | Server-fetched balance in KMP UI | Always accurate, requires network |
| ADR-10 | Three-layer test strategy | Coverage vs setup complexity |
| ADR-11 | Deny-by-default Firestore Rules | Defense in depth |
| ADR-12 | Per-action rate limits | Abuse prevention |
| ADR-13 | Separate audit log collection | Forensics beyond ledger |
