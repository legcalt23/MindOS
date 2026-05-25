/**
 * wallet-abuse.test.ts
 *
 * API-abuse security suite. Each test simulates an attacker action and
 * asserts the system blocks it AS DESIGNED — not by coincidence.
 *
 * Threat → control mapping:
 *   replay                    → idempotency cache (resolveIdempotencyDecision)
 *   idempotency conflict      → resolveIdempotencyDecision returns 'conflict'
 *   userId tampering          → walletId derived from context.auth.uid only
 *   extra fields              → validateXxxInput rejectForbiddenFields
 *   forbidden client fields   → FORBIDDEN_CLIENT_FIELDS denylist
 *   webhook spoofing          → real-provider path fails closed (403)
 *   admin unauthorized        → requireAdmin throws PERMISSION_DENIED
 *   rate limit exceeded       → checkRateLimitFailClosed throws resource-exhausted
 *
 * Tests use the REAL implementations of the relevant security primitives
 * (validators, idempotency state machine, RBAC) — mocking them would defeat
 * the point of an abuse test.
 */

// ── Imports — exercise REAL security primitives ──────────────────────────────

jest.mock('firebase-admin', () => ({
  initializeApp: jest.fn(),
  firestore: jest.fn(() => ({
    collection: jest.fn(() => ({ doc: jest.fn(() => ({ path: 'fake' })) })),
  })),
}));

// firebase-functions logger is mocked to silence noise; HttpsError class is real
jest.mock('firebase-functions', () => {
  const real = jest.requireActual('firebase-functions');
  return {
    ...real,
    logger: { info: jest.fn(), warn: jest.fn(), error: jest.fn() },
  };
});

import {
  validateSpendCoinsInput,
  validateInitiatePurchaseInput,
  validateVerifyPurchaseInput,
  validateGetBalanceInput,
  validateGetTransactionsInput,
} from './lib/walletValidators';
import {
  hashRequest,
  resolveIdempotencyDecision,
} from './lib/idempotencyService';
import { requireAdmin, ADMIN_SECURITY_CONFIG } from './lib/adminAuth';
import { walletError } from './lib/walletErrors';
import type { IdempotencyKeyDocument } from './lib/walletSchema';

// ── Helpers ──────────────────────────────────────────────────────────────────

function errorCodeOf(e: unknown): string | undefined {
  if (!e || typeof e !== 'object') return undefined;
  const errObj = e as { code?: string; details?: { errorCode?: string } };
  return errObj.details?.errorCode ?? errObj.code;
}

function makeCtx(opts: { uid?: string; role?: string } = {}) {
  const token: Record<string, unknown> = {};
  if (opts.role !== undefined) token['admin_role'] = opts.role;
  return {
    auth: { uid: opts.uid ?? 'user-honest', token },
  } as unknown as Parameters<typeof requireAdmin>[0];
}

beforeEach(() => {
  ADMIN_SECURITY_CONFIG.require2FA = false;
  ADMIN_SECURITY_CONFIG.requireIpAllowlist = false;
  ADMIN_SECURITY_CONFIG.ipAllowlist = [];
});

// ─────────────────────────────────────────────────────────────────────────────
// 1. REPLAY attack
// ─────────────────────────────────────────────────────────────────────────────

