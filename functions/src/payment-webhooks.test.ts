/**
 * payment-webhooks.test.ts
 * Tests for webhookGooglePlay, webhookAppStore, webhookStripe HTTP handlers.
 *
 * Note: onRequest functions are called directly (not via testEnv.wrap, which is for callables).
 *
 * Covers:
 *  - Fail closed: no secret configured → 403
 *  - Fail closed: secret configured but sandbox disabled → 403 (stub)
 *  - Method guard: non-POST → 405
 *  - Sandbox mode: valid event → 201 + stored
 *  - Sandbox mode: duplicate event → 200 (deduplicated)
 *  - Sandbox mode: missing required fields → 400
 *  - Sandbox mode: sanitized payload (no raw signatures stored)
 */

// ── Mock firebase-admin ───────────────────────────────────────────────────────

const mockDocGet    = jest.fn();
const mockDocSet    = jest.fn();
const mockDocRefFn  = jest.fn();
const mockCollFn    = jest.fn();

jest.mock('firebase-admin', () => ({
  initializeApp: jest.fn(),
  firestore:     jest.fn(() => ({
    collection: (col: string) => mockCollFn(col),
  })),
}));

// ── Mock walletLogger ─────────────────────────────────────────────────────────

jest.mock('./lib/walletLogger', () => ({
  walletLog: {
    info:  jest.fn(),
    warn:  jest.fn(),
    error: jest.fn(),
  },
}));

// Import AFTER mocks are set up
import { webhookGooglePlay, webhookAppStore, webhookStripe } from './webhooks';

// Type alias for calling onRequest functions as plain HTTP handlers
type HttpHandler = (req: unknown, res: unknown) => Promise<void>;

const callGooglePlay = webhookGooglePlay as unknown as HttpHandler;
const callAppStore   = webhookAppStore   as unknown as HttpHandler;
const callStripe     = webhookStripe     as unknown as HttpHandler;

// ── Request / Response helpers ────────────────────────────────────────────────

function makeReq(body: Record<string, unknown> = {}, method = 'POST') {
  const headers: Record<string, string> = {};
  if (process.env['SANDBOX_WEBHOOK_SECRET']) {
    headers['x-mindos-sandbox-webhook-secret'] = process.env['SANDBOX_WEBHOOK_SECRET'];
  }
  return { method, body, headers };
}

function makeRes() {
  const statusMock = jest.fn();
  const jsonMock   = jest.fn();
  const sendMock   = jest.fn();

  const res = {
    statusMock,
    jsonMock,
    sendMock,
    status(code: number) { statusMock(code); return res; },
    json(b: unknown)     { jsonMock(b);      return res; },
    send(b: unknown)     { sendMock(b);      return res; },
  };
  return res;
}

// ── Helpers ───────────────────────────────────────────────────────────────────

const VALID_BODY = {
  providerEventId: 'evt-001',
  eventType:       'subscription_purchased',
};

function clearEnvVars() {
  delete process.env['GOOGLE_PLAY_WEBHOOK_SECRET'];
  delete process.env['APPLE_WEBHOOK_SECRET'];
  delete process.env['STRIPE_WEBHOOK_SECRET'];
  delete process.env['SANDBOX_PAYMENTS_ENABLED'];
  delete process.env['SANDBOX_WEBHOOK_SECRET'];
  delete process.env['GCLOUD_PROJECT'];
  delete process.env['FIREBASE_CONFIG'];
}

function enableSandbox() {
  process.env['SANDBOX_PAYMENTS_ENABLED'] = 'true';
  process.env['SANDBOX_WEBHOOK_SECRET'] = 'sandbox-secret';
}

// ── Global reset ──────────────────────────────────────────────────────────────

beforeEach(() => {
  jest.clearAllMocks();
  clearEnvVars();

  // Default: event doc does not yet exist
  mockDocGet.mockResolvedValue({ exists: false });
  mockDocSet.mockResolvedValue(undefined);
  mockDocRefFn.mockReturnValue({ get: mockDocGet, set: mockDocSet });
  mockCollFn.mockReturnValue({ doc: mockDocRefFn });
});

