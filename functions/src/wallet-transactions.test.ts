/**
 * wallet-transactions.test.ts
 * Tests for getWalletTransactions callable.
 * Covers: auth, pagination, metadata sanitization, ordering, walletId isolation.
 */

// ── Mock firebase-admin ───────────────────────────────────────────────────────

jest.mock('firebase-admin', () => ({
  initializeApp: jest.fn(),
  firestore: jest.fn(() => ({})),
}));

// ── Mock walletRepository ─────────────────────────────────────────────────────

const mockListByWalletId = jest.fn();

jest.mock('./lib/walletRepository', () => ({
  LedgerTransactionRepository: {
    listByWalletId: (...args: unknown[]) => mockListByWalletId(...args),
  },
  WalletRepository: { findById: jest.fn(), create: jest.fn() },
  SnapshotRepository: { findByWalletId: jest.fn(), create: jest.fn() },
  AuditLogRepository: { append: jest.fn() },
  IdempotencyRepository: {},
}));

// ── Mock rate limiter ─────────────────────────────────────────────────────────

jest.mock('./lib/rateLimiter', () => ({
  checkRateLimitFailClosed: jest.fn().mockResolvedValue(undefined),
  checkRateLimit: jest.fn().mockResolvedValue(undefined),
}));

import * as functionsTest from 'firebase-functions-test';
const testEnv = functionsTest();

import { getWalletTransactions } from './walletFunctions';
import type { LedgerTransactionDocument } from './lib/walletSchema';

// ── Helpers ───────────────────────────────────────────────────────────────────

function makeContext(uid = 'user-123') {
  return { auth: { uid, token: {} }, app: { appId: 'test-app' } };
}

function makeTxDoc(overrides: Partial<LedgerTransactionDocument> = {}): LedgerTransactionDocument {
  return {
    transactionId: 'tx-001',
    walletId: 'user-123',
    type: 'spend',
    idempotencyKey: 'idem-key-001',
    status: 'confirmed',
    metadata: { itemId: 'item-abc', quantitySpent: 10, correlationId: 'corr-internal' },
    createdAt: 1_700_000_000_000,
    schemaVersion: 1,
    ...overrides,
  };
}

const wrapped = testEnv.wrap(getWalletTransactions);

// ── Tests ─────────────────────────────────────────────────────────────────────

beforeEach(() => {
  mockListByWalletId.mockReset();
  mockListByWalletId.mockResolvedValue([]);
});

describe('getWalletTransactions — auth & App Check', () => {
  it('should throw unauthenticated when no auth', async () => {
    await expect(
      wrapped({}, { auth: undefined, app: { appId: 'x' } }),
    ).rejects.toMatchObject({ code: 'unauthenticated' });
  });

  it('should throw unauthenticated when App Check absent', async () => {
    await expect(
      wrapped({}, { auth: { uid: 'user-1' }, app: undefined }),
    ).rejects.toMatchObject({ code: 'unauthenticated' });
  });
});

describe('getWalletTransactions — pagination', () => {
  it('should return empty items array when no transactions', async () => {
    const result = await wrapped({}, makeContext());
    expect(result.items).toEqual([]);
    expect(result.nextCursor).toBeNull();
  });

  it('should pass limit to repository (default 20)', async () => {
    await wrapped({}, makeContext());
    expect(mockListByWalletId).toHaveBeenCalledWith('user-123', 20, undefined);
  });

  it('should pass custom limit to repository', async () => {
    await wrapped({ limit: 5 }, makeContext());
    expect(mockListByWalletId).toHaveBeenCalledWith('user-123', 5, undefined);
  });

  it('should pass cursor to repository', async () => {
    await wrapped({ limit: 10, cursor: 'tx-prev' }, makeContext());
    expect(mockListByWalletId).toHaveBeenCalledWith('user-123', 10, 'tx-prev');
  });

  it('should return nextCursor when full page returned', async () => {
    const docs = Array.from({ length: 5 }, (_, i) =>
      makeTxDoc({ transactionId: `tx-00${i + 1}` }),
    );
    mockListByWalletId.mockResolvedValue(docs);

    const result = await wrapped({ limit: 5 }, makeContext());
    expect(result.nextCursor).toBe('tx-005');
  });

  it('should return null nextCursor when partial page returned', async () => {
    const docs = [makeTxDoc({ transactionId: 'tx-001' }), makeTxDoc({ transactionId: 'tx-002' })];
    mockListByWalletId.mockResolvedValue(docs);

    const result = await wrapped({ limit: 5 }, makeContext());
    expect(result.nextCursor).toBeNull();
  });

  it('should reject invalid limit (> 100)', async () => {
    await expect(wrapped({ limit: 101 }, makeContext())).rejects.toMatchObject({
      code: 'invalid-argument',
    });
  });
});

