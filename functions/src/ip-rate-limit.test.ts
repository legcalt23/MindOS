/**
 * ip-rate-limit.test.ts
 * Per-IP rate limiting for unauthenticated / low-trust surfaces (P1.1).
 * Covers clientIp() extraction and checkRateLimitByKey() allow/deny.
 */

const mockRunTransaction = jest.fn();

jest.mock('firebase-admin', () => {
  const firestore = () => ({
    collection: () => ({
      doc: (id: string) => ({ path: `rate_limits/${id}` }),
    }),
    runTransaction: mockRunTransaction,
  });
  // TTL field: buckets store expireAt as a Timestamp (S12a).
  (firestore as unknown as Record<string, unknown>)['Timestamp'] = {
    fromMillis: (ms: number) => ({ __timestampMs: ms }),
  };
  return { firestore };
});

jest.mock('firebase-functions', () => ({
  logger: { warn: jest.fn(), error: jest.fn() },
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

import { clientIp, checkRateLimitByKey, IP_RATE_LIMITS } from './lib/rateLimiter';
import * as functions from 'firebase-functions';

/** Drives mockRunTransaction with a fake stored bucket snapshot. */
function withBucket(snapData: Record<string, unknown> | null) {
  mockRunTransaction.mockImplementation(async (cb: (tx: unknown) => Promise<unknown>) =>
    cb({
      get: async () => ({ exists: snapData != null, data: () => snapData }),
      set: jest.fn(),
    }),
  );
}

beforeEach(() => jest.clearAllMocks());

describe('clientIp', () => {
  it('uses the first X-Forwarded-For entry', () => {
    expect(clientIp({ headers: { 'x-forwarded-for': '203.0.113.7, 10.0.0.1' } })).toBe('203.0.113.7');
  });

  it('handles array-valued X-Forwarded-For', () => {
    expect(clientIp({ headers: { 'x-forwarded-for': ['198.51.100.4', '10.0.0.1'] } })).toBe('198.51.100.4');
  });

  it('falls back to req.ip then socket.remoteAddress', () => {
    expect(clientIp({ headers: {}, ip: '192.0.2.9' })).toBe('192.0.2.9');
    expect(clientIp({ headers: {}, socket: { remoteAddress: '192.0.2.55' } })).toBe('192.0.2.55');
  });

  it('returns "unknown" when nothing is available', () => {
    expect(clientIp({ headers: {} })).toBe('unknown');
    expect(clientIp(undefined)).toBe('unknown');
  });
});

describe('checkRateLimitByKey', () => {
  const cfg = IP_RATE_LIMITS['webhook']; // { maxCalls: 60, windowMs: 60_000 }

  it('allows the first request in a fresh bucket', async () => {
    withBucket(null);
    await expect(checkRateLimitByKey('ip:203.0.113.7:webhook', cfg)).resolves.toBeUndefined();
  });

  it('allows requests under the limit within the window', async () => {
    withBucket({ count: cfg.maxCalls - 1, windowStart: Date.now() });
    await expect(checkRateLimitByKey('ip:203.0.113.7:webhook', cfg)).resolves.toBeUndefined();
  });

  it('throws resource-exhausted when the limit is exceeded in-window', async () => {
    withBucket({ count: cfg.maxCalls, windowStart: Date.now() });
    await expect(checkRateLimitByKey('ip:203.0.113.7:webhook', cfg)).rejects.toMatchObject({
      code: 'resource-exhausted',
    });
  });

  it('resets to 1 when the previous window has elapsed', async () => {
    withBucket({ count: cfg.maxCalls + 100, windowStart: Date.now() - cfg.windowMs - 1 });
    await expect(checkRateLimitByKey('ip:203.0.113.7:webhook', cfg)).resolves.toBeUndefined();
  });

  it('writes a Timestamp expireAt on the bucket for the TTL policy (S12a)', async () => {
    const set = jest.fn();
    mockRunTransaction.mockImplementation(async (cb: (tx: unknown) => Promise<unknown>) =>
      cb({ get: async () => ({ exists: false, data: () => null }), set }),
    );
    await checkRateLimitByKey('ip:203.0.113.7:webhook', cfg);
    expect(set).toHaveBeenCalledTimes(1);
    const written = set.mock.calls[0][1] as Record<string, unknown>;
    // Timestamp stub shape from the firebase-admin mock: { __timestampMs }.
    expect(written['expireAt']).toMatchObject({ __timestampMs: expect.any(Number) });
    expect((written['expireAt'] as { __timestampMs: number }).__timestampMs)
      .toBe((written['windowStart'] as number) + cfg.windowMs * 2);
  });

  it('uses an HttpsError instance for the exceeded case', async () => {
    withBucket({ count: cfg.maxCalls, windowStart: Date.now() });
    await expect(checkRateLimitByKey('ip:1.2.3.4:webhook', cfg)).rejects.toBeInstanceOf(
      functions.https.HttpsError,
    );
  });
});
