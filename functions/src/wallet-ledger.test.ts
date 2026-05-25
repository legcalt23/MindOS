/**
 * wallet-ledger.test.ts
 * Tests for LedgerService: applyInTx and reverse.
 * Covers derived balances, snapshot updates, reversals, and zero-amount rejection.
 */

// ── Mock firebase-admin ───────────────────────────────────────────────────────

const mockRunTransaction = jest.fn();
const mockDocRef = { path: 'fake/path' };
const mockCollection = jest.fn(() => ({ doc: jest.fn(() => mockDocRef) }));

jest.mock('firebase-admin', () => ({
  firestore: jest.fn(() => ({
    collection: mockCollection,
    runTransaction: (...args: unknown[]) => mockRunTransaction(...args),
  })),
  initializeApp: jest.fn(),
}));

// ── Mock walletRepository ─────────────────────────────────────────────────────

const mockUpdateInTx = jest.fn();
const mockCreateTxInTx = jest.fn();
const mockCreateEntryInTx = jest.fn();
const mockFindSnapshotInTx = jest.fn();
const mockListEntriesByTxId = jest.fn();

jest.mock('./lib/walletRepository', () => ({
  SnapshotRepository: {
    updateInTx: (...args: unknown[]) => mockUpdateInTx(...args),
    findByWalletIdInTx: (...args: unknown[]) => mockFindSnapshotInTx(...args),
  },
  LedgerTransactionRepository: {
    createInTx: (...args: unknown[]) => mockCreateTxInTx(...args),
  },
  LedgerEntryRepository: {
    createInTx: (...args: unknown[]) => mockCreateEntryInTx(...args),
    listByTransactionId: (...args: unknown[]) => mockListEntriesByTxId(...args),
  },
}));

import { LedgerService, LedgerEntrySpec } from './lib/ledgerService';
import { BalanceSnapshotDocument } from './lib/walletSchema';
import type { Transaction } from 'firebase-admin/firestore';

// ── Helpers ───────────────────────────────────────────────────────────────────

const makeSnapshot = (overrides: Partial<BalanceSnapshotDocument> = {}): BalanceSnapshotDocument => ({
  walletId: 'user-1',
  availableBalance: 100,
  purchasedBalance: 80,
  promotionalBalance: 20,
  earnedBalance: 0,
  version: 3,
  lastTransactionId: null,
  updatedAt: Date.now(),
  schemaVersion: 1,
  ...overrides,
});

const BASE_PARAMS = {
  transactionId: 'tx-001',
  type: 'spend' as const,
  idempotencyKey: 'idem-001',
  metadata: {},
};

const TX = {} as unknown as Transaction;

// ── LedgerService.applyInTx ───────────────────────────────────────────────────

