/**
 * wallet-reconciliation.test.ts
 * Tests for the reconciliation service: match, mismatch (detect), mismatch (correct).
 */

// ── Mock firebase-admin ───────────────────────────────────────────────────────

const mockUpdate = jest.fn();
const mockDocRef = { update: (...args: unknown[]) => mockUpdate(...args) };
const mockDocFn = jest.fn(() => mockDocRef);
const mockCollection = jest.fn(() => ({ doc: mockDocFn }));

jest.mock('firebase-admin', () => ({
  initializeApp: jest.fn(),
  firestore: jest.fn(() => ({ collection: mockCollection })),
}));

// ── Mock walletRepository ─────────────────────────────────────────────────────

const mockFindSnapshot = jest.fn();
const mockListEntries = jest.fn();
const mockAuditAppend = jest.fn();

jest.mock('./lib/walletRepository', () => ({
  SnapshotRepository: {
    findByWalletId: (...args: unknown[]) => mockFindSnapshot(...args),
  },
  LedgerEntryRepository: {
    listByWalletId: (...args: unknown[]) => mockListEntries(...args),
  },
  AuditLogRepository: {
    append: (...args: unknown[]) => mockAuditAppend(...args),
  },
}));

import { reconcileWallet, computeBalancesFromEntries } from './lib/reconciliationService';
import type { BalanceSnapshotDocument, LedgerEntryDocument } from './lib/walletSchema';

// ── Helpers ───────────────────────────────────────────────────────────────────

const makeSnapshot = (overrides: Partial<BalanceSnapshotDocument> = {}): BalanceSnapshotDocument => ({
  walletId: 'user-1',
  availableBalance: 100,
  purchasedBalance: 80,
  promotionalBalance: 20,
  earnedBalance: 0,
  version: 5,
  lastTransactionId: 'tx-prev',
  updatedAt: Date.now(),
  schemaVersion: 1,
  ...overrides,
});

const makeEntry = (bucket: LedgerEntryDocument['bucket'], amount: number): LedgerEntryDocument => ({
  entryId: `e-${Math.random()}`,
  transactionId: 'tx-1',
  walletId: 'user-1',
  entryType: amount > 0 ? 'credit' : 'debit',
  bucket,
  amount,
  runningBalance: 0,
  createdAt: Date.now(),
  schemaVersion: 1,
});

beforeEach(() => {
  jest.clearAllMocks();
  mockAuditAppend.mockResolvedValue(undefined);
  mockUpdate.mockResolvedValue(undefined);
});

// ── computeBalancesFromEntries (pure function) ────────────────────────────────

describe('computeBalancesFromEntries', () => {
  it('should return zeros for empty entries', () => {
    const result = computeBalancesFromEntries([]);
    expect(result).toEqual({ purchased: 0, promotional: 0, earned: 0 });
  });

  it('should sum credits and debits correctly per bucket', () => {
    const entries = [
      makeEntry('purchased', 100),   // credit
      makeEntry('purchased', -10),   // debit
      makeEntry('promotional', 20),  // credit
      makeEntry('earned', 5),        // credit
    ];
    const result = computeBalancesFromEntries(entries);
    expect(result).toEqual({ purchased: 90, promotional: 20, earned: 5 });
  });

  it('should handle multiple entries for same bucket', () => {
    const entries = [
      makeEntry('purchased', 50),
      makeEntry('purchased', 50),
      makeEntry('purchased', -30),
    ];
    expect(computeBalancesFromEntries(entries)).toEqual({ purchased: 70, promotional: 0, earned: 0 });
  });

  it('should produce zero for a balanced set of credit+debit', () => {
    const entries = [makeEntry('purchased', 100), makeEntry('purchased', -100)];
    expect(computeBalancesFromEntries(entries).purchased).toBe(0);
  });
});

// ── reconcileWallet — match ───────────────────────────────────────────────────

describe('reconcileWallet — match (snapshot = computed)', () => {
  beforeEach(() => {
    mockFindSnapshot.mockResolvedValue(makeSnapshot({ purchasedBalance: 90, promotionalBalance: 20, earnedBalance: 5 }));
    mockListEntries.mockResolvedValue([
      makeEntry('purchased', 100),
      makeEntry('purchased', -10),
      makeEntry('promotional', 20),
      makeEntry('earned', 5),
    ]);
    mockAuditAppend.mockResolvedValue(undefined);
  });

  it('should report hasMismatch: false', async () => {
    const result = await reconcileWallet('user-1');
    expect(result.hasMismatch).toBe(false);
  });

  it('should report corrected: false', async () => {
    const result = await reconcileWallet('user-1');
    expect(result.corrected).toBe(false);
  });

  it('should NOT append any audit log on match', async () => {
    await reconcileWallet('user-1');
    expect(mockAuditAppend).not.toHaveBeenCalled();
  });

  it('should return correct snapshot and computed values', async () => {
    const result = await reconcileWallet('user-1');
    expect(result.snapshotValues).toEqual({ purchased: 90, promotional: 20, earned: 5 });
    expect(result.computedValues).toEqual({ purchased: 90, promotional: 20, earned: 5 });
  });
});

