/**
 * wallet-repository.test.ts
 * Tests for repository helpers: uniqueness enforcement, append-only constraint,
 * snapshot delta calculation, and zero-amount guard.
 */

// ── Mock firebase-admin before any imports that touch it ──────────────────────

const mockDocRef = { path: 'fake/path' };
const mockCollection = jest.fn(() => ({ doc: jest.fn(() => mockDocRef) }));

jest.mock('firebase-admin', () => ({
  firestore: jest.fn(() => ({ collection: mockCollection })),
  initializeApp: jest.fn(),
}));

import { hashPurchaseToken, SnapshotRepository, LedgerEntryRepository } from './lib/walletRepository';
import { BalanceSnapshotDocument } from './lib/walletSchema';
import type { Transaction } from 'firebase-admin/firestore';

// ── hashPurchaseToken ─────────────────────────────────────────────────────────

describe('hashPurchaseToken', () => {
  it('should produce a 64-char hex SHA-256', () => {
    const hash = hashPurchaseToken('test-token');
    expect(hash).toMatch(/^[a-f0-9]{64}$/);
  });

  it('should be deterministic', () => {
    expect(hashPurchaseToken('abc')).toBe(hashPurchaseToken('abc'));
  });

  it('should produce different hashes for different inputs', () => {
    expect(hashPurchaseToken('token-a')).not.toBe(hashPurchaseToken('token-b'));
  });

  it('should not return the raw token', () => {
    const raw = 'my-secret-purchase-token';
    const hash = hashPurchaseToken(raw);
    expect(hash).not.toContain(raw);
  });
});

// ── SnapshotRepository.updateInTx ─────────────────────────────────────────────

const makeSnapshot = (overrides: Partial<BalanceSnapshotDocument> = {}): BalanceSnapshotDocument => ({
  walletId: 'user-1',
  availableBalance: 100,
  purchasedBalance: 80,
  promotionalBalance: 20,
  earnedBalance: 0,
  version: 5,
  lastTransactionId: null,
  updatedAt: Date.now(),
  schemaVersion: 1,
  ...overrides,
});

describe('SnapshotRepository.updateInTx', () => {
  it('should throw INSUFFICIENT_BALANCE when debit exceeds purchased balance', () => {
    const tx = { update: jest.fn() } as unknown as Transaction;
    const snap = makeSnapshot({ purchasedBalance: 10, availableBalance: 10, promotionalBalance: 0, earnedBalance: 0 });
    expect(() => {
      SnapshotRepository.updateInTx(tx, 'user-1', { purchasedDelta: -50 }, snap, 'tx-123');
    }).toThrow('INSUFFICIENT_BALANCE');
  });

  it('should throw INSUFFICIENT_BALANCE when any bucket goes negative', () => {
    const tx = { update: jest.fn() } as unknown as Transaction;
    const snap = makeSnapshot({ promotionalBalance: 5 });
    expect(() => {
      SnapshotRepository.updateInTx(tx, 'user-1', { promotionalDelta: -10 }, snap, 'tx-456');
    }).toThrow('INSUFFICIENT_BALANCE');
  });

  it('should call tx.update with correct new balances', () => {
    const mockUpdate = jest.fn();
    const tx = { update: mockUpdate } as unknown as Transaction;
    const snap = makeSnapshot({ purchasedBalance: 50, promotionalBalance: 20, earnedBalance: 0, availableBalance: 70 });

    SnapshotRepository.updateInTx(tx, 'user-1', { purchasedDelta: 10 }, snap, 'tx-789');

    expect(mockUpdate).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        purchasedBalance: 60,
        availableBalance: 80,
        version: snap.version + 1,
        lastTransactionId: 'tx-789',
      }),
    );
  });

  it('should increment version by 1', () => {
    const mockUpdate = jest.fn();
    const tx = { update: mockUpdate } as unknown as Transaction;
    const snap = makeSnapshot({ version: 7 });

    SnapshotRepository.updateInTx(tx, 'user-1', {}, snap, 'tx-ver');

    expect(mockUpdate).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ version: 8 }),
    );
  });

  it('should keep availableBalance = purchased + promotional + earned', () => {
    const mockUpdate = jest.fn();
    const tx = { update: mockUpdate } as unknown as Transaction;
    const snap = makeSnapshot({ purchasedBalance: 100, promotionalBalance: 30, earnedBalance: 10, availableBalance: 140 });

    SnapshotRepository.updateInTx(tx, 'user-1', { purchasedDelta: -20, earnedDelta: 5 }, snap, 'tx-sum');

    const updateArg = mockUpdate.mock.calls[0][1];
    expect(updateArg.availableBalance).toBe(80 + 30 + 15); // 125
    expect(updateArg.purchasedBalance).toBe(80);
    expect(updateArg.earnedBalance).toBe(15);
  });
});

// ── LedgerEntryRepository — zero amount guard ─────────────────────────────────

describe('LedgerEntryRepository.createInTx', () => {
  it('should throw when amount is zero', () => {
    const tx = { set: jest.fn() } as unknown as Transaction;
    expect(() => {
      LedgerEntryRepository.createInTx(tx, {
        entryId: 'entry-1',
        transactionId: 'tx-1',
        walletId: 'user-1',
        entryType: 'credit',
        bucket: 'purchased',
        amount: 0,
        runningBalance: 0,
      });
    }).toThrow('zero');
  });

  it('should call tx.set when amount is positive', () => {
    const mockSet = jest.fn();
    const tx = { set: mockSet } as unknown as Transaction;
    LedgerEntryRepository.createInTx(tx, {
      entryId: 'entry-2',
      transactionId: 'tx-1',
      walletId: 'user-1',
      entryType: 'credit',
      bucket: 'purchased',
      amount: 10,
      runningBalance: 10,
    });
    expect(mockSet).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ amount: 10, entryId: 'entry-2', schemaVersion: 1 }),
    );
  });

  it('should call tx.set when amount is negative', () => {
    const mockSet = jest.fn();
    const tx = { set: mockSet } as unknown as Transaction;
    LedgerEntryRepository.createInTx(tx, {
      entryId: 'entry-3',
      transactionId: 'tx-1',
      walletId: 'user-1',
      entryType: 'debit',
      bucket: 'purchased',
      amount: -5,
      runningBalance: 5,
    });
    expect(mockSet).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ amount: -5 }),
    );
  });
});
