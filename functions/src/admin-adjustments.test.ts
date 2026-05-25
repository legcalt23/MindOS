/**
 * admin-adjustments.test.ts
 *
 * Covers:
 *  - requestAdjustment: validates amount, justification, target wallet exists
 *  - requestAdjustment: creates pending action and emits requested audit
 *  - approveAdjustment: maker ≠ checker enforced — self-approval REJECTED
 *  - approveAdjustment: self-approval is recorded as 'admin_adjustment_rejected'
 *  - approveAdjustment: auto-execute permitted only when amount ≤ threshold
 *    AND allowSelfExecuteLow=true AND caller is maker
 *  - approveAdjustment: successful approval calls LedgerService (NO direct
 *    snapshot mutation) and emits approved + executed audit
 *  - approveAdjustment: already-executed action rejects with conflict
 *  - approveAdjustment: missing action rejects
 *  - approveAdjustment: target wallet must exist
 */

// ── firebase-admin mock ───────────────────────────────────────────────────────

const mockRunTransaction = jest.fn();

jest.mock('firebase-admin', () => ({
  initializeApp: jest.fn(),
  firestore: jest.fn(() => ({
    collection: jest.fn(() => ({ doc: jest.fn(() => ({ path: 'fake' })) })),
    runTransaction: (...a: unknown[]) => mockRunTransaction(...a),
  })),
}));

jest.mock('firebase-functions', () => ({
  logger: { info: jest.fn(), warn: jest.fn(), error: jest.fn() },
  https: {
    HttpsError: class extends Error {
      code: string;
      constructor(code: string, message: string) {
        super(message);
        this.code = code;
      }
    },
  },
}));

// ── walletRepository mock ─────────────────────────────────────────────────────

const mockFindWallet         = jest.fn();
const mockFindWalletInTx     = jest.fn();
const mockFindSnapshotInTx   = jest.fn();
const mockFindActionById     = jest.fn();
const mockFindActionByIdInTx = jest.fn();
const mockCreateAction       = jest.fn().mockResolvedValue(undefined);
const mockUpdateActionStatus = jest.fn();
const mockAuditAppend        = jest.fn().mockResolvedValue(undefined);
const mockAuditAppendInTx    = jest.fn();

jest.mock('./lib/walletRepository', () => ({
  WalletRepository: {
    findById:        (...a: unknown[]) => mockFindWallet(...a),
    findByIdInTx:    (...a: unknown[]) => mockFindWalletInTx(...a),
  },
  SnapshotRepository: {
    findByWalletIdInTx: (...a: unknown[]) => mockFindSnapshotInTx(...a),
  },
  AdminActionRepository: {
    findById:        (...a: unknown[]) => mockFindActionById(...a),
    findByIdInTx:    (...a: unknown[]) => mockFindActionByIdInTx(...a),
    create:          (...a: unknown[]) => mockCreateAction(...a),
    updateStatusInTx: (...a: unknown[]) => mockUpdateActionStatus(...a),
  },
  AuditLogRepository: {
    append:     (...a: unknown[]) => mockAuditAppend(...a),
    appendInTx: (...a: unknown[]) => mockAuditAppendInTx(...a),
  },
}));

// ── LedgerService mock ────────────────────────────────────────────────────────

const mockLedgerApplyInTx = jest.fn();

jest.mock('./lib/ledgerService', () => ({
  LedgerService: { applyInTx: (...a: unknown[]) => mockLedgerApplyInTx(...a) },
}));

// ── walletErrors mock ─────────────────────────────────────────────────────────

jest.mock('./lib/walletErrors', () => ({
  walletError: jest.fn((code: string) => {
    const err = new Error(code) as Error & { code: string };
    err.code = code;
    return err;
  }),
}));

// ── Imports ───────────────────────────────────────────────────────────────────

import {
  AdminAdjustmentService,
  AUTO_EXECUTE_THRESHOLD,
  MAX_ADJUSTMENT_AMOUNT,
} from './lib/adminAdjustmentService';

import type { Transaction } from 'firebase-admin/firestore';
const TX = {} as unknown as Transaction;

// ── Helpers ───────────────────────────────────────────────────────────────────

const makeWallet = (overrides: Record<string, unknown> = {}) => ({
  walletId: 'w-target', userId: 'w-target', status: 'active',
  createdAt: 0, updatedAt: 0, schemaVersion: 1, ...overrides,
});

const makeSnapshot = (overrides: Record<string, unknown> = {}) => ({
  walletId: 'w-target', availableBalance: 100,
  purchasedBalance: 100, promotionalBalance: 0, earnedBalance: 0,
  version: 1, lastTransactionId: null, updatedAt: 0, schemaVersion: 1,
  ...overrides,
});