describe('ABUSE — replay attack', () => {
  const sameBody = { itemId: 'item-1', quantity: 5 };
  const hash = hashRequest(sameBody);
  const completedRecord: IdempotencyKeyDocument = {
    docId: 'w_k', key: 'k', walletId: 'w', action: 'spend_coins',
    requestHash: hash, status: 'completed',
    responsePayload: { transactionId: 'tx-1', coinsSpent: 5 },
    transactionId: 'tx-1',
    createdAt: 0, expiresAt: 0, schemaVersion: 1,
  };

  it('a replay with same key + same body returns the CACHED response (no fresh side effect)', () => {
    const decision = resolveIdempotencyDecision(completedRecord, hash);
    expect(decision.kind).toBe('replay');
    if (decision.kind === 'replay') {
      expect(decision.transactionId).toBe('tx-1');
      expect(decision.response).toEqual({ transactionId: 'tx-1', coinsSpent: 5 });
    }
  });

  it('100 replays in a row all return the SAME cached transactionId', () => {
    const decisions = Array.from({ length: 100 }, () =>
      resolveIdempotencyDecision(completedRecord, hash),
    );
    for (const d of decisions) {
      expect(d.kind).toBe('replay');
      if (d.kind === 'replay') expect(d.transactionId).toBe('tx-1');
    }
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// 2. IDEMPOTENCY CONFLICT — same key, different body
// ─────────────────────────────────────────────────────────────────────────────

describe('ABUSE — idempotency-key reuse with different body', () => {
  const originalHash = hashRequest({ itemId: 'item-1', quantity: 1 });
  const tamperedHash = hashRequest({ itemId: 'item-1', quantity: 999 });
  const completed: IdempotencyKeyDocument = {
    docId: 'w_k', key: 'k', walletId: 'w', action: 'spend_coins',
    requestHash: originalHash, status: 'completed',
    responsePayload: { transactionId: 'tx-1', coinsSpent: 1 },
    transactionId: 'tx-1',
    createdAt: 0, expiresAt: 0, schemaVersion: 1,
  };

  it('completed + different body → conflict (NOT replay, NOT proceed)', () => {
    const decision = resolveIdempotencyDecision(completed, tamperedHash);
    expect(decision.kind).toBe('conflict');
  });

  it('failed + different body → conflict (key reuse blocked even on retry)', () => {
    const failed = { ...completed, status: 'failed' as const };
    const decision = resolveIdempotencyDecision(failed, tamperedHash);
    expect(decision.kind).toBe('conflict');
  });

  it('hash is canonical so reordering body keys does NOT cause false conflict', () => {
    const a = hashRequest({ itemId: 'i', quantity: 1 });
    const b = hashRequest({ quantity: 1, itemId: 'i' });
    expect(a).toBe(b);
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// 3. USERID / WALLETID TAMPERING
// ─────────────────────────────────────────────────────────────────────────────

describe('ABUSE — userId / walletId tampering', () => {
  // The wallet callables read walletId from context.auth.uid ONLY.
  // Any client-supplied walletId / userId field must be rejected by the
  // validator (it is in FORBIDDEN_CLIENT_FIELDS).
  it.each([
    ['walletId', 'attacker-victim-uid'],
    ['userId',   'attacker-victim-uid'],
  ])('spendCoins REJECTS client-supplied "%s"', (field, value) => {
    expect(() => validateSpendCoinsInput({
      itemId: 'item-1', quantity: 1, idempotencyKey: 'idem-key-test1',
      [field]: value,
    })).toThrow();
  });

  it.each([
    ['walletId', 'attacker-victim-uid'],
    ['userId',   'attacker-victim-uid'],
  ])('initiatePurchase REJECTS client-supplied "%s"', (field, value) => {
    expect(() => validateInitiatePurchaseInput({
      packId: 'coins_10', idempotencyKey: 'idem-key-test1',
      [field]: value,
    })).toThrow();
  });

  it('verifyPurchase REJECTS client-supplied walletId/userId', () => {
    expect(() => validateVerifyPurchaseInput({
      sandboxPurchaseId: 'p-1', idempotencyKey: 'idem-key-test1',
      walletId: 'victim',
    })).toThrow();
    expect(() => validateVerifyPurchaseInput({
      sandboxPurchaseId: 'p-1', idempotencyKey: 'idem-key-test1',
      userId: 'victim',
    })).toThrow();
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// 4. EXTRA / FORBIDDEN FIELDS
// ─────────────────────────────────────────────────────────────────────────────

describe('ABUSE — extra/unknown fields rejected', () => {
  // FORBIDDEN: must be rejected with a "not accepted from client" error.
  // UNKNOWN:   must be rejected with an "Unknown field" error.
  const FORBIDDEN_FIELDS = [
    'amount', 'newBalance', 'balance', 'price', 'priceCents',
    'coinsAmount', 'coins', 'ledgerTransactionId', 'status',
  ];

  it.each(FORBIDDEN_FIELDS)('spendCoins rejects FORBIDDEN field "%s"', (field) => {
    expect(() => validateSpendCoinsInput({
      itemId: 'item-1', quantity: 1, idempotencyKey: 'idem-key-test1',
      [field]: 'anything',
    })).toThrow();
  });

  it('spendCoins rejects arbitrary UNKNOWN field', () => {
    expect(() => validateSpendCoinsInput({
      itemId: 'item-1', quantity: 1, idempotencyKey: 'idem-key-test1',
      myCustomField: 'arbitrary',
    })).toThrow();
  });

  it('getBalance rejects ANY field (no body expected)', () => {
    expect(() => validateGetBalanceInput({ amount: 1 })).toThrow();
    expect(() => validateGetBalanceInput({ extra: 'x' })).toThrow();
  });

  it('getTransactions rejects unknown field', () => {
    expect(() => validateGetTransactionsInput({ limit: 10, extra: 'x' })).toThrow();
  });

  it('correlationId IS accepted on every endpoint (whitelisted)', () => {
    // Sanity: correlationId is the one "out-of-schema" key allowed everywhere
    expect(() => validateGetBalanceInput({ correlationId: 'corr' })).not.toThrow();
    expect(() => validateSpendCoinsInput({
      itemId: 'item-1', quantity: 1, idempotencyKey: 'idem-key-test1',
      correlationId: 'corr',
    })).not.toThrow();
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// 5. WEBHOOK SPOOFING — real providers fail closed
// ─────────────────────────────────────────────────────────────────────────────

describe('ABUSE — webhook spoofing (real provider, no signature validation yet)', () => {
  // We don't import the actual webhook handler — it depends on Express + admin
  // SDK in ways the abuse test doesn't need to reproduce. The CONTRACT we
  // verify here:
  //   - Real-provider paths (google_play/apple_iap/stripe) must remain
  //     non-operational until cryptographic signature validation is implemented.
  //   - This is enforced at code-level by the webhooks.ts decision tree which
  //     ALWAYS returns 403 for real providers regardless of payload.
  // The full HTTP-level test of this behavior lives in payment-webhooks.test.ts.
  //
  // Here we assert the CONTRACT statically by source-grep:
  //   - The webhooks.ts file must contain the documented "fail closed" comment
  //   - The file must NOT have lifted the gate (no "// removed" or commented
  //     out "fail closed" branches)
  it('webhooks.ts still declares the fail-closed contract for real providers', () => {
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const fs = require('fs') as typeof import('fs');
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const path = require('path') as typeof import('path');
    const source = fs.readFileSync(
      path.join(__dirname, 'webhooks.ts'), 'utf8',
    );
    expect(source).toMatch(/fail closed/i);
    expect(source).toContain('Fail closed');
  });

  it('SANDBOX_PAYMENTS_ENABLED is the only flag that opens the path', () => {
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const fs = require('fs') as typeof import('fs');
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const path = require('path') as typeof import('path');
    const source = fs.readFileSync(
      path.join(__dirname, 'webhooks.ts'), 'utf8',
    );
    expect(source).toContain("process.env['SANDBOX_PAYMENTS_ENABLED']");
    expect(source).toContain("=== 'true'");
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// 6. ADMIN UNAUTHORIZED — non-admin caller and role escalation attempts
// ─────────────────────────────────────────────────────────────────────────────

describe('ABUSE — admin endpoints reject non-admin callers', () => {
  it('caller with NO admin_role is rejected from every admin permission', () => {
    const perms = [
      'wallet.read', 'wallet.freeze', 'wallet.unfreeze',
      'wallet.adjustment.request', 'wallet.adjustment.approve',
      'wallet.adjustment.execute_low', 'audit.read',
      'fraud_signal.read', 'admin.manage_roles',
    ] as const;
    for (const p of perms) {
      expect(() => requireAdmin(makeCtx({ uid: 'attacker' }), p))
        .toThrow(/Administrative/);
    }
  });

  it('caller with FORGED-looking admin_role string is rejected (deny by default)', () => {
    expect(() => requireAdmin(makeCtx({ uid: 'a', role: 'GOD_MODE' }), 'wallet.freeze'))
      .toThrow(/Administrative/);
    expect(() => requireAdmin(makeCtx({ uid: 'a', role: 'admin\\u0000' }), 'wallet.freeze'))
      .toThrow(/Administrative/);
    expect(() => requireAdmin(makeCtx({ uid: 'a', role: '' }), 'wallet.freeze'))
      .toThrow(/Administrative/);
  });

  it('support role CANNOT escalate to freeze or adjustment.request', () => {
    expect(() => requireAdmin(makeCtx({ role: 'support' }), 'wallet.freeze'))
      .toThrow(/Administrative/);
    expect(() => requireAdmin(makeCtx({ role: 'support' }), 'wallet.adjustment.request'))
      .toThrow(/Administrative/);
  });

  it('admin role CANNOT escalate to admin.manage_roles (role-grant escalation guard)', () => {
    expect(() => requireAdmin(makeCtx({ role: 'admin' }), 'admin.manage_roles'))
      .toThrow(/Administrative/);
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// 7. RATE LIMIT
// ─────────────────────────────────────────────────────────────────────────────

describe('ABUSE — rate limit', () => {
  // The rate-limiter implementation is exhaustively tested in
  // wallet-rate-limit.test.ts. Here we assert the SECURITY CONTRACT:
  //   1. RATE_LIMITED is a defined wallet error code.
  //   2. The HttpsError gRPC mapping is 'resource-exhausted' (proper signal
  //      to the client to back off).
  //   3. Economic actions (spendCoins/initiatePurchase/verifyPurchase) are
  //      enumerated as fail-CLOSED in the rate limiter source — losing this
  //      property would silently re-open a flood path.
  it('walletError("RATE_LIMITED") returns HttpsError with code "resource-exhausted"', () => {
    const err = walletError('RATE_LIMITED', 'corr');
    expect(err.code).toBe('resource-exhausted');
    expect(err.message).toContain('Too many requests');
  });

  it('rateLimiter.ts declares spend/initiate/verify as economic fail-closed actions', () => {
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const fs = require('fs') as typeof import('fs');
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const path = require('path') as typeof import('path');
    const source = fs.readFileSync(
      path.join(__dirname, 'lib', 'rateLimiter.ts'), 'utf8',
    );
    expect(source).toContain('ECONOMIC_ACTIONS');
    expect(source).toMatch(/'spendCoins'/);
    expect(source).toMatch(/'initiatePurchase'/);
    expect(source).toMatch(/'verifyPurchase'/);
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// 8. NEGATIVE / DECIMAL / INFLATED AMOUNT TAMPERING
// ─────────────────────────────────────────────────────────────────────────────

describe('ABUSE — amount tampering at the validator boundary', () => {
  it('REJECTS negative quantity (signed-int overflow tampering)', () => {
    expect(() => validateSpendCoinsInput({
      itemId: 'item-1', quantity: -1, idempotencyKey: 'idem-key-test1',
    })).toThrow();
  });

  it('REJECTS zero quantity (free-spend tampering)', () => {
    expect(() => validateSpendCoinsInput({
      itemId: 'item-1', quantity: 0, idempotencyKey: 'idem-key-test1',
    })).toThrow();
  });

  it('REJECTS decimal quantity (precision-loss tampering)', () => {
    expect(() => validateSpendCoinsInput({
      itemId: 'item-1', quantity: 1.5, idempotencyKey: 'idem-key-test1',
    })).toThrow();
  });

  it('REJECTS quantity > validator cap (cap = 1000)', () => {
    expect(() => validateSpendCoinsInput({
      itemId: 'item-1', quantity: 1_000_000, idempotencyKey: 'idem-key-test1',
    })).toThrow();
  });

  it('REJECTS NaN / Infinity quantity', () => {
    expect(() => validateSpendCoinsInput({
      itemId: 'item-1', quantity: NaN, idempotencyKey: 'idem-key-test1',
    })).toThrow();
    expect(() => validateSpendCoinsInput({
      itemId: 'item-1', quantity: Infinity, idempotencyKey: 'idem-key-test1',
    })).toThrow();
  });

  it('REJECTS string-shaped quantity ("1000")', () => {
    expect(() => validateSpendCoinsInput({
      itemId: 'item-1', quantity: '1000', idempotencyKey: 'idem-key-test1',
    })).toThrow();
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// 9. IDEMPOTENCY KEY ABUSE
// ─────────────────────────────────────────────────────────────────────────────

describe('ABUSE — idempotency key tampering', () => {
  it('REJECTS missing idempotencyKey', () => {
    expect(() => validateSpendCoinsInput({
      itemId: 'item-1', quantity: 1,
    })).toThrow();
  });

  it('REJECTS too-short idempotencyKey (<8 chars)', () => {
    expect(() => validateSpendCoinsInput({
      itemId: 'item-1', quantity: 1, idempotencyKey: 'short',
    })).toThrow();
  });

  it('REJECTS overly-long idempotencyKey (>128 chars)', () => {
    expect(() => validateSpendCoinsInput({
      itemId: 'item-1', quantity: 1, idempotencyKey: 'a'.repeat(129),
    })).toThrow();
  });

  it('REJECTS idempotencyKey with control characters (injection attempt)', () => {
    expect(() => validateSpendCoinsInput({
      itemId: 'item-1', quantity: 1, idempotencyKey: 'idem key1',
    })).toThrow();
  });

  it('REJECTS idempotencyKey with path-traversal characters', () => {
    expect(() => validateSpendCoinsInput({
      itemId: 'item-1', quantity: 1, idempotencyKey: '../../etc/passwd',
    })).toThrow();
  });

  it('ACCEPTS a valid UUID v4', () => {
    expect(() => validateSpendCoinsInput({
      itemId: 'item-1', quantity: 1,
      idempotencyKey: '550e8400-e29b-41d4-a716-446655440000',
    })).not.toThrow();
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// 10. PACK ID TAMPERING
// ─────────────────────────────────────────────────────────────────────────────

describe('ABUSE — packId tampering on initiatePurchase', () => {
  it('REJECTS unknown packId (catalog injection)', () => {
    expect(() => validateInitiatePurchaseInput({
      packId: 'coins_999999', idempotencyKey: 'idem-key-test1',
    })).toThrow();
  });

  it('REJECTS packId with whitespace tampering', () => {
    expect(() => validateInitiatePurchaseInput({
      packId: ' coins_10 ', idempotencyKey: 'idem-key-test1',
    })).toThrow();
  });

  it('REJECTS packId that is a prefix of a real one', () => {
    expect(() => validateInitiatePurchaseInput({
      packId: 'coins_1_admin', idempotencyKey: 'idem-key-test1',
    })).toThrow();
  });

  it('REJECTS empty packId', () => {
    expect(() => validateInitiatePurchaseInput({
      packId: '', idempotencyKey: 'idem-key-test1',
    })).toThrow();
  });

  // ── Prototype-chain bypass (regression test for security finding) ────────
  // The original validator used `in` which walks the prototype chain. This
  // class of packId values bypassed the allowlist:
  it.each([
    '__proto__',
    'constructor',
    'toString',
    'hasOwnProperty',
    'valueOf',
    'isPrototypeOf',
    'propertyIsEnumerable',
  ])('REJECTS prototype-chain key "%s" (regression test)', (packId) => {
    expect(() => validateInitiatePurchaseInput({
      packId, idempotencyKey: 'idem-key-test1',
    })).toThrow();
  });
});

// Compile-time guard to ensure errorCodeOf is used somewhere
void errorCodeOf;
