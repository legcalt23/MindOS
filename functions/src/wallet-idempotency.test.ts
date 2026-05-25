/**
 * wallet-idempotency.test.ts
 * Tests for the idempotency service: hashing, state-machine decisions,
 * pre-transaction checks, and within-transaction enforcement.
 */

// ── Mock firebase-admin before any imports that touch it ──────────────────────

const mockDocRef = { path: 'fake/path' };
const mockCollection = jest.fn(() => ({ doc: jest.fn(() => mockDocRef) }));

jest.mock('firebase-admin', () => ({
  firestore: jest.fn(() => ({ collection: mockCollection })),
  initializeApp: jest.fn(),
}));

// ── Mock IdempotencyRepository so we control what find() returns ──────────────

const mockFind = jest.fn();

jest.mock('./lib/walletRepository', () => ({
  IdempotencyRepository: {
    find: (...args: unknown[]) => mockFind(...args),
    createInProgressInTx: jest.fn(),
    completeInTx: jest.fn(),
    failInTx: jest.fn(),
  },
}));

import {
  hashRequest,
  resolveIdempotencyDecision,
  checkIdempotencyPreTx,
  applyIdempotencyInTx,
} from './lib/idempotencyService';
import type { IdempotencyKeyDocument } from './lib/walletSchema';
import type { Transaction } from 'firebase-admin/firestore';
import { IdempotencyRepository } from './lib/walletRepository';

// ── Helpers ───────────────────────────────────────────────────────────────────

const HASH_A = 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa'.slice(0, 64);
const HASH_B = 'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb'.slice(0, 64);

function makeIdempotencyDoc(
  overrides: Partial<IdempotencyKeyDocument> = {},
): IdempotencyKeyDocument {
  return {
    docId: 'user-1_key-1',
    key: 'key-1',
    walletId: 'user-1',
    action: 'spendCoins',
    requestHash: HASH_A,
    status: 'completed',
    responsePayload: { transactionId: 'tx-cached', coinsSpent: 10 },
    transactionId: 'tx-cached',
    createdAt: Date.now(),
    expiresAt: Date.now() + 86400000,
    schemaVersion: 1,
    ...overrides,
  };
}

// ── hashRequest ───────────────────────────────────────────────────────────────

describe('hashRequest', () => {
  it('should produce a 64-char hex SHA-256', () => {
    expect(hashRequest({ packId: 'coins_10' })).toMatch(/^[a-f0-9]{64}$/);
  });

  it('should be deterministic', () => {
    const data = { packId: 'coins_10', idempotencyKey: 'key-abc' };
    expect(hashRequest(data)).toBe(hashRequest(data));
  });

  it('should be order-independent (canonical key sort)', () => {
    const a = hashRequest({ packId: 'coins_10', idempotencyKey: 'key-abc' });
    const b = hashRequest({ idempotencyKey: 'key-abc', packId: 'coins_10' });
    expect(a).toBe(b);
  });

  it('should produce different hashes for different inputs', () => {
    expect(hashRequest({ packId: 'coins_10' })).not.toBe(hashRequest({ packId: 'coins_100' }));
  });

  it('should handle nested objects with key ordering', () => {
    const a = hashRequest({ meta: { z: 1, a: 2 } });
    const b = hashRequest({ meta: { a: 2, z: 1 } });
    expect(a).toBe(b);
  });

  it('should handle null and primitive inputs', () => {
    expect(hashRequest(null)).toMatch(/^[a-f0-9]{64}$/);
    expect(hashRequest('string')).toMatch(/^[a-f0-9]{64}$/);
    expect(hashRequest(42)).toMatch(/^[a-f0-9]{64}$/);
  });
});

// ── resolveIdempotencyDecision ────────────────────────────────────────────────

describe('resolveIdempotencyDecision', () => {
  it('should return proceed when no existing document', () => {
    const result = resolveIdempotencyDecision(null, HASH_A);
    expect(result.kind).toBe('proceed');
  });

  it('should return concurrent when status is in_progress', () => {
    const doc = makeIdempotencyDoc({ status: 'in_progress' });
    const result = resolveIdempotencyDecision(doc, HASH_A);
    expect(result.kind).toBe('concurrent');
  });

  it('should return conflict when completed and hash differs', () => {
    const doc = makeIdempotencyDoc({ status: 'completed', requestHash: HASH_A });
    const result = resolveIdempotencyDecision(doc, HASH_B);
    expect(result.kind).toBe('conflict');
  });

  it('should return replay when completed and hash matches', () => {
    const doc = makeIdempotencyDoc({ status: 'completed', requestHash: HASH_A });
    const result = resolveIdempotencyDecision(doc, HASH_A);
    expect(result.kind).toBe('replay');
    if (result.kind === 'replay') {
      expect(result.transactionId).toBe('tx-cached');
      expect(result.response).toEqual({ transactionId: 'tx-cached', coinsSpent: 10 });
    }
  });

  it('should return conflict when failed and hash differs', () => {
    const doc = makeIdempotencyDoc({ status: 'failed', requestHash: HASH_A });
    const result = resolveIdempotencyDecision(doc, HASH_B);
    expect(result.kind).toBe('conflict');
  });

  it('should return proceed when failed and hash matches (safe retry)', () => {
    const doc = makeIdempotencyDoc({ status: 'failed', requestHash: HASH_A });
    const result = resolveIdempotencyDecision(doc, HASH_A);
    expect(result.kind).toBe('proceed');
  });
});

// ── checkIdempotencyPreTx ─────────────────────────────────────────────────────

