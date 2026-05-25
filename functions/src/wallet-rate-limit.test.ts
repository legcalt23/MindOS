/**
 * wallet-rate-limit.test.ts
 * Tests for wallet-specific rate limits and fail-closed behavior.
 */

// ── Mock setup ───────────────────────────────────────────────────────────────

const mockRunTransaction = jest.fn();

jest.mock('firebase-admin', () => ({
  firestore: () => ({
    collection: () => ({
      doc: (id: string) => ({
        path: `rate_limits/${id}`,
      }),
    }),
    runTransaction: mockRunTransaction,
  }),
}));

jest.mock('firebase-functions', () => ({
  logger: {
    warn: jest.fn(),
    error: jest.fn(),
  },
  https: {
    HttpsError: class HttpsError extends Error {
      code: string;
      constructor(code: string, message: string) {
        super(message);
        this.code = code;
      }
    },
  },
}));

import { RATE_LIMITS, checkRateLimitFailClosed } from './lib/rateLimiter';
import * as functions from 'firebase-functions';

// ── RATE_LIMITS config ───────────────────────────────────────────────────────

describe('RATE_LIMITS wallet config', () => {
  it('should have getBalance limit', () => {
    expect(RATE_LIMITS['getBalance']).toEqual({ maxCalls: 30, windowMs: 60_000 });
  });

  it('should have spendCoins limit', () => {
    expect(RATE_LIMITS['spendCoins']).toEqual({ maxCalls: 20, windowMs: 60_000 });
  });

  it('should have initiatePurchase limit', () => {
    expect(RATE_LIMITS['initiatePurchase']).toEqual({ maxCalls: 10, windowMs: 60_000 });
  });

  it('should have verifyPurchase limit', () => {
    expect(RATE_LIMITS['verifyPurchase']).toEqual({ maxCalls: 10, windowMs: 60_000 });
  });

  it('should not have removed existing social limits', () => {
    expect(RATE_LIMITS['follow']).toBeDefined();
    expect(RATE_LIMITS['block']).toBeDefined();
    expect(RATE_LIMITS['createPost']).toBeDefined();
  });
});

// ── checkRateLimitFailClosed ─────────────────────────────────────────────────

describe('checkRateLimitFailClosed', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should pass through when rate limit check succeeds', async () => {
    mockRunTransaction.mockResolvedValue(false);
    await expect(checkRateLimitFailClosed('user1', 'getBalance')).resolves.toBeUndefined();
  });

  it('should throw resource-exhausted when rate limit exceeded', async () => {
    mockRunTransaction.mockImplementation(async (fn: (tx: unknown) => Promise<boolean>) => {
      const tx = {
        get: jest.fn().mockResolvedValue({
          exists: true,
          data: () => ({ count: 999, windowStart: Date.now() }),
        }),
        set: jest.fn(),
      };
      return fn(tx);
    });
    await expect(checkRateLimitFailClosed('user1', 'getBalance'))
      .rejects.toMatchObject({ code: 'resource-exhausted' });
  });

  it('should deny economic action when infrastructure fails', async () => {
    mockRunTransaction.mockRejectedValue(new Error('Firestore unavailable'));
    await expect(checkRateLimitFailClosed('user1', 'spendCoins'))
      .rejects.toMatchObject({ code: 'unavailable' });
    expect(functions.logger.error).toHaveBeenCalled();
  });

  it('should deny initiatePurchase when infrastructure fails', async () => {
    mockRunTransaction.mockRejectedValue(new Error('Firestore unavailable'));
    await expect(checkRateLimitFailClosed('user1', 'initiatePurchase'))
      .rejects.toMatchObject({ code: 'unavailable' });
  });

  it('should deny verifyPurchase when infrastructure fails', async () => {
    mockRunTransaction.mockRejectedValue(new Error('Firestore unavailable'));
    await expect(checkRateLimitFailClosed('user1', 'verifyPurchase'))
      .rejects.toMatchObject({ code: 'unavailable' });
  });

  it('should allow non-economic action when infrastructure fails', async () => {
    mockRunTransaction.mockRejectedValue(new Error('Firestore unavailable'));
    await expect(checkRateLimitFailClosed('user1', 'getBalance')).resolves.toBeUndefined();
    expect(functions.logger.warn).toHaveBeenCalled();
  });

  it('should re-throw HttpsError from rate limiter directly', async () => {
    const httpErr = new (functions.https.HttpsError as unknown as new (code: string, msg: string) => Error)(
      'resource-exhausted',
      'Too many requests',
    );
    mockRunTransaction.mockRejectedValue(httpErr);
    await expect(checkRateLimitFailClosed('user1', 'spendCoins'))
      .rejects.toMatchObject({ code: 'resource-exhausted' });
  });
});