// ═════════════════════════════════════════════════════════════════════════════
// Fail-closed: not configured (sandbox disabled + no secret)
// ═════════════════════════════════════════════════════════════════════════════

describe('webhookGooglePlay — fail closed: not configured', () => {
  it('should return 403 when sandbox disabled and secret not set', async () => {
    const res = makeRes();
    await callGooglePlay(makeReq(VALID_BODY), res);
    expect(res.statusMock).toHaveBeenCalledWith(403);
    expect(mockDocSet).not.toHaveBeenCalled();
  });
});

describe('webhookAppStore — fail closed: not configured', () => {
  it('should return 403 when sandbox disabled and secret not set', async () => {
    const res = makeRes();
    await callAppStore(makeReq(VALID_BODY), res);
    expect(res.statusMock).toHaveBeenCalledWith(403);
    expect(mockDocSet).not.toHaveBeenCalled();
  });
});

describe('webhookStripe — fail closed: not configured', () => {
  it('should return 403 when sandbox disabled and secret not set', async () => {
    const res = makeRes();
    await callStripe(makeReq(VALID_BODY), res);
    expect(res.statusMock).toHaveBeenCalledWith(403);
    expect(mockDocSet).not.toHaveBeenCalled();
  });
});

// ═════════════════════════════════════════════════════════════════════════════
// Fail-closed: secret configured but sandbox disabled (stub always rejects)
// ═════════════════════════════════════════════════════════════════════════════

describe('webhookGooglePlay — fail closed: secret set, sandbox off', () => {
  beforeEach(() => { process.env['GOOGLE_PLAY_WEBHOOK_SECRET'] = 'some-secret'; });

  it('should return 403 even when secret env var is set (stub)', async () => {
    const res = makeRes();
    await callGooglePlay(makeReq(VALID_BODY), res);
    expect(res.statusMock).toHaveBeenCalledWith(403);
    expect(mockDocSet).not.toHaveBeenCalled();
  });
});

describe('webhookAppStore — fail closed: secret set, sandbox off', () => {
  beforeEach(() => { process.env['APPLE_WEBHOOK_SECRET'] = 'some-secret'; });

  it('should return 403 even when secret env var is set (stub)', async () => {
    const res = makeRes();
    await callAppStore(makeReq(VALID_BODY), res);
    expect(res.statusMock).toHaveBeenCalledWith(403);
    expect(mockDocSet).not.toHaveBeenCalled();
  });
});

describe('webhookStripe — fail closed: secret set, sandbox off', () => {
  beforeEach(() => { process.env['STRIPE_WEBHOOK_SECRET'] = 'whsec_fake'; });

  it('should return 403 even when secret env var is set (stub)', async () => {
    const res = makeRes();
    await callStripe(makeReq(VALID_BODY), res);
    expect(res.statusMock).toHaveBeenCalledWith(403);
    expect(mockDocSet).not.toHaveBeenCalled();
  });
});

// ═════════════════════════════════════════════════════════════════════════════
describe('webhook sandbox guardrails', () => {
  it('rejects sandbox mode when sandbox secret is not configured', async () => {
    process.env['SANDBOX_PAYMENTS_ENABLED'] = 'true';
    const res = makeRes();
    await callGooglePlay(makeReq(VALID_BODY), res);
    expect(res.statusMock).toHaveBeenCalledWith(403);
    expect(mockDocSet).not.toHaveBeenCalled();
  });

  it('rejects sandbox mode in the production Firebase project', async () => {
    enableSandbox();
    process.env['GCLOUD_PROJECT'] = 'mindos-8452f';
    const res = makeRes();
    await callGooglePlay(makeReq(VALID_BODY), res);
    expect(res.statusMock).toHaveBeenCalledWith(403);
    expect(mockDocSet).not.toHaveBeenCalled();
  });
});