describe('LedgerService.applyInTx', () => {
  beforeEach(() => {
    mockUpdateInTx.mockReset();
    mockCreateTxInTx.mockReset();
    mockCreateEntryInTx.mockReset();
  });

  it('should throw when entries array is empty', () => {
    expect(() =>
      LedgerService.applyInTx(TX, 'user-1', makeSnapshot(), { ...BASE_PARAMS, entries: [] }),
    ).toThrow('entries must not be empty');
  });

  it('should throw when any entry has zero amount', () => {
    expect(() =>
      LedgerService.applyInTx(TX, 'user-1', makeSnapshot(), {
        ...BASE_PARAMS,
        entries: [{ bucket: 'purchased', amount: 0 }],
      }),
    ).toThrow('amount must not be zero');
  });

  it('should call SnapshotRepository.updateInTx with summed deltas', () => {
    const snap = makeSnapshot({ purchasedBalance: 100, promotionalBalance: 10, earnedBalance: 5 });
    const entries: LedgerEntrySpec[] = [
      { bucket: 'purchased', amount: -30 },
      { bucket: 'promotional', amount: -5 },
    ];
    LedgerService.applyInTx(TX, 'user-1', snap, { ...BASE_PARAMS, entries });

    expect(mockUpdateInTx).toHaveBeenCalledWith(
      TX,
      'user-1',
      { purchasedDelta: -30, promotionalDelta: -5, earnedDelta: 0 },
      snap,
      'tx-001',
    );
  });

  it('should call LedgerTransactionRepository.createInTx with confirmed status', () => {
    LedgerService.applyInTx(TX, 'user-1', makeSnapshot(), {
      ...BASE_PARAMS,
      entries: [{ bucket: 'purchased', amount: 10 }],
    });

    expect(mockCreateTxInTx).toHaveBeenCalledWith(
      TX,
      expect.objectContaining({
        transactionId: 'tx-001',
        status: 'confirmed',
        type: 'spend',
        walletId: 'user-1',
      }),
    );
  });

  it('should call LedgerEntryRepository.createInTx once per entry', () => {
    const entries: LedgerEntrySpec[] = [
      { bucket: 'purchased', amount: -10 },
      { bucket: 'promotional', amount: -5 },
    ];
    LedgerService.applyInTx(TX, 'user-1', makeSnapshot(), { ...BASE_PARAMS, entries });

    expect(mockCreateEntryInTx).toHaveBeenCalledTimes(2);
  });

  it('should compute running balances per bucket across multiple entries', () => {
    const snap = makeSnapshot({ purchasedBalance: 100, promotionalBalance: 20, earnedBalance: 0 });
    const entries: LedgerEntrySpec[] = [
      { bucket: 'purchased', amount: -10 },
      { bucket: 'purchased', amount: -5 },
      { bucket: 'promotional', amount: -3 },
    ];
    LedgerService.applyInTx(TX, 'user-1', snap, { ...BASE_PARAMS, entries });

    const calls = mockCreateEntryInTx.mock.calls.map((c: unknown[]) => c[1]);
    expect(calls[0]).toMatchObject({ bucket: 'purchased', amount: -10, runningBalance: 90 });
    expect(calls[1]).toMatchObject({ bucket: 'purchased', amount: -5, runningBalance: 85 });
    expect(calls[2]).toMatchObject({ bucket: 'promotional', amount: -3, runningBalance: 17 });
  });

  it('should set entryType to credit for positive amounts', () => {
    LedgerService.applyInTx(TX, 'user-1', makeSnapshot(), {
      ...BASE_PARAMS,
      type: 'purchase_credit',
      entries: [{ bucket: 'purchased', amount: 50 }],
    });

    expect(mockCreateEntryInTx).toHaveBeenCalledWith(
      TX,
      expect.objectContaining({ entryType: 'credit', amount: 50 }),
    );
  });

  it('should set entryType to debit for negative amounts', () => {
    LedgerService.applyInTx(TX, 'user-1', makeSnapshot(), {
      ...BASE_PARAMS,
      entries: [{ bucket: 'purchased', amount: -10 }],
    });

    expect(mockCreateEntryInTx).toHaveBeenCalledWith(
      TX,
      expect.objectContaining({ entryType: 'debit', amount: -10 }),
    );
  });

  it('should propagate INSUFFICIENT_BALANCE from SnapshotRepository', () => {
    mockUpdateInTx.mockImplementation(() => { throw new Error('INSUFFICIENT_BALANCE'); });

    expect(() =>
      LedgerService.applyInTx(TX, 'user-1', makeSnapshot({ purchasedBalance: 5 }), {
        ...BASE_PARAMS,
        entries: [{ bucket: 'purchased', amount: -100 }],
      }),
    ).toThrow('INSUFFICIENT_BALANCE');
  });

  it('should handle credit-only transactions (purchase_credit)', () => {
    const snap = makeSnapshot({ purchasedBalance: 0, availableBalance: 0 });
    LedgerService.applyInTx(TX, 'user-1', snap, {
      transactionId: 'tx-002',
      type: 'purchase_credit',
      idempotencyKey: 'idem-002',
      entries: [{ bucket: 'purchased', amount: 100 }],
      metadata: { packId: 'coins_100' },
    });

    expect(mockUpdateInTx).toHaveBeenCalledWith(
      TX, 'user-1',
      { purchasedDelta: 100, promotionalDelta: 0, earnedDelta: 0 },
      snap, 'tx-002',
    );
  });

  it('should handle multi-bucket credits in one transaction', () => {
    const snap = makeSnapshot({ purchasedBalance: 0, promotionalBalance: 0, earnedBalance: 0, availableBalance: 0 });
    const entries: LedgerEntrySpec[] = [
      { bucket: 'purchased', amount: 50 },
      { bucket: 'promotional', amount: 10 },
    ];
    LedgerService.applyInTx(TX, 'user-1', snap, { ...BASE_PARAMS, type: 'admin_adjustment', entries });

    expect(mockUpdateInTx).toHaveBeenCalledWith(
      TX, 'user-1',
      { purchasedDelta: 50, promotionalDelta: 10, earnedDelta: 0 },
      snap, 'tx-001',
    );
    expect(mockCreateEntryInTx).toHaveBeenCalledTimes(2);
  });
});