const makeAction = (overrides: Record<string, unknown> = {}) => ({
  actionId: 'act-1', adminId: 'maker-1', action: 'wallet_adjustment',
  targetWalletId: 'w-target', amount: 5, justification: 'fixing test balance',
  status: 'pending', approverId: null,
  createdAt: 0, resolvedAt: null, schemaVersion: 1, ...overrides,
});

function setupApproveTx(actionOverride: Record<string, unknown> = {}, walletOverride: Record<string, unknown> = {}) {
  mockRunTransaction.mockImplementation(async (fn: (tx: Transaction) => unknown) => {
    mockFindActionByIdInTx.mockResolvedValue(makeAction(actionOverride));
    mockFindWalletInTx.mockResolvedValue(makeWallet(walletOverride));
    mockFindSnapshotInTx.mockResolvedValue(makeSnapshot());
    return fn(TX);
  });
}

beforeEach(() => {
  jest.clearAllMocks();
});

// ── requestAdjustment ─────────────────────────────────────────────────────────

describe('requestAdjustment — validation', () => {
  it('rejects zero amount', async () => {
    await expect(AdminAdjustmentService.requestAdjustment({
      adminId: 'admin-a', targetWalletId: 'w', amount: 0,
      justification: 'this is long enough', correlationId: 'c',
    })).rejects.toThrow(/INVALID_WALLET_INPUT/);
  });

  it('rejects non-integer amount', async () => {
    await expect(AdminAdjustmentService.requestAdjustment({
      adminId: 'admin-a', targetWalletId: 'w', amount: 1.5,
      justification: 'this is long enough', correlationId: 'c',
    })).rejects.toThrow(/INVALID_WALLET_INPUT/);
  });

  it('rejects amount above MAX_ADJUSTMENT_AMOUNT', async () => {
    await expect(AdminAdjustmentService.requestAdjustment({
      adminId: 'admin-a', targetWalletId: 'w', amount: MAX_ADJUSTMENT_AMOUNT + 1,
      justification: 'this is long enough', correlationId: 'c',
    })).rejects.toThrow(/INVALID_WALLET_INPUT/);
  });

  it('rejects justification shorter than MIN_JUSTIFICATION_LENGTH', async () => {
    await expect(AdminAdjustmentService.requestAdjustment({
      adminId: 'admin-a', targetWalletId: 'w', amount: 5,
      justification: 'short', correlationId: 'c',
    })).rejects.toThrow(/INVALID_WALLET_INPUT/);
  });

  it('rejects justification that is whitespace-only', async () => {
    await expect(AdminAdjustmentService.requestAdjustment({
      adminId: 'admin-a', targetWalletId: 'w', amount: 5,
      justification: '              ', correlationId: 'c',
    })).rejects.toThrow(/INVALID_WALLET_INPUT/);
  });

  it('rejects when target wallet does not exist', async () => {
    mockFindWallet.mockResolvedValue(null);
    await expect(AdminAdjustmentService.requestAdjustment({
      adminId: 'admin-a', targetWalletId: 'w-missing', amount: 5,
      justification: 'this is long enough', correlationId: 'c',
    })).rejects.toThrow(/WALLET_NOT_FOUND/);
  });
});

describe('requestAdjustment — success path', () => {
  beforeEach(() => {
    mockFindWallet.mockResolvedValue(makeWallet());
  });

  it('creates pending action and emits requested audit', async () => {
    const result = await AdminAdjustmentService.requestAdjustment({
      adminId: 'maker-1', targetWalletId: 'w-target', amount: 50,
      justification: 'compensating user for outage 2026-05-24',
      correlationId: 'corr-req',
    });

    expect(result.status).toBe('pending');
    expect(result.actionId).toBeDefined();
    expect(typeof result.actionId).toBe('string');

    expect(mockCreateAction).toHaveBeenCalledWith(expect.objectContaining({
      adminId:       'maker-1',
      targetWalletId: 'w-target',
      amount:        50,
      status:        'pending',
      approverId:    null,
    }));

    expect(mockAuditAppend).toHaveBeenCalledWith(
      'maker-1', 'admin', 'admin_adjustment_requested',
      'w-target', 'corr-req',
      expect.objectContaining({ amount: 50 }),
    );
  });

  it('trims justification before storing', async () => {
    await AdminAdjustmentService.requestAdjustment({
      adminId: 'maker-1', targetWalletId: 'w-target', amount: 10,
      justification: '   leading and trailing   ',
      correlationId: 'c',
    });
    const [createArgs] = mockCreateAction.mock.calls[0];
    expect((createArgs as { justification: string }).justification)
      .toBe('leading and trailing');
  });
});

