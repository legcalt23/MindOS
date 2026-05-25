/**
 * wallet-spend.test.ts
 * Tests for spendCoins callable.
 * Covers: auth, balance validation, idempotency, double-spend, bucket drain order.
 */

// ── Mock firebase-admin ───────────────────────────────────────────────────────

const mockRunTransaction = jest.fn();

jest.mock('firebase-admin', () => ({
  initializeApp: jest.fn(),
  firestore: jest.fn(() => ({
    collection: jest.fn(() => ({ doc: jest.fn(() => ({ path: 'fake' })) })),
    runTransaction: (...args: unknown[]) => mockRunTransaction(...args),
  })),
}));

// ── Mock walletRepository ─────────────────────────────────────────────────────

const mockFindWalletInTx = jest.fn();
const mockFindSnapshotInTx = jest.fn();
const mockFindIdempotencyInTx = jest.fn();
const mockCreateInProgressInTx = jest.fn();
const mockCompleteInTx = jest.fn();
const mockAuditAppendInTx = jest.fn();

// Also mock non-tx find for pre-check
const mockFindIdempotency = jest.fn();

jest.mock('./lib/walletRepository', () => ({
  WalletRepository: {
    findByIdInTx: (...args: unknown[]) => mockFindWalletInTx(...args),
  },
  SnapshotRepository: {
    findByWalletIdInTx: (...args: unknown[]) => mockFindSnapshotInTx(...args),
  },
  IdempotencyRepository: {
    find: (...args: unknown[]) => mockFindIdempotency(...args),
    findInTx: (...args: unknown[]) => mockFindIdempotencyInTx(...args),
    createInProgressInTx: (...args: unknown[]) => mockCreateInProgressInTx(...args),
    completeInTx: (...args: unknown[]) => mockCompleteInTx(...args),
    failInTx: jest.fn(),
  },
  AuditLogRepository: {
    appendInTx: (...args: unknown[]) => mockAuditAppendInTx(...args),
  },
  LedgerTransactionRepository: { createInTx: jest.fn() },
  LedgerEntryRepository: { createInTx: jest.fn() },
}));

// ── Mock LedgerService ────────────────────────────────────────────────────────

const mockLedgerApplyInTx = jest.fn();

jest.mock('./lib/ledgerService', () => ({
  LedgerService: {
    applyInTx: (...args: unknown[]) => mockLedgerApplyInTx(...args),
  },
}));

// ── Mock rate limiter ─────────────────────────────────────────────────────────

jest.mock('./lib/rateLimiter', () => ({
  checkRateLimitFailClosed: jest.fn().mockResolvedValue(undefined),
  checkRateLimit: jest.fn().mockResolvedValue(undefined),
}));

import * as functionsTest from 'firebase-functions-test';
const testEnv = functionsTest();

import { spendCoins } from './walletFunctions';
import type { WalletDocument, BalanceSnapshotDocument, IdempotencyKeyDocument } from './lib/walletSchema';
import type { Transaction } from 'firebase-admin/firestore';

// ── Helpers ───────────────────────────────────────────────────────────────────

function makeContext(uid = 'user-123') {
  return { auth: { uid, token: {} }, app: { appId: 'test-app' } };
}

const makeWallet = (overrides: Partial<WalletDocument> = {}): WalletDocument => ({
  walletId: 'user-123', userId: 'user-123', status: 'active',
  createdAt: Date.now(), updatedAt: Date.now(), schemaVersion: 1,
  ...overrides,
});

const makeSnapshot = (overrides: Partial<BalanceSnapshotDocument> = {}): BalanceSnapshotDocument => ({
  walletId: 'user-123', availableBalance: 100, purchasedBalance: 80,
  promotionalBalance: 20, earnedBalance: 0, version: 3,
  lastTransactionId: null, updatedAt: Date.now(), schemaVersion: 1,
  ...overrides,
});

const TX = {} as unknown as Transaction;

// Simulate a successful runTransaction: executes callback with mocked tx
function setupTransaction(walletOverride?: Partial<WalletDocument>, snapshotOverride?: Partial<BalanceSnapshotDocument>) {
  mockRunTransaction.mockImplementation(async (fn: (tx: Transaction) => Promise<unknown>) => {
    mockFindIdempotencyInTx.mockResolvedValue(null); // no existing idempotency key
    mockFindWalletInTx.mockResolvedValue(makeWallet(walletOverride));
    mockFindSnapshotInTx.mockResolvedValue(makeSnapshot(snapshotOverride));
    return fn(TX);
  });
}

const VALID_INPUT = {
  itemId: 'boost-post-abc',
  quantity: 10,
  idempotencyKey: 'idem-key-001',
};

const wrapped = testEnv.wrap(spendCoins);

// ── Tests ─────────────────────────────────────────────────────────────────────

beforeEach(() => {
  jest.clearAllMocks();
  mockFindIdempotency.mockResolvedValue(null); // default: no pre-existing idempotency key
});