// Method guard
// ═════════════════════════════════════════════════════════════════════════════

describe('webhook method guard', () => {
  beforeEach(() => { enableSandbox(); });

  it('webhookGooglePlay: should return 405 for GET', async () => {
    const res = makeRes();
    await callGooglePlay(makeReq(VALID_BODY, 'GET'), res);
    expect(res.statusMock).toHaveBeenCalledWith(405);
  });

  it('webhookAppStore: should return 405 for PUT', async () => {
    const res = makeRes();
    await callAppStore(makeReq(VALID_BODY, 'PUT'), res);
    expect(res.statusMock).toHaveBeenCalledWith(405);
  });

  it('webhookStripe: should return 405 for DELETE', async () => {
    const res = makeRes();
    await callStripe(makeReq(VALID_BODY, 'DELETE'), res);
    expect(res.statusMock).toHaveBeenCalledWith(405);
  });
});

// ═════════════════════════════════════════════════════════════════════════════
// Sandbox mode — successful event storage
// ═════════════════════════════════════════════════════════════════════════════

describe('webhookGooglePlay — sandbox mode: successful storage', () => {
  beforeEach(() => { enableSandbox(); });

  it('should return 201 for a valid new event', async () => {
    const res = makeRes();
    await callGooglePlay(makeReq(VALID_BODY), res);
    expect(res.statusMock).toHaveBeenCalledWith(201);
  });

  it('should call Firestore set with correct document shape', async () => {
    const res = makeRes();
    await callGooglePlay(makeReq({ providerEventId: 'evt-xyz', eventType: 'refund' }), res);
    expect(mockDocSet).toHaveBeenCalledWith(
      expect.objectContaining({
        eventId:         'google_play_evt-xyz',
        provider:        'google_play',
        providerEventId: 'evt-xyz',
        eventType:       'refund',
        schemaVersion:   1,
      }),
    );
  });

  it('should use compound doc ID: {provider}_{providerEventId}', async () => {
    const res = makeRes();
    await callGooglePlay(makeReq(VALID_BODY), res);
    expect(mockDocRefFn).toHaveBeenCalledWith('google_play_evt-001');
  });

  it('should include purchaseId in doc when provided', async () => {
    const res = makeRes();
    await callGooglePlay(makeReq({ ...VALID_BODY, purchaseId: 'purchase-abc' }), res);
    expect(mockDocSet).toHaveBeenCalledWith(
      expect.objectContaining({ purchaseId: 'purchase-abc' }),
    );
  });

  it('should set purchaseId to null when not provided', async () => {
    const res = makeRes();
    await callGooglePlay(makeReq(VALID_BODY), res);
    expect(mockDocSet).toHaveBeenCalledWith(
      expect.objectContaining({ purchaseId: null }),
    );
  });

  it('should store sandbox:true in sanitizedPayload', async () => {
    const res = makeRes();
    await callGooglePlay(makeReq(VALID_BODY), res);
    const stored = mockDocSet.mock.calls[0][0] as Record<string, unknown>;
    expect((stored['sanitizedPayload'] as Record<string, unknown>)['sandbox']).toBe(true);
  });

  it('should NOT store raw signatures or unknown fields in sanitizedPayload', async () => {
    const res = makeRes();
    await callGooglePlay(makeReq({
      ...VALID_BODY,
      rawSignature:   'secret-hmac-value',
      internalToken:  'raw-token-xyz',
      unknownField:   'should-not-be-stored',
    }), res);
    const stored  = mockDocSet.mock.calls[0][0] as Record<string, unknown>;
    const payload = JSON.stringify(stored['sanitizedPayload']);
    expect(payload).not.toContain('secret-hmac-value');
    expect(payload).not.toContain('raw-token-xyz');
    expect(payload).not.toContain('unknownField');
  });
});

