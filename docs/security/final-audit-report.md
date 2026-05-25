# NEU Wallet — Final Red Team Audit Report

**Date:** 2026-05-25
**Phase:** 9 (Auditoría final)
**Scope:** Full Red Team review of the virtual-currency subsystem prior to beta gate
**Tester:** Phase 9 Prompt 1 (automated grep + per-callable + per-service)

---

## Executive summary

| Severity      | Count |
|---------------|-------|
| CRITICAL      | 0     |
| HIGH          | 1     |
| MEDIUM        | 3     |
| LOW           | 3     |
| INFORMATIONAL | 6     |

**Overall posture:** the wallet implementation satisfies all 11 mandatory invariants
documented in `ESPECIFICACION_SEGURIDAD_INVARIANTES.md`. No CRITICAL findings.
One HIGH finding (admin endpoints lacking rate limiting) MUST be remediated
before the release gate.

The system already implements: server-authoritative balance, append-only ledger,
mandatory idempotency on every economic operation, fail-closed rate limiting on
user-facing economic endpoints, audit logging through a single typed service,
fraud risk scoring with auto-freeze, maker/checker admin adjustments, and
fail-closed real-provider webhooks. KMP client never computes balance.

---

## Methodology

**Cross-cutting greps run:**
1. `in` operator against object literals (prototype-chain bypass)
2. Direct snapshot mutations outside `LedgerService`
3. Client-controlled `walletId`/`userId`/`amount` references
4. Callables missing `requireAuth`/`requireAppCheck`/`checkRateLimitFailClosed`
5. Silent `catch (e) {}` blocks
6. `console.*` in production code
7. `data.amount`, `data.coinsAmount`, `data.balance` patterns
8. Spread of untrusted input into trusted objects
9. KMP client computing balance locally

**Per-callable review:**
- `getWalletBalance`, `spendCoins`, `getWalletTransactions`,
  `initiatePurchase`, `verifyPurchase`
- `adminRequestWalletAdjustment`, `adminApproveWalletAdjustment`
- `webhookGooglePlay`, `webhookAppStore`, `webhookStripe`

**Per-service review:**
- `LedgerService`, `FraudSignalService`, `RiskScoringService`,
  `AuditLogService`, `AdminAdjustmentService`, `ReconciliationService`

**Firestore Rules review:** all 10 wallet collections.

---

## Findings

### CRITICAL

*(none)*

---

### HIGH

#### H1 — Admin callables have NO rate limiting

- **File:** `functions/src/adminFunctions.ts`
- **Lines:** 102–166
- **Discovered by:** grep for `checkRateLimit` in `adminFunctions.ts` → no matches.

**Problem.** The two admin callables (`adminRequestWalletAdjustment`,
`adminApproveWalletAdjustment`) do NOT call `checkRateLimitFailClosed`.

A compromised or insider admin account can:
- Spam `adminRequestWalletAdjustment` to create thousands of `pending` rows
  in `admin_actions`, polluting the audit pipeline and burning Firestore quota.
- Spam `adminApproveWalletAdjustment` with fabricated `actionId`s.

The threat model for a wallet feature MUST treat insider abuse as in-scope.
Every other economic endpoint in the codebase already has fail-closed rate
limiting; admin endpoints are the one gap.

**Severity rationale.** HIGH (not CRITICAL) because:
- Requires a valid admin custom claim (so external attacker cannot exploit
  without first compromising an admin).
- All approval attempts are audited and the maker-≠-checker guard still holds.
- Auto-freeze on aggregate suspicious activity already runs.

But still HIGH because:
- Adding rate limiting is a one-line change with existing infrastructure.
- The release gate explicitly requires "rate limits" — leaving this open
  would fail the checklist.

**Remediation.** Add `RATE_LIMITS` entries for the two admin actions and
`checkRateLimitFailClosed` calls after `requireAdmin`. See Prompt 2 (REM-H1).