// ── approveAdjustment — self-approval ─────────────────────────────────────────

describe('approveAdjustment — self-approval guard (maker ≠ checker)', () => {
  it('REJECTS self-approval for amount above auto-execute threshold', async () => {
    // Maker and checker are the same UID; amount > threshold
    mockFindActionById.mockResolvedValue(makeAction({
      adminId: 'maker-1',
      amount:  AUTO_EXECUTE_THRESHOLD + 50,
      status:  'pending',
    }));

    // The service marks the action 'rejected' in a transaction. We capture
    // the tx callback to verify it ran.
    let txCallback: ((tx: Transaction) => unknown) | null = null;
    mockRunTransaction.mockImplementation(async (fn: (tx: Transaction) => unknown) => {
      txCallback = fn;
      return fn(TX);
    });

    await expect(AdminAdjustmentService.approveAdjustment({
      adminId: 'maker-1',          // SAME AS MAKER
      actionId: 'act-1',
      correlationId: 'c',
    })).rejects.toThrow(/FRAUD_DETECTED/);

    // Verify the rejection transaction ran
    expect(txCallback).not.toBeNull();
    expect(mockUpdateActionStatus).toHaveBeenCalledWith(
      TX, 'act-1', 'rejected', 'maker-1',
    );
    // Audit must record the self-approval attempt
    expect(mockAuditAppendInTx).toHaveBeenCalledWith(
      TX, 'maker-1', 'admin', 'admin_adjustment_rejected',
      'w-target', 'c',
      expect.objectContaining({ reason: 'self_approval_not_permitted' }),
    );
    // CRITICAL: ledger was NOT touched
    expect(mockLedgerApplyInTx).not.toHaveBeenCalled();
  });

  it('REJECTS self-approval when allowSelfExecuteLow=false even on low amount', async () => {
    mockFindActionById.mockResolvedValue(makeAction({
      adminId: 'maker-1',
      amount:  AUTO_EXECUTE_THRESHOLD - 1,
      status:  'pending',
    }));
    mockRunTransaction.mockImplementation(async (fn: (tx: Transaction) => unknown) => fn(TX));

    await expect(AdminAdjustmentService.approveAdjustment({
      adminId: 'maker-1',
      actionId: 'act-1',
      allowSelfExecuteLow: false, // explicitly not auto-executing
      correlationId: 'c',
    })).rejects.toThrow(/FRAUD_DETECTED/);

    expect(mockLedgerApplyInTx).not.toHaveBeenCalled();
  });
});

// ── approveAdjustment — auto-execute low value ────────────────────────────────

describe('approveAdjustment — low-value auto-execute', () => {
  it('PERMITS self-execute when amount ≤ threshold AND allowSelfExecuteLow=true', async () => {
    mockFindActionById.mockResolvedValue(makeAction({
      adminId: 'maker-1', amount: AUTO_EXECUTE_THRESHOLD, status: 'pending',
    }));
    setupApproveTx({ adminId: 'maker-1', amount: AUTO_EXECUTE_THRESHOLD });

    const result = await AdminAdjustmentService.approveAdjustment({
      adminId: 'maker-1',                  // same as maker
      actionId: 'act-1',
      allowSelfExecuteLow: true,
      correlationId: 'c',
    });

    expect(result.status).toBe('executed');
    expect(result.ledgerTransactionId).toBeDefined();

    // Ledger applied
    expect(mockLedgerApplyInTx).toHaveBeenCalledTimes(1);
    const [, walletIdArg, , specArg] = mockLedgerApplyInTx.mock.calls[0];
    expect(walletIdArg).toBe('w-target');
    expect(specArg).toMatchObject({
      type: 'admin_adjustment',
      entries: [{ bucket: 'promotional', amount: AUTO_EXECUTE_THRESHOLD }],
    });

    // Action moved to executed by the maker (acting as their own approver for low-value)
    expect(mockUpdateActionStatus).toHaveBeenCalledWith(
      TX, 'act-1', 'executed', 'maker-1',
    );

    // Approved + executed audits both emitted
    const auditActions = mockAuditAppendInTx.mock.calls.map((c) => c[3]);
    expect(auditActions).toContain('admin_adjustment_approved');
    expect(auditActions).toContain('admin_adjustment_executed');
  });
});

// ── approveAdjustment — happy path (different checker) ───────────────────────

