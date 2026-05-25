/**
 * wallet-purchase.test.ts
 * Tests for initiatePurchase and verifyPurchase callables.
 * Covers: auth, input validation, wallet state, cross-user protection,
 *         double-credit, idempotency, successful initiation and credit.
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

const mockFindWalletInTx      = jest.fn();
const mockFindSnapshotInTx    = jest.fn();
const mockFindIdempotencyInTx = jest.fn();
const mockFindPurchaseInTx    = jest.fn();
const mockFindIdempotency     = jest.fn();
const mockFindPurchaseById    = jest.fn();
const mockCreatePurchaseInTx  = jest.fn();
const mockUpdateStatusInTx    = jest.fn();
const mockCompleteInTx        = jest.fn();
const mockAuditAppendInTx     = jest.fn();

jest.mock('./lib/walletRepository', () => ({
  WalletRepository: {
    findByIdInTx: (...args: unknown[]) => mockFindWalletInTx(...args),
  },
  SnapshotRepository: {
    findByWalletIdInTx: (...args: unknown[]) => mockFindSnapshotInTx(...args),
  },
  IdempotencyRepository: {
    find:                (...args: unknown[]) => mockFindIdempotency(...args),
    findInTx:            (...args: unknown[]) => mockFindIdempotencyInTx(...args),
    createInProgressInTx: jest.fn(),
    completeInTx:        (...args: unknown[]) => mockCompleteInTx(...args),
    failInTx:            jest.fn(),
  },
  PurchaseRepository: {
    findById:       (...args: unknown[]) => mockFindPurchaseById(...args),
    findByIdInTx:   (...args: unknown[]) => mockFindPurchaseInTx(...args),
    findTokenIndex: jest.fn().mockResolvedValue(null),
    createInTx:     (...args: unknown[]) => mockCreatePurchaseInTx(...args),
    updateStatusInTx: (...args: unknown[]) => mockUpdateStatusInTx(...args),
  },
  AuditLogRepository: {
    appendInTx: (...args: unknown[]) => mockAuditAppendInTx(...args),
  },
  LedgerTransactionRepository: { createInTx: jest.fn() },
  LedgerEntryRepository:        { createInTx: jest.fn() },
  hashPurchaseToken:             jest.fn((raw: string) => 'hash_of_' + raw),
}));

// ── Mock LedgerService ────────────────────────────────────────────────────────

const mockLedgerApplyInTx = jest.fn();

jest.mock('./lib/ledgerService', () => ({
  LedgerService: {
    applyInTx: (...args: unknown[]) => mockLedgerApplyInTx(...args),
  },
}));

// ── Mock paymentProviders ─────────────────────────────────────────────────────

const mockProviderVerify = jest.fn();

jest.mock('./lib/paymentProviders', () => ({
  getPaymentProvider: jest.fn(() => ({
    providerName: 'sandbox',
    verify: (...args: unknown[]) => mockProviderVerify(...args),
    refundStatus: jest.fn(),
  })),
}));

// ── Mock rate limiter ─────────────────────────────────────────────────────────

jest.mock('./lib/rateLimiter', () => ({
  checkRateLimitFailClosed: jest.fn().mockResolvedValue(undefined),
}));

import * as functionsTest from 'firebase-functions-test';
const testEnv = functionsTest();

import { initiatePurchase, verifyPurchase } from './walletFunctions';
import type {
  WalletDocument,
  BalanceSnapshotDocument,
  IdempotencyKeyDocument,
  PurchaseDocument,
} from './lib/walletSchema';
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
  walletId: 'user-123', availableBalance: 0, purchasedBalance: 0,
  promotionalBalance: 0, earnedBalance: 0, version: 1,
  lastTransactionId: null, updatedAt: Date.now(), schemaVersion: 1,
  ...overrides,
});

const makePurchase = (overrides: Partial<PurchaseDocument> = {}): PurchaseDocument => ({
  purchaseId: 'purchase-uuid-001', walletId: 'user-123', userId: 'user-123',
  provider: 'sandbox', providerPurchaseTokenHash: 'hash_of_sandbox_xxx',
  providerTransactionId: null, packId: 'coins_10', coinsAmount: 10,
  priceCents: 0, currency: 'SANDBOX', status: 'initiated',
  ledgerTransactionId: null, createdAt: Date.now(), updatedAt: Date.now(), schemaVersion: 1,
  ...overrides,
});

const TX = {} as unknown as Transaction;

const VALID_INITIATE = { packId: 'coins_10', idempotencyKey: 'init-idem-001' };
const VALID_VERIFY   = { sandboxPurchaseId: 'purchase-uuid-001', idempotencyKey: 'verify-idem-001' };

function setupInitiateTransaction(walletOverride?: Partial<WalletDocument>) {
  mockRunTransaction.mockImplementation(async (fn: (tx: Transaction) => Promise<unknown>) => {
    mockFindIdempotencyInTx.mockResolvedValue(null);
    mockFindWalletInTx.mockResolvedValue(makeWallet(walletOverride));
    return fn(TX);
  });
}

function setupVerifyTransaction(
  walletOverride?: Partial<WalletDocument>,
  purchaseOverride?: Partial<PurchaseDocument>,
) {
  mockRunTransaction.mockImplementation(async (fn: (tx: Transaction) => Promise<unknown>) => {
    mockFindIdempotencyInTx.mockResolvedValue(null);
    mockFindWalletInTx.mockResolvedValue(makeWallet(walletOverride));
    mockFindSnapshotInTx.mockResolvedValue(makeSnapshot());
    mockFindPurchaseInTx.mockResolvedValue(makePurchase(purchaseOverride));
    return fn(TX);
  });
}

const wrappedInitiate = testEnv.wrap(initiatePurchase);
const wrappedVerify   = testEnv.wrap(verifyPurchase);

// ── Global reset ──────────────────────────────────────────────────────────────

beforeEach(() => {
  jest.clearAllMocks();
  mockFindIdempotency.mockResolvedValue(null);
  mockFindPurchaseById.mockResolvedValue(makePurchase());
  mockProviderVerify.mockResolvedValue({
    isValid: true,
    coinsToCredit: 10,
    providerTransactionId: 'purchase-uuid-001',
    sanitizedResponse: { sandbox: true },
  });
});

// ═════════════════════════════════════════════════════════════════════════════
// initiatePurchase
// ═════════════════════════════════════════════════════════════════════════════

describe('initiatePurchase — auth & App Check', () => {
  it('should throw unauthenticated when no auth', async () => {
    await expect(
      wrappedInitiate(VALID_INITIATE, { auth: undefined, app: { appId: 'x' } }),
    ).rejects.toMatchObject({ code: 'unauthenticated' });
  });

  it('should throw unauthenticated when App Check absent', async () => {
    await expect(
      wrappedInitiate(VALID_INITIATE, { auth: { uid: 'user-1' }, app: undefined }),
    ).rejects.toMatchObject({ code: 'unauthenticated' });
  });
});

describe('initiatePurchase — input validation', () => {
  it('should reject unknown packId', async () => {
    await expect(
      wrappedInitiate({ ...VALID_INITIATE, packId: 'coins_9999' }, makeContext()),
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });

  it('should reject forbidden field "coinsAmount" from client', async () => {
    await expect(
      wrappedInitiate({ ...VALID_INITIATE, coinsAmount: 999 }, makeContext()),
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });

  it('should reject missing idempotencyKey', async () => {
    await expect(
      wrappedInitiate({ packId: 'coins_10' }, makeContext()),
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });

  it('should reject missing packId', async () => {
    await expect(
      wrappedInitiate({ idempotencyKey: 'k-001' }, makeContext()),
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });
});

describe('initiatePurchase — wallet state validation', () => {
  it('should throw WALLET_FROZEN for frozen wallet', async () => {
    setupInitiateTransaction({ status: 'frozen' });
    await expect(wrappedInitiate(VALID_INITIATE, makeContext())).rejects.toMatchObject({ code: 'failed-precondition' });
  });

  it('should throw WALLET_SUSPENDED for suspended wallet', async () => {
    setupInitiateTransaction({ status: 'suspended' });
    await expect(wrappedInitiate(VALID_INITIATE, makeContext())).rejects.toMatchObject({ code: 'permission-denied' });
  });

  it('should throw WALLET_NOT_FOUND when wallet is null', async () => {
    mockRunTransaction.mockImplementation(async (fn: (tx: Transaction) => Promise<unknown>) => {
      mockFindIdempotencyInTx.mockResolvedValue(null);
      mockFindWalletInTx.mockResolvedValue(null);
      return fn(TX);
    });
    await expect(wrappedInitiate(VALID_INITIATE, makeContext())).rejects.toMatchObject({ code: 'not-found' });
  });
});

describe('initiatePurchase — successful initiation', () => {
  beforeEach(() => { setupInitiateTransaction(); });

  it('should return purchaseId, sandboxToken, packId, coinsAmount', async () => {
    const result = await wrappedInitiate(VALID_INITIATE, makeContext());
    expect(typeof result.purchaseId).toBe('string');
    expect(typeof result.sandboxToken).toBe('string');
    expect(result.packId).toBe('coins_10');
    expect(result.coinsAmount).toBe(10);
  });

  it('should return sandboxToken starting with "sandbox_"', async () => {
    const result = await wrappedInitiate(VALID_INITIATE, makeContext());
    expect(result.sandboxToken).toMatch(/^sandbox_/);
  });

  it('should derive coinsAmount from server catalog, not from client data', async () => {
    // Even if client tried to pass coinsAmount, validator rejects it; server uses catalog
    const result = await wrappedInitiate(VALID_INITIATE, makeContext());
    expect(result.coinsAmount).toBe(10); // coins_10 → 10 from PACK_CATALOG
  });

  it('should call PurchaseRepository.createInTx with provider="sandbox" and status="initiated"', async () => {
    await wrappedInitiate(VALID_INITIATE, makeContext());
    expect(mockCreatePurchaseInTx).toHaveBeenCalledWith(
      TX,
      expect.objectContaining({
        provider:   'sandbox',
        status:     'initiated',
        packId:     'coins_10',
        coinsAmount: 10,
        priceCents:  0,
        currency:    'SANDBOX',
        walletId:    'user-123',
      }),
    );
  });

  it('should call AuditLogRepository.appendInTx with purchase_initiated action', async () => {
    await wrappedInitiate(VALID_INITIATE, makeContext());
    expect(mockAuditAppendInTx).toHaveBeenCalledWith(
      TX,
      'user-123',
      'user',
      'purchase_initiated',
      'user-123',
      expect.any(String),
      expect.objectContaining({ packId: 'coins_10', coinsAmount: 10 }),
    );
  });

  it('should call IdempotencyRepository.completeInTx', async () => {
    await wrappedInitiate(VALID_INITIATE, makeContext());
    expect(mockCompleteInTx).toHaveBeenCalledWith(
      TX,
      'user-123',
      'init-idem-001',
      expect.any(String),
      expect.objectContaining({ packId: 'coins_10', coinsAmount: 10 }),
    );
  });

  it('should use callerUid as walletId — never from client data', async () => {
    await wrappedInitiate(VALID_INITIATE, makeContext('specific-uid'));
    expect(mockFindWalletInTx).toHaveBeenCalledWith(TX, 'specific-uid');
  });

  it('should NOT include sandboxToken in PurchaseRepository (only hash stored)', async () => {
    await wrappedInitiate(VALID_INITIATE, makeContext());
    const callArg = mockCreatePurchaseInTx.mock.calls[0][1] as Record<string, unknown>;
    expect(callArg).not.toHaveProperty('sandboxToken');
    expect(callArg).not.toHaveProperty('providerPurchaseToken');
    // The hash is stored (providerPurchaseTokenHash) but not the raw token
    expect(callArg).toHaveProperty('providerPurchaseTokenHash');
  });
});

describe('initiatePurchase — idempotency', () => {
  it('should return cached response on pre-tx replay (same key+body)', async () => {
    const cached = { purchaseId: 'cached-pid', sandboxToken: 'sandbox_cached', packId: 'coins_10', coinsAmount: 10, correlationId: 'c-1' };
    const { hashRequest: realHash } = jest.requireActual('./lib/idempotencyService') as { hashRequest: (d: unknown) => string };
    const hash = realHash({ packId: 'coins_10' });

    mockFindIdempotency.mockResolvedValue({
      status: 'completed',
      requestHash: hash,
      responsePayload: cached,
      transactionId: 'cached-pid',
    } as Partial<IdempotencyKeyDocument>);

    const result = await wrappedInitiate(VALID_INITIATE, makeContext());
    expect(result.purchaseId).toBe('cached-pid');
    expect(mockRunTransaction).not.toHaveBeenCalled();
  });

  it('should throw conflict when same key used with different packId', async () => {
    const { hashRequest: realHash } = jest.requireActual('./lib/idempotencyService') as { hashRequest: (d: unknown) => string };
    const otherHash = realHash({ packId: 'coins_100' });

    mockFindIdempotency.mockResolvedValue({
      status: 'completed',
      requestHash: otherHash,
      responsePayload: {},
      transactionId: 'tx-other',
    } as Partial<IdempotencyKeyDocument>);

    await expect(wrappedInitiate(VALID_INITIATE, makeContext())).rejects.toMatchObject({ code: 'aborted' });
  });

  it('should throw concurrent error when key is in_progress', async () => {
    const { hashRequest: realHash } = jest.requireActual('./lib/idempotencyService') as { hashRequest: (d: unknown) => string };
    const hash = realHash({ packId: 'coins_10' });

    mockFindIdempotency.mockResolvedValue({
      status: 'in_progress',
      requestHash: hash,
    } as Partial<IdempotencyKeyDocument>);

    await expect(wrappedInitiate(VALID_INITIATE, makeContext())).rejects.toMatchObject({ code: 'already-exists' });
  });
});

// ═════════════════════════════════════════════════════════════════════════════
// verifyPurchase
// ═════════════════════════════════════════════════════════════════════════════

describe('verifyPurchase — auth & App Check', () => {
  it('should throw unauthenticated when no auth', async () => {
    await expect(
      wrappedVerify(VALID_VERIFY, { auth: undefined, app: { appId: 'x' } }),
    ).rejects.toMatchObject({ code: 'unauthenticated' });
  });

  it('should throw unauthenticated when App Check absent', async () => {
    await expect(
      wrappedVerify(VALID_VERIFY, { auth: { uid: 'user-1' }, app: undefined }),
    ).rejects.toMatchObject({ code: 'unauthenticated' });
  });
});

describe('verifyPurchase — input validation', () => {
  it('should reject forbidden field "coinsAmount" from client', async () => {
    await expect(
      wrappedVerify({ ...VALID_VERIFY, coinsAmount: 999 }, makeContext()),
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });

  it('should reject missing sandboxPurchaseId', async () => {
    await expect(
      wrappedVerify({ idempotencyKey: 'k-001' }, makeContext()),
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });

  it('should reject missing idempotencyKey', async () => {
    await expect(
      wrappedVerify({ sandboxPurchaseId: 'purchase-uuid-001' }, makeContext()),
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });
});

describe('verifyPurchase — cross-user protection', () => {
  it('should throw PURCHASE_NOT_FOUND when purchase belongs to a different user', async () => {
    // Purchase is owned by another user — must return PURCHASE_NOT_FOUND to hide existence
    mockFindPurchaseById.mockResolvedValue(makePurchase({ walletId: 'other-user-999' }));
    await expect(wrappedVerify(VALID_VERIFY, makeContext('user-123'))).rejects.toMatchObject({ code: 'not-found' });
  });

  it('should throw PURCHASE_NOT_FOUND (not a leaking error) for cross-user access', async () => {
    mockFindPurchaseById.mockResolvedValue(makePurchase({ walletId: 'attacker-uid' }));
    const err = await wrappedVerify(VALID_VERIFY, makeContext('victim-uid')).catch((e: unknown) => e);
    // Must be not-found, not permission-denied — avoids confirming the purchase exists
    expect(err.code).toBe('not-found');
  });

  it('should throw PURCHASE_NOT_FOUND when purchase does not exist', async () => {
    mockFindPurchaseById.mockResolvedValue(null);
    await expect(wrappedVerify(VALID_VERIFY, makeContext())).rejects.toMatchObject({ code: 'not-found' });
  });
});

describe('verifyPurchase — double-credit protection', () => {
  it('should throw PURCHASE_ALREADY_CREDITED (pre-tx) when purchase is already credited', async () => {
    mockFindPurchaseById.mockResolvedValue(makePurchase({ status: 'credited' }));
    await expect(wrappedVerify(VALID_VERIFY, makeContext())).rejects.toMatchObject({ code: 'already-exists' });
  });

  it('should throw PURCHASE_ALREADY_CREDITED inside transaction (race condition protection)', async () => {
    // Pre-read shows 'initiated' but by the time we enter the transaction, it's 'credited'
    mockFindPurchaseById.mockResolvedValue(makePurchase({ status: 'initiated' }));
    setupVerifyTransaction(undefined, { status: 'credited' });

    await expect(wrappedVerify(VALID_VERIFY, makeContext())).rejects.toMatchObject({ code: 'already-exists' });
  });
});

describe('verifyPurchase — wallet state validation (inside transaction)', () => {
  it('should throw WALLET_FROZEN for frozen wallet', async () => {
    setupVerifyTransaction({ status: 'frozen' });
    await expect(wrappedVerify(VALID_VERIFY, makeContext())).rejects.toMatchObject({ code: 'failed-precondition' });
  });

  it('should throw WALLET_SUSPENDED for suspended wallet', async () => {
    setupVerifyTransaction({ status: 'suspended' });
    await expect(wrappedVerify(VALID_VERIFY, makeContext())).rejects.toMatchObject({ code: 'permission-denied' });
  });
});

describe('verifyPurchase — successful credit', () => {
  beforeEach(() => { setupVerifyTransaction(); });

  it('should return transactionId, coinsReceived, and purchaseId', async () => {
    const result = await wrappedVerify(VALID_VERIFY, makeContext());
    expect(typeof result.transactionId).toBe('string');
    expect(result.coinsReceived).toBe(10);
    expect(result.purchaseId).toBe('purchase-uuid-001');
  });

  it('should call LedgerService.applyInTx with purchase_credit type and purchased bucket', async () => {
    await wrappedVerify(VALID_VERIFY, makeContext());
    expect(mockLedgerApplyInTx).toHaveBeenCalledWith(
      TX,
      'user-123',
      expect.anything(),
      expect.objectContaining({
        type:    'purchase_credit',
        entries: [{ bucket: 'purchased', amount: 10 }],
      }),
    );
  });

  it('should call PurchaseRepository.updateStatusInTx with "credited"', async () => {
    await wrappedVerify(VALID_VERIFY, makeContext());
    expect(mockUpdateStatusInTx).toHaveBeenCalledWith(
      TX,
      'purchase-uuid-001',
      'credited',
      expect.any(String),
    );
  });

  it('should call AuditLogRepository.appendInTx with purchase_credited action', async () => {
    await wrappedVerify(VALID_VERIFY, makeContext());
    expect(mockAuditAppendInTx).toHaveBeenCalledWith(
      TX,
      'user-123',
      'user',
      'purchase_credited',
      'user-123',
      expect.any(String),
      expect.objectContaining({ coinsReceived: 10, purchaseId: 'purchase-uuid-001' }),
    );
  });

  it('should call IdempotencyRepository.completeInTx', async () => {
    await wrappedVerify(VALID_VERIFY, makeContext());
    expect(mockCompleteInTx).toHaveBeenCalledWith(
      TX,
      'user-123',
      'verify-idem-001',
      expect.any(String),
      expect.objectContaining({ coinsReceived: 10 }),
    );
  });

  it('should use callerUid as walletId — never from client data', async () => {
    mockFindPurchaseById.mockResolvedValue(makePurchase({ walletId: 'specific-uid', userId: 'specific-uid' }));
    setupVerifyTransaction(
      { walletId: 'specific-uid', userId: 'specific-uid' },
      { walletId: 'specific-uid', userId: 'specific-uid' },
    );
    await wrappedVerify(VALID_VERIFY, makeContext('specific-uid'));
    expect(mockFindWalletInTx).toHaveBeenCalledWith(TX, 'specific-uid');
  });

  it('should credit coinsToCredit from provider result, not from client data', async () => {
    // Provider says 100 coins; result must honor that
    mockProviderVerify.mockResolvedValue({
      isValid: true,
      coinsToCredit: 100,
      providerTransactionId: 'purchase-uuid-001',
      sanitizedResponse: { sandbox: true },
    });
    mockFindPurchaseById.mockResolvedValue(makePurchase({ packId: 'coins_100', coinsAmount: 100 }));
    setupVerifyTransaction(undefined, { packId: 'coins_100', coinsAmount: 100 });

    const result = await wrappedVerify(VALID_VERIFY, makeContext());
    expect(result.coinsReceived).toBe(100);
    expect(mockLedgerApplyInTx).toHaveBeenCalledWith(
      TX, 'user-123', expect.anything(),
      expect.objectContaining({ entries: [{ bucket: 'purchased', amount: 100 }] }),
    );
  });
});

describe('verifyPurchase — idempotency', () => {
  it('should return cached response on pre-tx replay (same key+body)', async () => {
    const cached = { transactionId: 'tx-cached', coinsReceived: 10, purchaseId: 'purchase-uuid-001', correlationId: 'c-1' };
    const { hashRequest: realHash } = jest.requireActual('./lib/idempotencyService') as { hashRequest: (d: unknown) => string };
    const hash = realHash({ sandboxPurchaseId: 'purchase-uuid-001' });

    mockFindIdempotency.mockResolvedValue({
      status: 'completed',
      requestHash: hash,
      responsePayload: cached,
      transactionId: 'tx-cached',
    } as Partial<IdempotencyKeyDocument>);

    const result = await wrappedVerify(VALID_VERIFY, makeContext());
    expect(result.transactionId).toBe('tx-cached');
    expect(mockRunTransaction).not.toHaveBeenCalled();
  });

  it('should throw conflict when same key used with different sandboxPurchaseId', async () => {
    const { hashRequest: realHash } = jest.requireActual('./lib/idempotencyService') as { hashRequest: (d: unknown) => string };
    const otherHash = realHash({ sandboxPurchaseId: 'different-purchase-id' });

    mockFindIdempotency.mockResolvedValue({
      status: 'completed',
      requestHash: otherHash,
      responsePayload: {},
      transactionId: 'tx-other',
    } as Partial<IdempotencyKeyDocument>);

    await expect(wrappedVerify(VALID_VERIFY, makeContext())).rejects.toMatchObject({ code: 'aborted' });
  });
});
