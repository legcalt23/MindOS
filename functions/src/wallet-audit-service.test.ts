/**
 * wallet-audit-service.test.ts
 *
 * Covers:
 *  - sanitizeDetails: sensitive keys redacted, non-sensitive passed through
 *  - sanitizeDetails: recursive sanitization of nested objects and arrays
 *  - sanitizeDetails: null/undefined preserved; primitives passed through
 *  - AuditLogService.record: calls repository.append with sanitized details
 *  - AuditLogService.recordInTx: calls repository.appendInTx with sanitized details
 *  - AuditLogService.recordRefundSkeleton: emits refund_skeleton audit with note
 *  - Each AuditAction enum value can be emitted (type safety)
 *  - Sanitization NEVER stores raw tokens or secrets
 */

// ── Mocks ─────────────────────────────────────────────────────────────────────

const mockAppend     = jest.fn().mockResolvedValue(undefined);
const mockAppendInTx = jest.fn();

jest.mock('firebase-admin', () => ({
  initializeApp: jest.fn(),
  firestore: jest.fn(() => ({
    collection: jest.fn(() => ({ doc: jest.fn(() => ({ path: 'fake' })) })),
  })),
}));

jest.mock('./lib/walletRepository', () => ({
  AuditLogRepository: {
    append:     (...a: unknown[]) => mockAppend(...a),
    appendInTx: (...a: unknown[]) => mockAppendInTx(...a),
  },
}));

// ── Imports ───────────────────────────────────────────────────────────────────

import {
  sanitizeDetails,
  AuditLogService,
  AuditAction,
} from './lib/auditLogService';

import type { Transaction } from 'firebase-admin/firestore';
const TX = {} as unknown as Transaction;

// ── sanitizeDetails ───────────────────────────────────────────────────────────

describe('sanitizeDetails', () => {
  beforeEach(() => jest.clearAllMocks());

  it('redacts keys containing "token"', () => {
    const out = sanitizeDetails({ sandboxToken: 'sandbox_xyz_abc' });
    expect(out['sandboxToken']).toBe('<redacted>');
  });

  it('redacts keys containing "secret"', () => {
    const out = sanitizeDetails({ webhookSecret: 'sk_live_123' });
    expect(out['webhookSecret']).toBe('<redacted>');
  });

  it('redacts keys containing "password"', () => {
    const out = sanitizeDetails({ password: 'p@ssw0rd' });
    expect(out['password']).toBe('<redacted>');
  });

  it('redacts keys containing "credential"', () => {
    const out = sanitizeDetails({ credentialFile: '/path/to/cred.json' });
    expect(out['credentialFile']).toBe('<redacted>');
  });

  it('redacts keys containing "authorization"', () => {
    const out = sanitizeDetails({ authorization: 'Bearer xyz' });
    expect(out['authorization']).toBe('<redacted>');
  });

  it('redacts keys containing "apikey" (case-insensitive)', () => {
    const out = sanitizeDetails({ apiKey: 'AIza...', externalApiKEY: 'k' });
    expect(out['apiKey']).toBe('<redacted>');
    expect(out['externalApiKEY']).toBe('<redacted>');
  });

  it('redacts accessToken / refreshToken / idToken', () => {
    const out = sanitizeDetails({
      accessToken:  'a',
      refreshToken: 'r',
      idToken:      'i',
    });
    expect(out['accessToken']).toBe('<redacted>');
    expect(out['refreshToken']).toBe('<redacted>');
    expect(out['idToken']).toBe('<redacted>');
  });

  it('passes through non-sensitive keys unchanged', () => {
    const out = sanitizeDetails({
      packId:        'coins_10',
      coinsAmount:   10,
      transactionId: 'uuid-1',
    });
    expect(out['packId']).toBe('coins_10');
    expect(out['coinsAmount']).toBe(10);
    expect(out['transactionId']).toBe('uuid-1');
  });

  it('recurses into nested objects', () => {
    const out = sanitizeDetails({
      metadata: { token: 't', packId: 'p' },
    });
    expect((out['metadata'] as Record<string, unknown>)['token']).toBe('<redacted>');
    expect((out['metadata'] as Record<string, unknown>)['packId']).toBe('p');
  });

  it('sanitizes object items inside arrays', () => {
    const out = sanitizeDetails({
      events: [
        { token: 't1', name: 'e1' },
        { token: 't2', name: 'e2' },
      ],
    });
    const events = out['events'] as Array<Record<string, unknown>>;
    expect(events[0]['token']).toBe('<redacted>');
    expect(events[0]['name']).toBe('e1');
    expect(events[1]['token']).toBe('<redacted>');
  });

  it('preserves primitive array elements unchanged', () => {
    const out = sanitizeDetails({ ids: [1, 2, 'three'] });
    expect(out['ids']).toEqual([1, 2, 'three']);
  });

  it('preserves null and undefined values', () => {
    const out = sanitizeDetails({ a: null, b: undefined, c: 0 });
    expect(out['a']).toBeNull();
    expect(out['b']).toBeUndefined();
    expect(out['c']).toBe(0);
  });

  it('returns an empty object for empty input', () => {
    expect(sanitizeDetails({})).toEqual({});
  });

  it('NEVER allows raw tokens to leak via case-only differences', () => {
    const out = sanitizeDetails({
      Token:      'T1',
      TOKEN:      'T2',
      authToken:  'T3',
      myTokenXYZ: 'T4',
    });
    Object.values(out).forEach((v) => expect(v).toBe('<redacted>'));
  });
});

// ── AuditLogService.record ────────────────────────────────────────────────────

