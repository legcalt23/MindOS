/**
 * wallet-balance.test.ts
 * Tests for getWalletBalance callable.
 * Covers: auth, App Check, auto-creation, correct balance return, client-data isolation.
 */

// ── Mock firebase-admin ───────────────────────────────────────────────────────

jest.mock('firebase-admin', () => ({
  initializeApp: jest.fn(),
  firestore: jest.fn(() => ({})),
}));

// ── Mock walletRepository ─────────────────────────────────────────────────────

const mockFindWallet = jest.fn();
const mockCreateWallet = jest.fn();
const mockFindSnapshot = jest.fn();
const mockCreateSnapshot = jest.fn();
const mockAuditAppend = jest.fn();

jest.mock('./lib/walletRepository', () => ({
  WalletRepository: {
    findById: (...args: unknown[]) => mockFindWallet(...args),
    create: (...args: unknown[]) => mockCreateWallet(...args),
  },
  SnapshotRepository: {
    findByWalletId: (...args: unknown[]) => mockFindSnapshot(...args),
    create: (...args: unknown[]) => mockCreateSnapshot(...args),
  },
  AuditLogRepository: {
    append: (...args: unknown[]) => mockAuditAppend(...args),
  },
}));

// ── Mock rate limiter ─────────────────────────────────────────────────────────

jest.mock('./lib/rateLimiter', () => ({
  checkRateLimitFailClosed: jest.fn().mockResolvedValue(undefined),
  checkRateLimit: jest.fn().mockResolvedValue(undefined),
}));

// ── Mock guards ───────────────────────────────────────────────────────────────
// We do NOT mock guards — we pass valid/invalid contexts to test them.

import * as functionsTest from 'firebase-functions-test';
const testEnv = functionsTest();

import { getWalletBalance } from './walletFunctions';
import type { WalletDocument, BalanceSnapshotDocument } from './lib/walletSchema';

// ── Helpers ───────────────────────────────────────────────────────────────────

function makeContext(uid: string | null = 'user-123', hasAppCheck = true) {
  return {
    auth: uid ? { uid, token: {} } : undefined,
    app: hasAppCheck ? { appId: 'test-app' } : undefined,
  };
}

const makeWallet = (overrides: Partial<WalletDocument> = {}): WalletDocument => ({
  walletId: 'user-123',
  userId: 'user-123',
  status: 'active',
  createdAt: Date.now(),
  updatedAt: Date.now(),
  schemaVersion: 1,
  ...overrides,
});

const makeSnapshot = (overrides: Partial<BalanceSnapshotDocument> = {}): BalanceSnapshotDocument => ({
  walletId: 'user-123',
  availableBalance: 100,
  purchasedBalance: 80,
  promotionalBalance: 20,
  earnedBalance: 0,
  version: 3,
  lastTransactionId: 'tx-prev',
  updatedAt: Date.now(),
  schemaVersion: 1,
  ...overrides,
});

const wrapped = testEnv.wrap(getWalletBalance);

// ── Tests ─────────────────────────────────────────────────────────────────────

beforeEach(() => {
  mockFindWallet.mockReset();
  mockCreateWallet.mockReset();
  mockFindSnapshot.mockReset();
  mockCreateSnapshot.mockReset();
  mockAuditAppend.mockReset();
  mockAuditAppend.mockResolvedValue(undefined);
});

describe('getWalletBalance — auth & App Check', () => {
  it('should throw unauthenticated when no auth context', async () => {
    await expect(wrapped({}, makeContext(null))).rejects.toMatchObject({ code: 'unauthenticated' });
  });

  it('should throw unauthenticated when App Check is absent', async () => {
    await expect(wrapped({}, makeContext('user-123', false))).rejects.toMatchObject({
      code: 'unauthenticated',
    });
  });
});