---

### MEDIUM

#### M1 — `computeBalancesFromEntries` uses `in` operator (prototype-chain bypass)

- **File:** `functions/src/lib/reconciliationService.ts`
- **Line:** 47
- **Discovered by:** grep `\bin\b\s+result` → matched the reconciliation code.

**Problem.** Same class of bug as Phase 8's `__proto__` finding in
`validateInitiatePurchaseInput`:

```typescript
for (const entry of entries) {
  if (entry.bucket in result) {                       // ← prototype-chain check
    result[entry.bucket as LedgerBucket] += entry.amount;
  }
}
```

If a ledger entry document existed with `bucket = '__proto__'`,
`'constructor'`, `'toString'`, etc., the `in` check would pass. The line
that follows would then attempt
`result['__proto__' as LedgerBucket] += entry.amount` — coercing
`Object.prototype.__proto__ += number` (`NaN`) at runtime, silently corrupting
the reconciliation result.

**Exploitation prerequisites.**
- Attacker must first cause a malformed `bucket` field to be written to
  `ledger_entries`. Since `LedgerService.applyInTx` types `bucket` as
  `LedgerBucket = 'purchased' | 'promotional' | 'earned'` and all callers
  go through validated entries, runtime injection is not currently possible.
- HOWEVER: TypeScript types are erased at runtime. A future bug, a manual
  Firestore admin write, or a misuse of the Admin SDK could introduce a bad
  bucket. Defense-in-depth requires the consumer to validate.

**Severity rationale.** MEDIUM, not HIGH, because exploitation requires
write access to `ledger_entries` (which is `read, write: if false` for clients).
But same class as the H finding closed in Phase 8 — must be remediated for
consistency.

**Remediation.** Replace `in` with `Object.prototype.hasOwnProperty.call(...)`.
See Prompt 2 (REM-M1).

---

#### M2 — Webhook handlers have NO rate limiting (sandbox path)

- **File:** `functions/src/webhooks.ts`
- **Lines:** 100–162 (sandbox path) and 171–193 (exports)
- **Discovered by:** per-callable review.

**Problem.** When `SANDBOX_PAYMENTS_ENABLED=true`, the three webhook
handlers accept any well-formed payload from any IP and store it in
`payment_provider_events`. There is no per-IP, per-provider, or global
rate limit.

In production (sandbox flag NOT set), all three webhooks `return 403`
unconditionally — no exposure.

In sandbox / dev environments (the only ones where the flag should be on)
an external scanner that finds the URL can fill the collection by sending
unique `providerEventId` values.

**Severity rationale.** MEDIUM. In sandbox the data is throwaway and the
impact is quota burn. But this is a release-gate item ("rate limits"
checklist).

**Remediation.** Defer to Phase 7+ infra work (Cloud Armor / per-IP throttle).
For this audit, document in `release-gate.md` as a known limitation of
sandbox mode. NOT blocking for GO since real provider paths are fail-closed.

---

#### M3 — Reconciliation job not scheduled

- **Files:** `functions/src/lib/reconciliationService.ts` (exists),
  `functions/src/index.ts` (no schedule export).
- **Discovered by:** grep `reconcile` → no scheduled function trigger.

**Problem.** `ReconciliationService.reconcileWallet` exists and is tested,
but no Cloud Scheduler / pubsub trigger invokes it periodically. Snapshot
vs ledger drift could go undetected.

**Severity rationale.** MEDIUM. The accounting model is designed to be
self-consistent (snapshot derived from ledger by `LedgerService`), so drift
is unlikely. But for fintech the principle "trust but verify" applies —
periodic reconciliation is a checklist requirement.

**Remediation.** Add a scheduled function. This is a small piece of work
that I am classifying as out-of-scope for Phase 9 Prompt 2 (which only
remediates CRITICAL/HIGH per its own rules). Document in release gate as
"required pre-public-beta" but not blocking for sandbox beta.