describe('AuditLogService.record', () => {
  beforeEach(() => jest.clearAllMocks());

  it('calls AuditLogRepository.append with sanitized details', async () => {
    await AuditLogService.record({
      actorId:        'user-abc',
      actorType:      'user',
      action:         'spend_coins',
      targetWalletId: 'user-abc',
      correlationId:  'corr-1',
      details:        { itemId: 'i1', token: 'secret_xyz' },
    });

    expect(mockAppend).toHaveBeenCalledWith(
      'user-abc',
      'user',
      'spend_coins',
      'user-abc',
      'corr-1',
      { itemId: 'i1', token: '<redacted>' },
    );
  });

  it('treats missing details as empty object', async () => {
    await AuditLogService.record({
      actorId:        'sys',
      actorType:      'system',
      action:         'wallet_auto_created',
      targetWalletId: 'wallet-1',
      correlationId:  'corr-2',
    });

    expect(mockAppend).toHaveBeenCalledWith(
      'sys', 'system', 'wallet_auto_created', 'wallet-1', 'corr-2', {},
    );
  });

  it('awaits the repository call (propagates rejection)', async () => {
    mockAppend.mockRejectedValueOnce(new Error('firestore down'));
    await expect(
      AuditLogService.record({
        actorId: 'a', actorType: 'system', action: 'wallet_auto_created',
        targetWalletId: 'w', correlationId: 'c', details: {},
      }),
    ).rejects.toThrow('firestore down');
  });
});

// ── AuditLogService.recordInTx ────────────────────────────────────────────────

describe('AuditLogService.recordInTx', () => {
  beforeEach(() => jest.clearAllMocks());

  it('calls AuditLogRepository.appendInTx with sanitized details', () => {
    AuditLogService.recordInTx(TX, {
      actorId:        'user-abc',
      actorType:      'user',
      action:         'purchase_credited',
      targetWalletId: 'user-abc',
      correlationId:  'corr-3',
      details:        {
        transactionId: 'tx-1',
        purchaseToken: 'sandbox_secret', // should be redacted
        coinsReceived: 10,
      },
    });

    expect(mockAppendInTx).toHaveBeenCalledWith(
      TX,
      'user-abc',
      'user',
      'purchase_credited',
      'user-abc',
      'corr-3',
      {
        transactionId: 'tx-1',
        purchaseToken: '<redacted>',
        coinsReceived: 10,
      },
    );
  });

  it('returns void (transactional batch — caller commits)', () => {
    const result = AuditLogService.recordInTx(TX, {
      actorId: 'a', actorType: 'user', action: 'spend_coins',
      targetWalletId: 'w', correlationId: 'c',
    });
    expect(result).toBeUndefined();
  });
});

// ── recordRefundSkeleton ──────────────────────────────────────────────────────

describe('AuditLogService.recordRefundSkeleton', () => {
  beforeEach(() => jest.clearAllMocks());

  it('emits refund_skeleton audit with not-implemented note', async () => {
    await AuditLogService.recordRefundSkeleton({
      actorId:        'admin-1',
      actorType:      'admin',
      targetWalletId: 'wallet-X',
      correlationId:  'corr-refund',
      purchaseId:     'purchase-99',
      reason:         'user requested',
    });

    expect(mockAppend).toHaveBeenCalledWith(
      'admin-1',
      'admin',
      'refund_skeleton',
      'wallet-X',
      'corr-refund',
      {
        purchaseId: 'purchase-99',
        reason:     'user requested',
        note:       'refund_not_implemented_in_sandbox_phase',
      },
    );
  });

  it('handles missing purchaseId as null', async () => {
    await AuditLogService.recordRefundSkeleton({
      actorId: 'admin-1', actorType: 'admin',
      targetWalletId: 'wallet-X', correlationId: 'c',
      reason: 'investigation',
    });
    const [, , , , , details] = mockAppend.mock.calls[0];
    expect((details as Record<string, unknown>)['purchaseId']).toBeNull();
  });
});

// ── AuditAction enum coverage ─────────────────────────────────────────────────

describe('AuditAction enum coverage', () => {
  beforeEach(() => jest.clearAllMocks());

  // Iterate every action — ensures the union is exhaustive in tests
  const allActions: AuditAction[] = [
    'spend_coins',
    'purchase_initiated',
    'purchase_credited',
    'refund_skeleton',
    'wallet_auto_created',
    'wallet_auto_frozen_fraud',
    'fraud_review_flagged',
    'admin_wallet_frozen',
    'admin_wallet_unfrozen',
    'admin_adjustment_requested',
    'admin_adjustment_approved',
    'admin_adjustment_rejected',
    'admin_adjustment_executed',
  ];

  it.each(allActions)('records audit for action %s', async (action) => {
    await AuditLogService.record({
      actorId: 'x', actorType: 'system', action,
      targetWalletId: 'w', correlationId: 'c', details: {},
    });
    const [, , recordedAction] = mockAppend.mock.calls[0];
    expect(recordedAction).toBe(action);
  });
});

// ── Sensitivity guarantee ─────────────────────────────────────────────────────

describe('sanitization safety net', () => {
  beforeEach(() => jest.clearAllMocks());

  it('does NOT persist raw tokens regardless of nesting depth', async () => {
    await AuditLogService.record({
      actorId: 'u', actorType: 'user', action: 'purchase_initiated',
      targetWalletId: 'u', correlationId: 'c',
      details: {
        provider: 'sandbox',
        nested: {
          deeper: {
            sandboxToken: 'sandbox_VERY_SECRET',
            apiKey:       'AIzaSecret',
          },
        },
      },
    });

    const calls = mockAppend.mock.calls[0];
    const stored = calls[5] as Record<string, unknown>;
    const flat = JSON.stringify(stored);

    expect(flat).not.toContain('sandbox_VERY_SECRET');
    expect(flat).not.toContain('AIzaSecret');
    expect(flat).toContain('<redacted>');
  });
});
