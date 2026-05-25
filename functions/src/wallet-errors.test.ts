/**
 * wallet-errors.test.ts
 * Tests for: unified error model, correlation ID, and log sanitization.
 */

import { walletError } from './lib/walletErrors';
import { resolveCorrelationId, generateCorrelationId } from './lib/correlationId';

// ── walletError ──────────────────────────────────────────────────────────────

describe('walletError', () => {
  it('should return HttpsError with correct gRPC code for INSUFFICIENT_BALANCE', () => {
    const err = walletError('INSUFFICIENT_BALANCE', 'corr-123');
    expect(err.code).toBe('failed-precondition');
    expect(err.message).toBe('Insufficient balance for this operation.');
    expect(err.details).toMatchObject({
      errorCode: 'INSUFFICIENT_BALANCE',
      correlationId: 'corr-123',
    });
  });

  it('should return HttpsError with correct gRPC code for WALLET_NOT_FOUND', () => {
    const err = walletError('WALLET_NOT_FOUND', 'corr-456');
    expect(err.code).toBe('not-found');
  });

  it('should return HttpsError with correct gRPC code for DUPLICATE_IDEMPOTENCY_KEY', () => {
    const err = walletError('DUPLICATE_IDEMPOTENCY_KEY', 'corr-789');
    expect(err.code).toBe('already-exists');
  });

  it('should return HttpsError with correct gRPC code for RATE_LIMITED', () => {
    const err = walletError('RATE_LIMITED', 'corr-rate');
    expect(err.code).toBe('resource-exhausted');
  });

  it('should return HttpsError with correct gRPC code for PROVIDER_NOT_CONFIGURED', () => {
    const err = walletError('PROVIDER_NOT_CONFIGURED', 'corr-prov');
    expect(err.code).toBe('failed-precondition');
  });

  it('should include timestamp in ISO-8601 format', () => {
    const err = walletError('WALLET_FROZEN', 'corr-ts');
    const details = err.details as { timestamp: string };
    expect(details.timestamp).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/);
  });

  it('should include field when provided', () => {
    const err = walletError('INVALID_WALLET_INPUT', 'corr-field', 'packId');
    const details = err.details as { field: string };
    expect(details.field).toBe('packId');
  });

  it('should not include field when not provided', () => {
    const err = walletError('INVALID_WALLET_INPUT', 'corr-no-field');
    const details = err.details as { field?: string };
    expect(details.field).toBeUndefined();
  });

  it('should never expose internal details in user message', () => {
    const err = walletError('FRAUD_DETECTED', 'corr-fraud');
    expect(err.message).not.toContain('fraud');
    expect(err.message).not.toContain('Fraud');
    expect(err.message).toBe('This operation has been blocked.');
  });

  it('should map all error codes to safe user messages without leaking internals', () => {
    const codes = [
      'INSUFFICIENT_BALANCE', 'WALLET_NOT_FOUND', 'WALLET_FROZEN',
      'WALLET_SUSPENDED', 'DUPLICATE_IDEMPOTENCY_KEY', 'IDEMPOTENCY_KEY_CONFLICT',
      'PURCHASE_ALREADY_CREDITED', 'PURCHASE_NOT_FOUND', 'INVALID_PACK_ID',
      'PROVIDER_NOT_CONFIGURED', 'PROVIDER_VERIFICATION_FAILED',
      'INVALID_WALLET_INPUT', 'RATE_LIMITED', 'FRAUD_DETECTED', 'CONCURRENCY_CONFLICT',
    ] as const;
    for (const code of codes) {
      const err = walletError(code, 'corr-all');
      expect(err.code).toBeTruthy();
      expect(err.message).toBeTruthy();
      expect(err.message.length).toBeGreaterThan(5);
      expect(err.message).not.toContain('stack');
      expect(err.message).not.toContain('Error:');
    }
  });
});

// ── correlationId ────────────────────────────────────────────────────────────