describe('webhookAppStore — sandbox mode: successful storage', () => {
  beforeEach(() => { enableSandbox(); });

  it('should return 201 for a valid new event', async () => {
    const res = makeRes();
    await callAppStore(makeReq(VALID_BODY), res);
    expect(res.statusMock).toHaveBeenCalledWith(201);
  });

  it('should use provider=apple_iap in doc ID', async () => {
    const res = makeRes();
    await callAppStore(makeReq(VALID_BODY), res);
    expect(mockDocRefFn).toHaveBeenCalledWith('apple_iap_evt-001');
  });
});

describe('webhookStripe — sandbox mode: successful storage', () => {
  beforeEach(() => { enableSandbox(); });

  it('should return 201 for a valid new event', async () => {
    const res = makeRes();
    await callStripe(makeReq(VALID_BODY), res);
    expect(res.statusMock).toHaveBeenCalledWith(201);
  });

  it('should use provider=stripe in doc ID', async () => {
    const res = makeRes();
    await callStripe(makeReq(VALID_BODY), res);
    expect(mockDocRefFn).toHaveBeenCalledWith('stripe_evt-001');
  });
});

// ═════════════════════════════════════════════════════════════════════════════
// Sandbox mode — deduplication
// ═════════════════════════════════════════════════════════════════════════════

describe('webhook deduplication — sandbox mode', () => {
  beforeEach(() => {
    enableSandbox();
    // Simulate: event already exists in Firestore
    mockDocGet.mockResolvedValue({ exists: true });
  });

  it('should return 200 (already_processed) for duplicate Google Play event', async () => {
    const res = makeRes();
    await callGooglePlay(makeReq(VALID_BODY), res);
    expect(res.statusMock).toHaveBeenCalledWith(200);
  });

  it('should NOT call Firestore set on duplicate (no double-write)', async () => {
    const res = makeRes();
    await callGooglePlay(makeReq(VALID_BODY), res);
    expect(mockDocSet).not.toHaveBeenCalled();
  });

  it('should return 200 (already_processed) for duplicate App Store event', async () => {
    const res = makeRes();
    await callAppStore(makeReq(VALID_BODY), res);
    expect(res.statusMock).toHaveBeenCalledWith(200);
  });

  it('should return 200 (already_processed) for duplicate Stripe event', async () => {
    const res = makeRes();
    await callStripe(makeReq(VALID_BODY), res);
    expect(res.statusMock).toHaveBeenCalledWith(200);
  });
});

// ═════════════════════════════════════════════════════════════════════════════
// Sandbox mode — body validation
// ═════════════════════════════════════════════════════════════════════════════

describe('webhookGooglePlay — sandbox mode: body validation', () => {
  beforeEach(() => { enableSandbox(); });

  it('should return 400 when providerEventId is missing', async () => {
    const res = makeRes();
    await callGooglePlay(makeReq({ eventType: 'refund' }), res);
    expect(res.statusMock).toHaveBeenCalledWith(400);
    expect(mockDocSet).not.toHaveBeenCalled();
  });

  it('should return 400 when providerEventId is empty string', async () => {
    const res = makeRes();
    await callGooglePlay(makeReq({ providerEventId: '', eventType: 'refund' }), res);
    expect(res.statusMock).toHaveBeenCalledWith(400);
    expect(mockDocSet).not.toHaveBeenCalled();
  });

  it('should return 400 when eventType is missing', async () => {
    const res = makeRes();
    await callGooglePlay(makeReq({ providerEventId: 'evt-001' }), res);
    expect(res.statusMock).toHaveBeenCalledWith(400);
    expect(mockDocSet).not.toHaveBeenCalled();
  });

  it('should return 400 when eventType is empty string', async () => {
    const res = makeRes();
    await callGooglePlay(makeReq({ providerEventId: 'evt-001', eventType: '' }), res);
    expect(res.statusMock).toHaveBeenCalledWith(400);
    expect(mockDocSet).not.toHaveBeenCalled();
  });
});