describe('approveAdjustment — maker/checker happy path', () => {
  it('executes adjustment via LedgerService (NEVER direct snapshot write)', async () => {
    mockFindActionById.mockResolvedValue(makeAction({
      adminId: 'maker-1', amount: 200, status: 'pending',
    }));
    setupApproveTx({ adminId: 'maker-1', amount: 200 });

    const result = await AdminAdjustmentService.approveAdjustment({
      adminId: 'checker-2',  // DIFFERENT from maker
      actionId: 'act-1',
      correlationId: 'corr-app',
    });

    expect(result.status).toBe('executed');
    expect(result.ledgerTransactionId).toBeDefined();

    // CRITICAL: LedgerService used (not SnapshotRepository.updateInTx directly)
    expect(mockLedgerApplyInTx).toHaveBeenCalledTimes(1);
    const [, walletId, , spec] = mockLedgerApplyInTx.mock.calls[0];
    expect(walletId).toBe('w-target');
    expect(spec).toMatchObject({
      type: 'admin_adjustment',
      idempotencyKey: 'admin_action_act-1',
      entries: [{ bucket: 'promotional', amount: 200 }],
    });
    expect((spec as { metadata: Record<string, unknown> }).metadata).toMatchObject({
      actionId:        'act-1',
      makerAdminId:    'maker-1',
      approverAdminId: 'checker-2',
    });

    // Action moved to executed by the checker
    expect(mockUpdateActionStatus).toHaveBeenCalledWith(
      TX, 'act-1', 'executed', 'checker-2',
    );

    // Both audits emitted
    expect(mockAuditAppendInTx).toHaveBeenCalledWith(
      TX, 'checker-2', 'admin', 'admin_adjustment_approved',
      'w-target', 'corr-app',
      expect.objectContaining({ makerAdminId: 'maker-1' }),
    );
    expect(mockAuditAppendInTx).toHaveBeenCalledWith(
      TX, 'checker-2', 'admin', 'admin_adjustment_executed',
      'w-target', 'corr-app',
      expect.objectContaining({
        transactionId: result.ledgerTransactionId,
        makerAdminId:  'maker-1',
        approverAdminId: 'checker-2',
      }),
    );
  });

  it('handles negative amounts (debits) correctly', async () => {
    mockFindActionById.mockResolvedValue(makeAction({
      adminId: 'maker-1', amount: -30, status: 'pending',
    }));
    setupApproveTx({ adminId: 'maker-1', amount: -30 }, { status: 'active' });

    await AdminAdjustmentService.approveAdjustment({
      adminId: 'checker-2', actionId: 'act-1', correlationId: 'c',
    });

    const [, , , spec] = mockLedgerApplyInTx.mock.calls[0];
    expect((spec as { entries: { amount: number }[] }).entries[0].amount).toBe(-30);
  });
});

// ── approveAdjustment — error paths ───────────────────────────────────────────

describe('approveAdjustment — error paths', () => {
  it('rejects when action does not exist', async () => {
    mockFindActionById.mockResolvedValue(null);
    await expect(AdminAdjustmentService.approveAdjustment({
      adminId: 'checker-2', actionId: 'missing', correlationId: 'c',
    })).rejects.toThrow(/WALLET_NOT_FOUND/);
  });

  it('rejects when action is already executed', async () => {
    mockFindActionById.mockResolvedValue(makeAction({ status: 'executed' }));
    await expect(AdminAdjustmentService.approveAdjustment({
      adminId: 'checker-2', actionId: 'act-1', correlationId: 'c',
    })).rejects.toThrow(/IDEMPOTENCY_KEY_CONFLICT/);
  });

  it('rejects when action is already rejected', async () => {
    mockFindActionById.mockResolvedValue(makeAction({ status: 'rejected' }));
    await expect(AdminAdjustmentService.approveAdjustment({
      adminId: 'checker-2', actionId: 'act-1', correlationId: 'c',
    })).rejects.toThrow(/IDEMPOTENCY_KEY_CONFLICT/);
  });

  it('rejects when target wallet is suspended', async () => {
    mockFindActionById.mockResolvedValue(makeAction({ adminId: 'maker-1' }));
    setupApproveTx({ adminId: 'maker-1' }, { status: 'suspended' });

    await expect(AdminAdjustmentService.approveAdjustment({
      adminId: 'checker-2', actionId: 'act-1', correlationId: 'c',
    })).rejects.toThrow();
    // Ledger must not have been written for suspended wallet
    expect(mockLedgerApplyInTx).not.toHaveBeenCalled();
  });
});
