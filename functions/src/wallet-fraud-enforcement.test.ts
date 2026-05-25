/**
 * wallet-fraud-enforcement.test.ts
 *
 * Covers:
 *  - spendCoins: risk=allow → proceeds to transaction (no signal recorded)
 *  - spendCoins: risk=throttle → signal recorded; operation proceeds
 *  - spendCoins: risk=review → signal + audit recorded; operation proceeds
 *  - spendCoins: risk=freeze → wallet frozen + audited + FRAUD_DETECTED thrown; tx not called
 *  - initiatePurchase: risk=freeze → FRAUD_DETECTED; tx not called
 *  - verifyPurchase: risk=freeze → FRAUD_DETECTED; tx not called
 *  - Freeze: wallet.update called with status='frozen'
 *  - Freeze: AuditLogRepository.append called for 'wallet_auto_frozen_fraud'
 *  - throttle: AuditLogRepository.append NOT called
 *  - review: AuditLogRepository.append called for 'fraud_review_flagged'
 *  - FraudSignalService.record called once per signal
 */

// ── firebase-admin mock ───────────────────────────────────────────────────────

const mockWalletUpdate = jest.fn().mockResolvedValue(undefined);
const mockRunTransaction = jest.fn();

jest.mock('firebase-admin', () => ({
  initializeApp: jest.fn(),
  firestore: jest.fn(() => ({
    collection: jest.fn((name: string) => ({
      doc: jest.fn(() => ({
        update: (...args: unknown[]) => mockWalletUpdate(...args),
        path: `${name}/fake`,
      })),
    })),
    runTransaction: (...args: unknown[]) => mockRunTransaction(...args),
  })),
}));

// ── walletRepository mock ─────────────────────────────────────────────────────

const mockFindWalletInTx      = jest.fn();
const mockFindSnapshotInTx    = jest.fn();
const mockFindIdempotencyInTx = jest.fn();
const mockFindIdempotency     = jest.fn();
const mockFindPurchaseById    = jest.fn();
const mockFindPurchaseInTx    = jest.fn();
const mockAuditAppend         = jest.fn().mockResolvedValue(undefined);
const mockAuditAppendInTx     = jest.fn();

jest.mock('./lib/walletRepository', () => ({
  WalletRepository: {
    findByIdInTx:    (...a: unknown[]) => mockFindWalletInTx(...a),
    updateStatusInTx: jest.fn(),
  },
  SnapshotRepository: {
    findByWalletIdInTx: (...a: unknown[]) => mockFindSnapshotInTx(...a),
  },
  IdempotencyRepository: {
    find:                 (...a: unknown[]) => mockFindIdempotency(...a),
    findInTx:             (...a: unknown[]) => mockFindIdempotencyInTx(...a),
    createInProgressInTx: jest.fn(),
    completeInTx:         jest.fn(),
    failInTx:             jest.fn(),
  },
  PurchaseRepository: {
    findById:         (...a: unknown[]) => mockFindPurchaseById(...a),
    findByIdInTx:     (...a: unknown[]) => mockFindPurchaseInTx(...a),
    findTokenIndex:   jest.fn().mockResolvedValue(null),
    createInTx:       jest.fn(),
    updateStatusInTx: jest.fn(),
  },
  AuditLogRepository: {
    append:     (...a: unknown[]) => mockAuditAppend(...a),
    appendInTx: (...a: unknown[]) => mockAuditAppendInTx(...a),
  },
  LedgerTransactionRepository: { createInTx: jest.fn() },
  LedgerEntryRepository:       { createInTx: jest.fn() },
  FraudSignalRepository:       { append: jest.fn().mockResolvedValue('sig-1') },
  hashPurchaseToken:            jest.fn((r: string) => 'hash_' + r),
}));

// ── riskScoringService mock ───────────────────────────────────────────────────

const mockEvaluate = jest.fn();

jest.mock('./lib/riskScoringService', () => ({
  RiskScoringService: { evaluate: (...a: unknown[]) => mockEvaluate(...a) },
  RISK_THRESHOLDS: { throttle: 30, review: 60, freeze: 80 },
}));

// ── fraudSignalService mock ───────────────────────────────────────────────────

const mockRecordSignal = jest.fn().mockResolvedValue('sig-x');

jest.mock('./lib/fraudSignalService', () => ({
  FraudSignalService: { record: (...a: unknown[]) => mockRecordSignal(...a) },
}));