// ── LedgerService.reverse ─────────────────────────────────────────────────────

describe('LedgerService.reverse', () => {
  beforeEach(() => {
    mockListEntriesByTxId.mockReset();
    mockFindSnapshotInTx.mockReset();
    mockUpdateInTx.mockReset();
    mockCreateTxInTx.mockReset();
    mockCreateEntryInTx.mockReset();
    mockRunTransaction.mockReset();
  });

  it('should throw PURCHASE_NOT_FOUND when original entries are missing', async () => {
    mockListEntriesByTxId.mockResolvedValue([]);

    await expect(
      LedgerService.reverse('user-1', 'tx-orig', 'tx-rev', 'idem-rev', 'corr-1', {}),
    ).rejects.toThrow();

    expect(mockRunTransaction).not.toHaveBeenCalled();
  });

  it('should run a Firestore transaction when original entries exist', async () => {
    mockListEntriesByTxId.mockResolvedValue([
      { bucket: 'purchased', amount: -10, entryId: 'e-1', transactionId: 'tx-orig', walletId: 'user-1', entryType: 'debit', runningBalance: 90, createdAt: 0, schemaVersion: 1 },
    ]);
    mockRunTransaction.mockImplementation(async (fn: (tx: Transaction) => Promise<void>) => {
      const snap = makeSnapshot({ purchasedBalance: 90 });
      mockFindSnapshotInTx.mockResolvedValue(snap);
      await fn(TX);
    });

    await LedgerService.reverse('user-1', 'tx-orig', 'tx-rev', 'idem-rev', 'corr-1', {});

    expect(mockRunTransaction).toHaveBeenCalledTimes(1);
  });

  it('should negate original entry amounts in reversal', async () => {
    mockListEntriesByTxId.mockResolvedValue([
      { bucket: 'purchased', amount: -10, entryId: 'e-1', transactionId: 'tx-orig', walletId: 'user-1', entryType: 'debit', runningBalance: 90, createdAt: 0, schemaVersion: 1 },
    ]);
    mockRunTransaction.mockImplementation(async (fn: (tx: Transaction) => Promise<void>) => {
      const snap = makeSnapshot({ purchasedBalance: 90 });
      mockFindSnapshotInTx.mockResolvedValue(snap);
      await fn(TX);
    });

    await LedgerService.reverse('user-1', 'tx-orig', 'tx-rev', 'idem-rev', 'corr-1', {});

    expect(mockUpdateInTx).toHaveBeenCalledWith(
      TX, 'user-1',
      { purchasedDelta: 10, promotionalDelta: 0, earnedDelta: 0 },
      expect.anything(),
      'tx-rev',
    );
  });

  it('should use type refund and include reversesTransactionId in metadata', async () => {
    mockListEntriesByTxId.mockResolvedValue([
      { bucket: 'purchased', amount: -5, entryId: 'e-2', transactionId: 'tx-orig', walletId: 'user-1', entryType: 'debit', runningBalance: 95, createdAt: 0, schemaVersion: 1 },
    ]);
    mockRunTransaction.mockImplementation(async (fn: (tx: Transaction) => Promise<void>) => {
      const snap = makeSnapshot({ purchasedBalance: 95 });
      mockFindSnapshotInTx.mockResolvedValue(snap);
      await fn(TX);
    });

    await LedgerService.reverse('user-1', 'tx-orig', 'tx-rev', 'idem-rev', 'corr-1', { reason: 'admin_refund' });

    expect(mockCreateTxInTx).toHaveBeenCalledWith(
      TX,
      expect.objectContaining({
        type: 'refund',
        metadata: expect.objectContaining({ reversesTransactionId: 'tx-orig', reason: 'admin_refund' }),
      }),
    );
  });

  it('should throw WALLET_NOT_FOUND if snapshot is missing inside transaction', async () => {
    mockListEntriesByTxId.mockResolvedValue([
      { bucket: 'purchased', amount: -5, entryId: 'e-3', transactionId: 'tx-orig', walletId: 'user-1', entryType: 'debit', runningBalance: 95, createdAt: 0, schemaVersion: 1 },
    ]);
    mockRunTransaction.mockImplementation(async (fn: (tx: Transaction) => Promise<void>) => {
      mockFindSnapshotInTx.mockResolvedValue(null);
      await fn(TX);
    });

    await expect(
      LedgerService.reverse('user-1', 'tx-orig', 'tx-rev', 'idem-rev', 'corr-1', {}),
    ).rejects.toThrow();
  });
});