describe('resolveCorrelationId', () => {
  it('should use client-provided correlationId when valid', () => {
    const id = resolveCorrelationId({ correlationId: 'client-req-12345678' });
    expect(id).toBe('client-req-12345678');
  });

  it('should generate a new ID when client provides invalid format', () => {
    const id = resolveCorrelationId({ correlationId: 'ab' }); // too short
    expect(id).not.toBe('ab');
    expect(id.length).toBeGreaterThanOrEqual(8);
  });

  it('should generate a new ID when no correlationId in data', () => {
    const id = resolveCorrelationId({ someField: 'value' });
    expect(id.length).toBeGreaterThanOrEqual(8);
  });

  it('should generate a new ID when data is null', () => {
    const id = resolveCorrelationId(null);
    expect(id.length).toBeGreaterThanOrEqual(8);
  });

  it('should generate a new ID when data is undefined', () => {
    const id = resolveCorrelationId(undefined);
    expect(id.length).toBeGreaterThanOrEqual(8);
  });

  it('should reject correlationId with special characters', () => {
    const id = resolveCorrelationId({ correlationId: 'id-with-spaces here' });
    expect(id).not.toBe('id-with-spaces here');
  });
});

describe('generateCorrelationId', () => {
  it('should generate unique IDs', () => {
    const ids = new Set(Array.from({ length: 100 }, () => generateCorrelationId()));
    expect(ids.size).toBe(100);
  });

  it('should generate UUID-format IDs', () => {
    const id = generateCorrelationId();
    expect(id).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/);
  });
});

// ── Log sanitization ─────────────────────────────────────────────────────────

// We test sanitization by importing the internal sanitization logic indirectly
// through walletLog. Since walletLog calls functions.logger which we can mock,
// we verify the sanitized output.

jest.mock('firebase-functions', () => ({
  logger: {
    info: jest.fn(),
    warn: jest.fn(),
    error: jest.fn(),
  },
  https: {
    HttpsError: class HttpsError extends Error {
      code: string;
      details: unknown;
      constructor(code: string, message: string, details?: unknown) {
        super(message);
        this.code = code;
        this.details = details;
      }
    },
  },
}));

import { walletLog } from './lib/walletLogger';
import * as functions from 'firebase-functions';

describe('walletLog sanitization', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should include correlationId and action in every log', () => {
    walletLog.info({ correlationId: 'corr-1', action: 'spendCoins' }, 'test');
    expect(functions.logger.info).toHaveBeenCalledWith('test', expect.objectContaining({
      correlationId: 'corr-1',
      action: 'spendCoins',
    }));
  });

  it('should sanitize purchaseToken to first 8 chars + ***', () => {
    walletLog.info(
      { correlationId: 'corr-2', action: 'verifyPurchase' },
      'verifying',
      { purchaseToken: 'abcdefghijklmnopqrstuvwxyz' },
    );
    const loggedData = (functions.logger.info as jest.Mock).mock.calls[0][1];
    expect(loggedData.purchaseToken).toBe('abcdefgh***');
  });

  it('should sanitize token fields', () => {
    walletLog.info(
      { correlationId: 'corr-3', action: 'test' },
      'test',
      { accessToken: 'secret-token-value-12345' },
    );
    const loggedData = (functions.logger.info as jest.Mock).mock.calls[0][1];
    expect(loggedData.accessToken).toBe('secret-t***');
  });

  it('should sanitize short token-like values completely', () => {
    walletLog.info(
      { correlationId: 'corr-4', action: 'test' },
      'test',
      { apiKey: 'short' },
    );
    const loggedData = (functions.logger.info as jest.Mock).mock.calls[0][1];
    expect(loggedData.apiKey).toBe('***');
  });

  it('should sanitize nested sensitive fields', () => {
    walletLog.info(
      { correlationId: 'corr-5', action: 'test' },
      'test',
      { provider: { providerToken: 'long-token-value-here' } },
    );
    const loggedData = (functions.logger.info as jest.Mock).mock.calls[0][1];
    expect((loggedData.provider as Record<string, unknown>).providerToken).toBe('long-tok***');
  });

  it('should not sanitize non-sensitive fields', () => {
    walletLog.info(
      { correlationId: 'corr-6', action: 'test' },
      'test',
      { packId: 'coins_10', amount: 10 },
    );
    const loggedData = (functions.logger.info as jest.Mock).mock.calls[0][1];
    expect(loggedData.packId).toBe('coins_10');
    expect(loggedData.amount).toBe(10);
  });

  it('should call warn for warnings', () => {
    walletLog.warn({ correlationId: 'corr-7', action: 'test' }, 'warning msg');
    expect(functions.logger.warn).toHaveBeenCalledWith('warning msg', expect.objectContaining({
      correlationId: 'corr-7',
    }));
  });

  it('should call error for errors', () => {
    walletLog.error({ correlationId: 'corr-8', action: 'test' }, 'error msg');
    expect(functions.logger.error).toHaveBeenCalledWith('error msg', expect.objectContaining({
      correlationId: 'corr-8',
    }));
  });
});