// ── Other dependency mocks ────────────────────────────────────────────────────

jest.mock('./lib/guards', () => ({
  requireAppCheck: jest.fn(),
  requireAuth: jest.fn(() => 'uid-alice'),
}));

jest.mock('./lib/rateLimiter', () => ({
  checkRateLimitFailClosed: jest.fn().mockResolvedValue(undefined),
}));

jest.mock('./lib/correlationId', () => ({
  resolveCorrelationId: jest.fn(() => 'corr-test'),
}));

jest.mock('./lib/walletValidators', () => ({
  validateSpendCoinsInput: jest.fn(() => ({
    idempotencyKey: 'idem-1', correlationId: 'corr-test', itemId: 'item-1', quantity: 1,
  })),
  validateInitiatePurchaseInput: jest.fn(() => ({
    idempotencyKey: 'idem-1', correlationId: 'corr-test', packId: 'coins_10',
  })),
  validateVerifyPurchaseInput: jest.fn(() => ({
    idempotencyKey: 'idem-1', correlationId: 'corr-test', sandboxPurchaseId: 'purchase-1',
  })),
  validateGetBalanceInput:      jest.fn(() => ({})),
  validateGetTransactionsInput: jest.fn(() => ({ limit: 20 })),
  PACK_CATALOG: { coins_10: { coins: 10, priceCents: 0 } },
}));

jest.mock('./lib/walletErrors', () => ({
  walletError: jest.fn((code: string) => {
    const err = new Error(code) as Error & { code: string };
    err.code = code;
    return err;
  }),
}));

jest.mock('./lib/walletLogger', () => ({
  walletLog: { info: jest.fn(), warn: jest.fn(), error: jest.fn() },
}));

jest.mock('./lib/idempotencyService', () => ({
  hashRequest:           jest.fn(() => 'req-hash'),
  checkIdempotencyPreTx: jest.fn().mockResolvedValue({ isReplay: false }),
  applyIdempotencyInTx:  jest.fn(() => ({ isReplay: false })),
}));

jest.mock('./lib/ledgerService', () => ({
  LedgerService: { applyInTx: jest.fn() },
}));

jest.mock('./lib/paymentProviders', () => ({
  getPaymentProvider: jest.fn(() => ({
    verify: jest.fn().mockResolvedValue({ isValid: true, coinsToCredit: 10 }),
  })),
}));

// ── Test setup ────────────────────────────────────────────────────────────────

import * as functionsTest from 'firebase-functions-test';
const testEnv = functionsTest();

import { spendCoins, initiatePurchase, verifyPurchase } from './walletFunctions';
import type { WalletDocument, BalanceSnapshotDocument, PurchaseDocument } from './lib/walletSchema';
import type { Transaction } from 'firebase-admin/firestore';

const TX = {} as unknown as Transaction;

const wrappedSpend    = testEnv.wrap(spendCoins);
const wrappedInitiate = testEnv.wrap(initiatePurchase);
const wrappedVerify   = testEnv.wrap(verifyPurchase);

const fakeContext = { auth: { uid: 'uid-alice', token: {} }, app: { appId: 'test-app' } };

// ── Assessment fixtures ───────────────────────────────────────────────────────

const ASSESS_ALLOW    = { score: 0,  decision: 'allow',    signals: [] };
const ASSESS_THROTTLE = {
  score: 30, decision: 'throttle', signals: [
    { type: 'velocity_purchases', contribution: 30, severity: 'medium', source: 'velocity', detail: 'x' },
  ],
};
const ASSESS_REVIEW = {
  score: 60, decision: 'review', signals: [
    { type: 'velocity_purchases', contribution: 40, severity: 'high',   source: 'velocity', detail: 'x' },
    { type: 'repeated_pack',      contribution: 20, severity: 'medium', source: 'behavior', detail: 'y' },
  ],
};
const ASSESS_FREEZE = {
  score: 90, decision: 'freeze', signals: [
    { type: 'velocity_purchases', contribution: 40, severity: 'high',   source: 'velocity', detail: 'a' },
    { type: 'repeated_pack',      contribution: 20, severity: 'medium', source: 'behavior', detail: 'b' },
    { type: 'velocity_spend',     contribution: 30, severity: 'medium', source: 'velocity', detail: 'c' },
  ],
};

// ── Wallet/Snapshot factories ─────────────────────────────────────────────────