describe('checkIdempotencyPreTx', () => {
  beforeEach(() => mockFind.mockReset());

  it('should return isReplay: false when no existing document', async () => {
    mockFind.mockResolvedValue(null);
    const result = await checkIdempotencyPreTx('user-1', 'key-1', HASH_A, 'corr-1');
    expect(result.isReplay).toBe(false);
  });

  it('should return replay result when completed + same hash', async () => {
    mockFind.mockResolvedValue(makeIdempotencyDoc({ status: 'completed', requestHash: HASH_A }));
    const result = await checkIdempotencyPreTx('user-1', 'key-1', HASH_A, 'corr-1');
    expect(result.isReplay).toBe(true);
    if (result.isReplay) {
      expect(result.transactionId).toBe('tx-cached');
    }
  });

  it('should throw IDEMPOTENCY_KEY_CONFLICT when completed + different hash', async () => {
    mockFind.mockResolvedValue(makeIdempotencyDoc({ status: 'completed', requestHash: HASH_A }));
    await expect(
      checkIdempotencyPreTx('user-1', 'key-1', HASH_B, 'corr-1'),
    ).rejects.toMatchObject({ code: 'aborted' });
  });

  it('should throw DUPLICATE_IDEMPOTENCY_KEY when in_progress', async () => {
    mockFind.mockResolvedValue(makeIdempotencyDoc({ status: 'in_progress' }));
    await expect(
      checkIdempotencyPreTx('user-1', 'key-1', HASH_A, 'corr-1'),
    ).rejects.toMatchObject({ code: 'already-exists' });
  });

  it('should return isReplay: false when failed + same hash (retry allowed)', async () => {
    mockFind.mockResolvedValue(makeIdempotencyDoc({ status: 'failed', requestHash: HASH_A }));
    const result = await checkIdempotencyPreTx('user-1', 'key-1', HASH_A, 'corr-1');
    expect(result.isReplay).toBe(false);
  });

  it('should throw IDEMPOTENCY_KEY_CONFLICT when failed + different hash', async () => {
    mockFind.mockResolvedValue(makeIdempotencyDoc({ status: 'failed', requestHash: HASH_A }));
    await expect(
      checkIdempotencyPreTx('user-1', 'key-1', HASH_B, 'corr-1'),
    ).rejects.toMatchObject({ code: 'aborted' });
  });

  it('should pass walletId and key to repository', async () => {
    mockFind.mockResolvedValue(null);
    await checkIdempotencyPreTx('wallet-xyz', 'my-key', HASH_A, 'corr-1');
    expect(mockFind).toHaveBeenCalledWith('wallet-xyz', 'my-key');
  });
});

// ── applyIdempotencyInTx ──────────────────────────────────────────────────────

describe('applyIdempotencyInTx', () => {
  const mockTx = {} as unknown as Transaction;

  beforeEach(() => {
    (IdempotencyRepository.createInProgressInTx as jest.Mock).mockClear();
  });

  it('should create in_progress and return proceed when no existing doc', () => {
    const result = applyIdempotencyInTx(
      mockTx, null, 'user-1', 'key-1', 'spendCoins', HASH_A, 'corr-1',
    );
    expect(result.isReplay).toBe(false);
    expect(IdempotencyRepository.createInProgressInTx).toHaveBeenCalledWith(
      mockTx, 'user-1', 'key-1', 'spendCoins', HASH_A,
    );
  });

  it('should return replay result when completed + same hash without writing', () => {
    const doc = makeIdempotencyDoc({ status: 'completed', requestHash: HASH_A });
    const result = applyIdempotencyInTx(
      mockTx, doc, 'user-1', 'key-1', 'spendCoins', HASH_A, 'corr-1',
    );
    expect(result.isReplay).toBe(true);
    if (result.isReplay) {
      expect(result.transactionId).toBe('tx-cached');
    }
    expect(IdempotencyRepository.createInProgressInTx).not.toHaveBeenCalled();
  });

  it('should throw IDEMPOTENCY_KEY_CONFLICT when completed + different hash', () => {
    const doc = makeIdempotencyDoc({ status: 'completed', requestHash: HASH_A });
    expect(() =>
      applyIdempotencyInTx(mockTx, doc, 'user-1', 'key-1', 'spendCoins', HASH_B, 'corr-1'),
    ).toThrow();
    expect(IdempotencyRepository.createInProgressInTx).not.toHaveBeenCalled();
  });

  it('should throw DUPLICATE_IDEMPOTENCY_KEY when in_progress', () => {
    const doc = makeIdempotencyDoc({ status: 'in_progress' });
    expect(() =>
      applyIdempotencyInTx(mockTx, doc, 'user-1', 'key-1', 'spendCoins', HASH_A, 'corr-1'),
    ).toThrow();
    expect(IdempotencyRepository.createInProgressInTx).not.toHaveBeenCalled();
  });

  it('should create in_progress when failed + same hash (retry)', () => {
    const doc = makeIdempotencyDoc({ status: 'failed', requestHash: HASH_A });
    const result = applyIdempotencyInTx(
      mockTx, doc, 'user-1', 'key-1', 'spendCoins', HASH_A, 'corr-1',
    );
    expect(result.isReplay).toBe(false);
    expect(IdempotencyRepository.createInProgressInTx).toHaveBeenCalled();
  });

  it('should throw IDEMPOTENCY_KEY_CONFLICT when failed + different hash', () => {
    const doc = makeIdempotencyDoc({ status: 'failed', requestHash: HASH_A });
    expect(() =>
      applyIdempotencyInTx(mockTx, doc, 'user-1', 'key-1', 'spendCoins', HASH_B, 'corr-1'),
    ).toThrow();
    expect(IdempotencyRepository.createInProgressInTx).not.toHaveBeenCalled();
  });
});