describe('getWalletTransactions — metadata sanitization', () => {
  it('should expose itemId and coinsSpent for spend transactions', async () => {
    mockListByWalletId.mockResolvedValue([
      makeTxDoc({ type: 'spend', metadata: { itemId: 'boost-123', quantitySpent: 10, correlationId: 'internal' } }),
    ]);

    const result = await wrapped({}, makeContext());
    const item = result.items[0];
    expect(item.metadata.itemId).toBe('boost-123');
    expect(item.metadata.coinsSpent).toBe(10);
  });

  it('should NOT expose correlationId from metadata', async () => {
    mockListByWalletId.mockResolvedValue([
      makeTxDoc({ type: 'spend', metadata: { itemId: 'x', quantitySpent: 1, correlationId: 'should-not-leak' } }),
    ]);

    const result = await wrapped({}, makeContext());
    expect(result.items[0].metadata.correlationId).toBeUndefined();
  });

  it('should NOT expose idempotencyKey in response', async () => {
    mockListByWalletId.mockResolvedValue([makeTxDoc()]);
    const result = await wrapped({}, makeContext());
    expect(result.items[0]).not.toHaveProperty('idempotencyKey');
  });

  it('should NOT expose walletId in each item', async () => {
    mockListByWalletId.mockResolvedValue([makeTxDoc()]);
    const result = await wrapped({}, makeContext());
    expect(result.items[0]).not.toHaveProperty('walletId');
  });

  it('should NOT expose status in each item', async () => {
    mockListByWalletId.mockResolvedValue([makeTxDoc()]);
    const result = await wrapped({}, makeContext());
    expect(result.items[0]).not.toHaveProperty('status');
  });

  it('should expose packId for purchase_credit transactions', async () => {
    mockListByWalletId.mockResolvedValue([
      makeTxDoc({ type: 'purchase_credit', metadata: { packId: 'coins_100', purchaseId: 'internal-id' } }),
    ]);

    const result = await wrapped({}, makeContext());
    expect(result.items[0].metadata.packId).toBe('coins_100');
    expect(result.items[0].metadata.purchaseId).toBeUndefined();
  });

  it('should expose reversesTransactionId for refund transactions', async () => {
    mockListByWalletId.mockResolvedValue([
      makeTxDoc({ type: 'refund', metadata: { reversesTransactionId: 'tx-orig', internal: 'should-hide' } }),
    ]);

    const result = await wrapped({}, makeContext());
    expect(result.items[0].metadata.reversesTransactionId).toBe('tx-orig');
    expect(result.items[0].metadata.internal).toBeUndefined();
  });

  it('should always include transactionId, type, and createdAt', async () => {
    mockListByWalletId.mockResolvedValue([makeTxDoc()]);
    const result = await wrapped({}, makeContext());
    const item = result.items[0];
    expect(item.transactionId).toBe('tx-001');
    expect(item.type).toBe('spend');
    expect(item.createdAt).toBe(1_700_000_000_000);
  });
});

describe('getWalletTransactions — walletId isolation', () => {
  it('should use callerUid as walletId — never from client', async () => {
    await wrapped({}, makeContext('authenticated-user'));
    expect(mockListByWalletId).toHaveBeenCalledWith('authenticated-user', 20, undefined);
    expect(mockListByWalletId).not.toHaveBeenCalledWith('attacker-wallet', expect.anything(), expect.anything());
  });

  it('should return correlationId in response', async () => {
    const result = await wrapped({}, makeContext());
    expect(typeof result.correlationId).toBe('string');
  });
});