function makeWallet(overrides: Partial<WalletDocument> = {}): WalletDocument {
  return {
    walletId: 'uid-alice', userId: 'uid-alice', status: 'active',
    createdAt: 0, updatedAt: 0, schemaVersion: 1, ...overrides,
  };
}
function makeSnapshot(overrides: Partial<BalanceSnapshotDocument> = {}): BalanceSnapshotDocument {
  return {
    walletId: 'uid-alice', availableBalance: 100,
    purchasedBalance: 100, promotionalBalance: 0, earnedBalance: 0,
    version: 1, lastTransactionId: null, updatedAt: 0, schemaVersion: 1, ...overrides,
  };
}
function makePurchase(overrides: Partial<PurchaseDocument> = {}): PurchaseDocument {
  return {
    purchaseId: 'purchase-1', walletId: 'uid-alice', userId: 'uid-alice',
    provider: 'sandbox', providerPurchaseTokenHash: 'hash_p1',
    providerTransactionId: null, packId: 'coins_10', coinsAmount: 10,
    priceCents: 0, currency: 'SANDBOX', status: 'initiated',
    ledgerTransactionId: null, createdAt: 0, updatedAt: 0, schemaVersion: 1, ...overrides,
  };
}

function setupSpendTransaction() {
  mockRunTransaction.mockImplementation(async (fn: (tx: Transaction) => unknown) => {
    mockFindIdempotencyInTx.mockResolvedValue(null);
    mockFindWalletInTx.mockResolvedValue(makeWallet());
    mockFindSnapshotInTx.mockResolvedValue(makeSnapshot());
    return fn(TX);
  });
}

function setupInitiateTransaction() {
  mockRunTransaction.mockImplementation(async (fn: (tx: Transaction) => unknown) => {
    mockFindIdempotencyInTx.mockResolvedValue(null);
    mockFindWalletInTx.mockResolvedValue(makeWallet());
    return fn(TX);
  });
  mockFindIdempotency.mockResolvedValue(null);
}

function setupVerifyTransaction() {
  mockFindPurchaseById.mockResolvedValue(makePurchase());
  mockFindIdempotency.mockResolvedValue(null);
  mockRunTransaction.mockImplementation(async (fn: (tx: Transaction) => unknown) => {
    mockFindIdempotencyInTx.mockResolvedValue(null);
    mockFindWalletInTx.mockResolvedValue(makeWallet());
    mockFindSnapshotInTx.mockResolvedValue(makeSnapshot());
    mockFindPurchaseInTx.mockResolvedValue(makePurchase());
    return fn(TX);
  });
}

// ── spendCoins enforcement tests ──────────────────────────────────────────────

describe('spendCoins — fraud enforcement', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockRecordSignal.mockResolvedValue('sig-x');
    mockAuditAppend.mockResolvedValue(undefined);
    mockWalletUpdate.mockResolvedValue(undefined);
  });

  it('allow: no signal recorded; transaction called', async () => {
    mockEvaluate.mockResolvedValue(ASSESS_ALLOW);
    setupSpendTransaction();

    await wrappedSpend({ itemId: 'item-1', quantity: 1 }, fakeContext);

    expect(mockRecordSignal).not.toHaveBeenCalled();
    expect(mockWalletUpdate).not.toHaveBeenCalled();
    expect(mockRunTransaction).toHaveBeenCalled();
  });

  it('throttle: signal recorded; operation proceeds to transaction', async () => {
    mockEvaluate.mockResolvedValue(ASSESS_THROTTLE);
    setupSpendTransaction();

    await wrappedSpend({ itemId: 'item-1', quantity: 1 }, fakeContext);

    expect(mockRecordSignal).toHaveBeenCalledTimes(1); // 1 signal
    expect(mockRunTransaction).toHaveBeenCalled();
    expect(mockWalletUpdate).not.toHaveBeenCalled();
    // No review audit for throttle
    expect(mockAuditAppend).not.toHaveBeenCalledWith(
      expect.anything(), expect.anything(), 'fraud_review_flagged',
      expect.anything(), expect.anything(), expect.anything(),
    );
  });

  it('review: signals recorded + audit appended; operation proceeds', async () => {
    mockEvaluate.mockResolvedValue(ASSESS_REVIEW);
    setupSpendTransaction();

    await wrappedSpend({ itemId: 'item-1', quantity: 1 }, fakeContext);

    expect(mockRecordSignal).toHaveBeenCalledTimes(2); // 2 signals
    expect(mockAuditAppend).toHaveBeenCalledWith(
      'system', 'system', 'fraud_review_flagged',
      'uid-alice', 'corr-test',
      expect.objectContaining({ action: 'spendCoins', score: 60 }),
    );
    expect(mockRunTransaction).toHaveBeenCalled();
    expect(mockWalletUpdate).not.toHaveBeenCalled();
  });

  it('freeze: wallet frozen + audited + FRAUD_DETECTED thrown; tx NOT called', async () => {
    mockEvaluate.mockResolvedValue(ASSESS_FREEZE);

    await expect(
      wrappedSpend({ itemId: 'item-1', quantity: 1 }, fakeContext),
    ).rejects.toThrow('FRAUD_DETECTED');

    expect(mockWalletUpdate).toHaveBeenCalledWith(
      expect.objectContaining({ status: 'frozen' }),
    );
    expect(mockAuditAppend).toHaveBeenCalledWith(
      'system', 'system', 'wallet_auto_frozen_fraud',
      'uid-alice', 'corr-test',
      expect.objectContaining({ action: 'spendCoins', score: 90 }),
    );
    expect(mockRecordSignal).toHaveBeenCalledTimes(3); // 3 signals
    expect(mockRunTransaction).not.toHaveBeenCalled();
  });
});