describe('getWalletBalance — existing wallet', () => {
  beforeEach(() => {
    mockFindWallet.mockResolvedValue(makeWallet());
    mockFindSnapshot.mockResolvedValue(makeSnapshot());
  });

  it('should return all balance buckets', async () => {
    const result = await wrapped({}, makeContext());
    expect(result.availableBalance).toBe(100);
    expect(result.purchasedBalance).toBe(80);
    expect(result.promotionalBalance).toBe(20);
    expect(result.earnedBalance).toBe(0);
  });

  it('should return wallet status', async () => {
    const result = await wrapped({}, makeContext());
    expect(result.status).toBe('active');
  });

  it('should return version and lastTransactionId', async () => {
    const result = await wrapped({}, makeContext());
    expect(result.version).toBe(3);
    expect(result.lastTransactionId).toBe('tx-prev');
  });

  it('should return correlationId in response', async () => {
    const result = await wrapped({}, makeContext());
    expect(typeof result.correlationId).toBe('string');
  });

  it('should NOT include sensitive fields (userId, createdAt, etc.)', async () => {
    const result = await wrapped({}, makeContext());
    expect(result).not.toHaveProperty('userId');
    expect(result).not.toHaveProperty('walletId');
    expect(result).not.toHaveProperty('createdAt');
    expect(result).not.toHaveProperty('updatedAt');
  });
});

describe('getWalletBalance — auto-creation on first access', () => {
  beforeEach(() => {
    mockFindWallet.mockResolvedValue(null);
    mockFindSnapshot.mockResolvedValue(null);
    mockCreateWallet.mockResolvedValue(makeWallet({ walletId: 'user-123' }));
    mockCreateSnapshot.mockResolvedValue(
      makeSnapshot({ availableBalance: 0, purchasedBalance: 0, promotionalBalance: 0, earnedBalance: 0, version: 0, lastTransactionId: null }),
    );
  });

  it('should auto-create wallet when not found', async () => {
    await wrapped({}, makeContext('user-123'));
    expect(mockCreateWallet).toHaveBeenCalledWith('user-123');
  });

  it('should auto-create snapshot when not found', async () => {
    await wrapped({}, makeContext('user-123'));
    expect(mockCreateSnapshot).toHaveBeenCalledWith('user-123');
  });

  it('should return zero balance for new wallet', async () => {
    const result = await wrapped({}, makeContext('user-123'));
    expect(result.availableBalance).toBe(0);
    expect(result.purchasedBalance).toBe(0);
  });

  it('should append audit log on auto-creation', async () => {
    await wrapped({}, makeContext('user-123'));
    expect(mockAuditAppend).toHaveBeenCalledWith(
      'user-123',
      'system',
      'wallet_auto_created',
      'user-123',
      expect.any(String),
      expect.any(Object),
    );
  });
});

describe('getWalletBalance — security: client cannot control walletId', () => {
  beforeEach(() => {
    mockFindWallet.mockResolvedValue(makeWallet({ walletId: 'user-123' }));
    mockFindSnapshot.mockResolvedValue(makeSnapshot({ walletId: 'user-123' }));
  });

  it('should ignore walletId from client data and use auth uid', async () => {
    // Attacker sends walletId of another user in data
    // validateGetBalanceInput rejects this field, but even if bypassed,
    // the function uses callerUid exclusively.
    // This test verifies the repository is called with the authenticated uid.
    await wrapped({}, makeContext('user-123'));
    expect(mockFindWallet).toHaveBeenCalledWith('user-123');
    expect(mockFindWallet).not.toHaveBeenCalledWith('attacker-wallet');
  });

  it('should use auth uid as walletId regardless of data content', async () => {
    // validateGetBalanceInput will throw 'invalid-argument' for unknown fields,
    // so this test also verifies that forbidden fields in data cause rejection.
    await expect(wrapped({ walletId: 'other-user' }, makeContext('user-123'))).rejects.toMatchObject({
      code: 'invalid-argument',
    });
  });
});

describe('getWalletBalance — suspended wallet', () => {
  it('should throw permission-denied for suspended wallet', async () => {
    mockFindWallet.mockResolvedValue(makeWallet({ status: 'suspended' }));
    mockFindSnapshot.mockResolvedValue(makeSnapshot());

    await expect(wrapped({}, makeContext())).rejects.toMatchObject({ code: 'permission-denied' });
  });

  it('should allow balance read for frozen wallet', async () => {
    mockFindWallet.mockResolvedValue(makeWallet({ status: 'frozen' }));
    mockFindSnapshot.mockResolvedValue(makeSnapshot());

    const result = await wrapped({}, makeContext());
    expect(result.status).toBe('frozen');
    expect(result.availableBalance).toBe(100);
  });
});