---

### LOW

#### L1 — Real-provider webhooks return 403 even for malformed payload

- **File:** `functions/src/webhooks.ts:80–98`.
- **By design.** Fail-closed wins over diagnostic feedback. No action.

#### L2 — Structured-logger output format depends on `firebase-functions.logger` defaults

- **File:** `functions/src/lib/walletLogger.ts`
- **Observation.** Log entries are emitted via `functions.logger.info` etc.,
  which serialize as JSON in Cloud Functions but not necessarily elsewhere
  (local emulator outputs human-readable lines). Trace correlation works
  in production but local debugging may be noisier.
- **Action.** None for this audit.

#### L3 — No documented runbook for admin role rotation

- **Observation.** Admin custom claims are set via Admin SDK
  (`admin.auth().setCustomUserClaims(uid, { admin_role: 'admin' })`) but no
  runbook documents who can do this or rotation cadence.
- **Action.** Add to `docs/security/release-gate.md` as a known gap.

---

### INFORMATIONAL (positive findings)

I1. **Firestore Rules deny all client writes to wallet collections.**
   Verified for: `ledger_transactions`, `ledger_entries`, `idempotency_keys`,
   `purchases`, `purchase_token_index`, `payment_provider_events`,
   `fraud_signals`, `wallet_audit_logs`, `admin_actions`. Users can `get`
   their own wallet status and balance snapshot only (no `list`).

I2. **No callable accepts client-supplied `walletId`/`userId`/`amount`.**
   `walletId = callerUid` is enforced in every wallet callable
   (`walletFunctions.ts` grep confirms). `FORBIDDEN_CLIENT_FIELDS` in
   `walletValidators.ts` denies these fields at validation time.

I3. **All wallet economic callables have the full security envelope.**
   `requireAppCheck → requireAuth → validateInput →
   checkRateLimitFailClosed → enforceRiskCheck → idempotency pre-check →
   transaction → idempotency complete`. Verified for all 5 callables.

I4. **KMP client never computes balance locally.**
   `WalletStateHolder.buyPack` calls `client.getBalance()` after `verify`
   and uses the server response verbatim. Test
   `buyPack_balanceComesFromServer_notFromVerifyResult` enforces this.

I5. **Audit log service redacts sensitive keys recursively.**
   `sanitizeDetails` redacts `token`, `secret`, `password`, `credential`,
   `authorization`, `apikey`, `accessToken`, `refreshToken`, `idToken`,
   `sessionid`. Test `wallet-audit-service.test.ts` (35 cases) verifies.

I6. **No `console.*` in production code.**
   All logging goes through `walletLog` which sanitizes sensitive values.

---

## Mapping to release-gate checklist

| Gate item              | Status                                                |
|------------------------|-------------------------------------------------------|
| Tests pass             | 691/691 backend, 165/165 Kotlin — ✅                  |
| Migrations             | N/A (Firestore schemaless) — ✅                       |
| Config separated       | `.env.example` committed; `.env` gitignored — ✅      |
| Secrets                | None in source (scan-secrets PASS) — ✅               |
| Rate limits            | User callables ✅; admin callables ❌ (H1)            |
| Sandbox flag           | `SANDBOX_PAYMENTS_ENABLED` documented + tested — ✅   |
| Admin RBAC             | Deny-by-default + escalation guard tested — ✅        |
| Audit logs             | Append-only via single service; sanitized — ✅        |
| Reconciliation         | Service exists; no scheduler (M3) — ⚠️                |
| Backups docs           | TBD — document in release-gate.md — ⚠️                |

---

## Recommendation

**Block release until H1 is remediated.** All other findings are MEDIUM/LOW
and acceptable for a sandbox beta with the M3 and "backup docs" gaps
explicitly called out in the release gate.

Proceed to Prompt 2 to remediate H1 and M1, then to Prompt 3 to issue the
GO/NO-GO decision.