// ── initiatePurchase enforcement tests ───────────────────────────────────────

describe('initiatePurchase — fraud enforcement', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockRecordSignal.mockResolvedValue('sig-x');
    mockAuditAppend.mockResolvedValue(undefined);
    mockWalletUpdate.mockResolvedValue(undefined);
  });

  it('freeze: FRAUD_DETECTED thrown; tx NOT called', async () => {
    mockEvaluate.mockResolvedValue(ASSESS_FREEZE);

    await expect(
      wrappedInitiate({ packId: 'coins_10' }, fakeContext),
    ).rejects.toThrow('FRAUD_DETECTED');

    expect(mockWalletUpdate).toHaveBeenCalledWith(
      expect.objectContaining({ status: 'frozen' }),
    );
    expect(mockAuditAppend).toHaveBeenCalledWith(
      'system', 'system', 'wallet_auto_frozen_fraud',
      'uid-alice', 'corr-test',
      expect.objectContaining({ action: 'initiatePurchase' }),
    );
    expect(mockRunTransaction).not.toHaveBeenCalled();
  });

  it('allow: proceeds to transaction', async () => {
    mockEvaluate.mockResolvedValue(ASSESS_ALLOW);
    setupInitiateTransaction();

    await wrappedInitiate({ packId: 'coins_10' }, fakeContext);

    expect(mockRunTransaction).toHaveBeenCalled();
    expect(mockWalletUpdate).not.toHaveBeenCalled();
  });
});

// ── verifyPurchase enforcement tests ─────────────────────────────────────────

describe('verifyPurchase — fraud enforcement', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockRecordSignal.mockResolvedValue('sig-x');
    mockAuditAppend.mockResolvedValue(undefined);
    mockWalletUpdate.mockResolvedValue(undefined);
  });

  it('freeze: FRAUD_DETECTED thrown; purchase NOT credited', async () => {
    mockEvaluate.mockResolvedValue(ASSESS_FREEZE);

    await expect(
      wrappedVerify({ sandboxPurchaseId: 'purchase-1' }, fakeContext),
    ).rejects.toThrow('FRAUD_DETECTED');

    expect(mockWalletUpdate).toHaveBeenCalledWith(
      expect.objectContaining({ status: 'frozen' }),
    );
    expect(mockAuditAppend).toHaveBeenCalledWith(
      'system', 'system', 'wallet_auto_frozen_fraud',
      'uid-alice', 'corr-test',
      expect.objectContaining({ action: 'verifyPurchase' }),
    );
    expect(mockRunTransaction).not.toHaveBeenCalled();
  });

  it('allow: proceeds to purchase lookup and transaction', async () => {
    mockEvaluate.mockResolvedValue(ASSESS_ALLOW);
    setupVerifyTransaction();

    await wrappedVerify({ sandboxPurchaseId: 'purchase-1' }, fakeContext);

    expect(mockRunTransaction).toHaveBeenCalled();
    expect(mockWalletUpdate).not.toHaveBeenCalled();
  });
});