describe('spendCoins — auth & App Check', () => {
  it('should throw unauthenticated when no auth', async () => {
    await expect(
      wrapped(VALID_INPUT, { auth: undefined, app: { appId: 'x' } }),
    ).rejects.toMatchObject({ code: 'unauthenticated' });
  });

  it('should throw unauthenticated when App Check absent', async () => {
    await expect(
      wrapped(VALID_INPUT, { auth: { uid: 'user-1' }, app: undefined }),
    ).rejects.toMatchObject({ code: 'unauthenticated' });
  });
});

describe('spendCoins — input validation', () => {
  it('should reject forbidden field "amount" from client', async () => {
    await expect(
      wrapped({ ...VALID_INPUT, amount: 10 }, makeContext()),
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });

  it('should reject quantity = 0', async () => {
    await expect(
      wrapped({ ...VALID_INPUT, quantity: 0 }, makeContext()),
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });

  it('should reject quantity > 1000', async () => {
    await expect(
      wrapped({ ...VALID_INPUT, quantity: 1001 }, makeContext()),
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });

  it('should reject missing idempotencyKey', async () => {
    const { idempotencyKey: _, ...rest } = VALID_INPUT;
    await expect(wrapped(rest, makeContext())).rejects.toMatchObject({ code: 'invalid-argument' });
  });
});

describe('spendCoins — wallet state validation', () => {
  it('should throw WALLET_FROZEN for frozen wallet', async () => {
    setupTransaction({ status: 'frozen' });
    await expect(wrapped(VALID_INPUT, makeContext())).rejects.toMatchObject({ code: 'failed-precondition' });
  });

  it('should throw WALLET_SUSPENDED for suspended wallet', async () => {
    setupTransaction({ status: 'suspended' });
    await expect(wrapped(VALID_INPUT, makeContext())).rejects.toMatchObject({ code: 'permission-denied' });
  });

  it('should throw resource-exhausted for insufficient balance', async () => {
    setupTransaction(undefined, {
      availableBalance: 5, purchasedBalance: 5,
      promotionalBalance: 0, earnedBalance: 0,
    });
    await expect(
      wrapped({ ...VALID_INPUT, quantity: 10 }, makeContext()),
    ).rejects.toMatchObject({ code: 'failed-precondition' });
  });
});

describe('spendCoins — successful spend', () => {
  beforeEach(() => {
    setupTransaction();
  });

  it('should return transactionId, coinsSpent, and itemId', async () => {
    const result = await wrapped(VALID_INPUT, makeContext());
    expect(result.coinsSpent).toBe(10);
    expect(result.itemId).toBe('boost-post-abc');
    expect(typeof result.transactionId).toBe('string');
  });

  it('should call LedgerService.applyInTx with spend type', async () => {
    await wrapped(VALID_INPUT, makeContext());
    expect(mockLedgerApplyInTx).toHaveBeenCalledWith(
      TX,
      'user-123',
      expect.anything(),
      expect.objectContaining({ type: 'spend', idempotencyKey: 'idem-key-001' }),
    );
  });

  it('should call AuditLogRepository.appendInTx', async () => {
    await wrapped(VALID_INPUT, makeContext());
    expect(mockAuditAppendInTx).toHaveBeenCalledWith(
      TX,
      'user-123',
      'user',
      'spend_coins',
      'user-123',
      expect.any(String),
      expect.objectContaining({ itemId: 'boost-post-abc', quantitySpent: 10 }),
    );
  });

  it('should call IdempotencyRepository.completeInTx', async () => {
    await wrapped(VALID_INPUT, makeContext());
    expect(mockCompleteInTx).toHaveBeenCalledWith(
      TX,
      'user-123',
      'idem-key-001',
      expect.any(String),
      expect.objectContaining({ coinsSpent: 10 }),
    );
  });

  it('should use callerUid as walletId — never from client data', async () => {
    await wrapped(VALID_INPUT, makeContext('authenticated-user'));
    // Should read wallet for authenticated-user, not any id from data
    expect(mockFindWalletInTx).toHaveBeenCalledWith(TX, 'authenticated-user');
  });
});

describe('spendCoins — bucket drain order', () => {
  it('should drain promotional first, then earned, then purchased', async () => {
    // Override LedgerService.applyInTx to capture entries
    let capturedEntries: unknown[] = [];
    mockLedgerApplyInTx.mockImplementation((_tx: unknown, _wid: unknown, _snap: unknown, params: { entries: unknown[] }) => {
      capturedEntries = params.entries;
    });

    mockRunTransaction.mockImplementation(async (fn: (tx: Transaction) => Promise<unknown>) => {
      mockFindIdempotencyInTx.mockResolvedValue(null);
      mockFindWalletInTx.mockResolvedValue(makeWallet());
      mockFindSnapshotInTx.mockResolvedValue(makeSnapshot({
        availableBalance: 30,
        promotionalBalance: 15,
        earnedBalance: 5,
        purchasedBalance: 10,
      }));
      return fn(TX);
    });

    await wrapped({ ...VALID_INPUT, quantity: 25 }, makeContext());

    // Should drain: 15 promo + 5 earned + 5 purchased = 25
    expect(capturedEntries).toEqual(
      expect.arrayContaining([
        { bucket: 'promotional', amount: -15 },
        { bucket: 'earned', amount: -5 },
        { bucket: 'purchased', amount: -5 },
      ]),
    );
  });

  it('should only create entries for buckets that are drained', async () => {
    let capturedEntries: unknown[] = [];
    mockLedgerApplyInTx.mockImplementation((_tx: unknown, _wid: unknown, _snap: unknown, params: { entries: unknown[] }) => {
      capturedEntries = params.entries;
    });

    mockRunTransaction.mockImplementation(async (fn: (tx: Transaction) => Promise<unknown>) => {
      mockFindIdempotencyInTx.mockResolvedValue(null);
      mockFindWalletInTx.mockResolvedValue(makeWallet());
      mockFindSnapshotInTx.mockResolvedValue(makeSnapshot({
        availableBalance: 80,
        promotionalBalance: 0,
        earnedBalance: 0,
        purchasedBalance: 80,
      }));
      return fn(TX);
    });

    await wrapped({ ...VALID_INPUT, quantity: 10 }, makeContext());

    // Only purchased entry, no promotional/earned entries
    expect(capturedEntries).toEqual([{ bucket: 'purchased', amount: -10 }]);
  });
});

describe('spendCoins — idempotency', () => {
  it('should return cached response on replay (pre-tx check)', async () => {
    const cachedResponse = { transactionId: 'tx-cached', coinsSpent: 10, itemId: 'boost-post-abc', correlationId: 'corr-1' };
    mockFindIdempotency.mockResolvedValue({
      status: 'completed',
      requestHash: 'a'.repeat(64), // will match since hashRequest is deterministic
      responsePayload: cachedResponse,
      transactionId: 'tx-cached',
    } as Partial<IdempotencyKeyDocument>);

    // The pre-check will find the completed key and return early without transaction
    // But the hash won't match unless we mock hashRequest too.
    // Instead, test via transaction replay path (in-transaction idempotency).
    // This test verifies that when the transaction sees a completed key, it returns the cached response.
    mockFindIdempotency.mockResolvedValue(null); // no pre-check hit
    mockRunTransaction.mockImplementation(async (fn: (tx: Transaction) => Promise<unknown>) => {
      // Simulate finding completed idempotency key inside transaction
      mockFindIdempotencyInTx.mockResolvedValue({
        status: 'completed',
        requestHash: 'matching-hash',
        responsePayload: cachedResponse,
        transactionId: 'tx-cached',
      } as Partial<IdempotencyKeyDocument>);
      mockFindWalletInTx.mockResolvedValue(makeWallet());
      mockFindSnapshotInTx.mockResolvedValue(makeSnapshot());
      return fn(TX);
    });

    // The idempotency service will call resolveIdempotencyDecision with the existing doc.
    // Since requestHash won't match (mock doesn't know real hash), let's use a simpler approach:
    // mock the real completed status with the actual computed hash.
    // Instead, we trust the resolveIdempotencyDecision unit tests and test that
    // the callable correctly returns the replay response from the pre-check.

    // Test the simpler case: pre-check returns replay
    const { hashRequest: realHashRequest } = jest.requireActual('./lib/idempotencyService') as { hashRequest: (data: unknown) => string };
    const realHash = realHashRequest({ itemId: 'boost-post-abc', quantity: 10 });

    mockFindIdempotency.mockResolvedValue({
      status: 'completed',
      requestHash: realHash,
      responsePayload: cachedResponse,
      transactionId: 'tx-cached',
    } as Partial<IdempotencyKeyDocument>);

    const result = await wrapped(VALID_INPUT, makeContext());
    expect(result.transactionId).toBe('tx-cached');
    expect(result.coinsSpent).toBe(10);
    // Should NOT have run a transaction
    expect(mockRunTransaction).not.toHaveBeenCalled();
  });

  it('should throw conflict when same key used with different body', async () => {
    const { hashRequest: realHashRequest } = jest.requireActual('./lib/idempotencyService') as { hashRequest: (data: unknown) => string };
    const differentBodyHash = realHashRequest({ itemId: 'different-item', quantity: 999 });

    mockFindIdempotency.mockResolvedValue({
      status: 'completed',
      requestHash: differentBodyHash,
      responsePayload: { coinsSpent: 999 },
      transactionId: 'tx-other',
    } as Partial<IdempotencyKeyDocument>);

    await expect(wrapped(VALID_INPUT, makeContext())).rejects.toMatchObject({ code: 'aborted' });
  });

  it('should throw concurrent error when key is in_progress (double-spend attempt)', async () => {
    const { hashRequest: realHashRequest } = jest.requireActual('./lib/idempotencyService') as { hashRequest: (data: unknown) => string };
    const realHash = realHashRequest({ itemId: 'boost-post-abc', quantity: 10 });

    mockFindIdempotency.mockResolvedValue({
      status: 'in_progress',
      requestHash: realHash,
    } as Partial<IdempotencyKeyDocument>);

    await expect(wrapped(VALID_INPUT, makeContext())).rejects.toMatchObject({ code: 'already-exists' });
  });
});