// ── reconcileWallet — mismatch (detect-only) ─────────────────────────────────

describe('reconcileWallet — mismatch, detect-only (default)', () => {
  beforeEach(() => {
    // Snapshot says 100 purchased, but ledger only has 80
    mockFindSnapshot.mockResolvedValue(makeSnapshot({ purchasedBalance: 100, promotionalBalance: 0, earnedBalance: 0, availableBalance: 100 }));
    mockListEntries.mockResolvedValue([
      makeEntry('purchased', 80),  // ledger total: 80, not 100
    ]);
    mockAuditAppend.mockResolvedValue(undefined);
  });

  it('should report hasMismatch: true', async () => {
    const result = await reconcileWallet('user-1');
    expect(result.hasMismatch).toBe(true);
  });

  it('should report corrected: false in detect-only mode', async () => {
    const result = await reconcileWallet('user-1');
    expect(result.corrected).toBe(false);
  });

  it('should append audit log with reconciliation_mismatch action', async () => {
    await reconcileWallet('user-1');
    expect(mockAuditAppend).toHaveBeenCalledWith(
      'system', 'system', 'reconciliation_mismatch', 'user-1',
      expect.any(String),
      expect.objectContaining({
        snapshotValues: expect.objectContaining({ purchased: 100 }),
        computedValues: expect.objectContaining({ purchased: 80 }),
      }),
    );
  });

  it('should NOT update Firestore snapshot in detect-only mode', async () => {
    await reconcileWallet('user-1');
    expect(mockUpdate).not.toHaveBeenCalled();
  });

  it('should return both snapshot and computed values for comparison', async () => {
    const result = await reconcileWallet('user-1');
    expect(result.snapshotValues.purchased).toBe(100);
    expect(result.computedValues.purchased).toBe(80);
  });
});

// ── reconcileWallet — mismatch (correction mode) ─────────────────────────────

describe('reconcileWallet — mismatch, correctIfMismatch: true', () => {
  beforeEach(() => {
    mockFindSnapshot.mockResolvedValue(makeSnapshot({ purchasedBalance: 100, promotionalBalance: 0, earnedBalance: 0, availableBalance: 100 }));
    mockListEntries.mockResolvedValue([makeEntry('purchased', 80)]);
    mockAuditAppend.mockResolvedValue(undefined);
    mockUpdate.mockResolvedValue(undefined);
  });

  it('should report corrected: true', async () => {
    const result = await reconcileWallet('user-1', { correctIfMismatch: true });
    expect(result.corrected).toBe(true);
  });

  it('should update Firestore snapshot with computed values', async () => {
    await reconcileWallet('user-1', { correctIfMismatch: true });
    expect(mockUpdate).toHaveBeenCalledWith(
      expect.objectContaining({
        purchasedBalance:   80,
        promotionalBalance: 0,
        earnedBalance:      0,
        availableBalance:   80,
      }),
    );
  });

  it('should append two audit logs: mismatch + correction', async () => {
    await reconcileWallet('user-1', { correctIfMismatch: true });
    expect(mockAuditAppend).toHaveBeenCalledTimes(2);
    expect(mockAuditAppend).toHaveBeenCalledWith(
      'system', 'system', 'reconciliation_mismatch', 'user-1', expect.any(String), expect.any(Object),
    );
    expect(mockAuditAppend).toHaveBeenCalledWith(
      'system', 'system', 'reconciliation_correction', 'user-1', expect.any(String), expect.any(Object),
    );
  });
});

// ── reconcileWallet — missing snapshot ───────────────────────────────────────

describe('reconcileWallet — missing snapshot', () => {
  it('should throw when wallet snapshot does not exist', async () => {
    mockFindSnapshot.mockResolvedValue(null);
    mockListEntries.mockResolvedValue([]);

    await expect(reconcileWallet('nonexistent-wallet')).rejects.toThrow('no snapshot found');
  });
});
