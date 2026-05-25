/**
 * payment-providers.test.ts
 * Tests for payment provider abstraction — fail-closed stubs for real providers.
 */

// ── Mock firebase-admin ───────────────────────────────────────────────────────

jest.mock('firebase-admin', () => ({
  initializeApp: jest.fn(),
  firestore: jest.fn(() => ({})),
}));

// ── Mock sandboxProvider to prevent it loading in these tests ─────────────────

jest.mock('./lib/sandboxProvider', () => ({
  SandboxPaymentProvider: class {
    providerName = 'sandbox';
    async verify() { return { isValid: true, coinsToCredit: 0, providerTransactionId: null, sanitizedResponse: {} }; }
    async refundStatus() { return { isRefunded: false, providerRefundId: null }; }
  },
}));

import { getPaymentProvider } from './lib/paymentProviders';

const VERIFY_PARAMS = {
  purchaseId: 'p-001',
  walletId: 'user-1',
  packId: 'coins_10',
  providerPurchaseToken: 'raw-token-abc',
  correlationId: 'corr-1',
};

const REFUND_PARAMS = {
  providerTransactionId: 'tx-provider-1',
  correlationId: 'corr-1',
};

// ── Helpers ───────────────────────────────────────────────────────────────────

function clearProviderEnvVars() {
  delete process.env['GOOGLE_PLAY_SERVICE_ACCOUNT_JSON'];
  delete process.env['APPLE_IAP_SHARED_SECRET'];
  delete process.env['STRIPE_SECRET_KEY'];
}

// ── Google Play — fail closed ─────────────────────────────────────────────────

describe('GooglePlayPaymentProvider', () => {
  beforeEach(clearProviderEnvVars);

  it('should throw PROVIDER_NOT_CONFIGURED when env var is absent', async () => {
    const provider = getPaymentProvider('google_play', 'corr-1');
    await expect(provider.verify(VERIFY_PARAMS)).rejects.toMatchObject({ code: 'failed-precondition' });
  });

  it('should throw PROVIDER_NOT_CONFIGURED even when env var is set (stub)', async () => {
    process.env['GOOGLE_PLAY_SERVICE_ACCOUNT_JSON'] = '{"type":"service_account"}';
    const provider = getPaymentProvider('google_play', 'corr-1');
    await expect(provider.verify(VERIFY_PARAMS)).rejects.toMatchObject({ code: 'failed-precondition' });
  });

  it('should throw PROVIDER_NOT_CONFIGURED for refundStatus', async () => {
    const provider = getPaymentProvider('google_play', 'corr-1');
    await expect(provider.refundStatus(REFUND_PARAMS)).rejects.toMatchObject({ code: 'failed-precondition' });
  });

  it('should have correct providerName', () => {
    const provider = getPaymentProvider('google_play', 'corr-1');
    expect(provider.providerName).toBe('google_play');
  });
});

// ── Apple IAP — fail closed ───────────────────────────────────────────────────

describe('ApplePaymentProvider', () => {
  beforeEach(clearProviderEnvVars);

  it('should throw PROVIDER_NOT_CONFIGURED when env var is absent', async () => {
    const provider = getPaymentProvider('apple_iap', 'corr-1');
    await expect(provider.verify(VERIFY_PARAMS)).rejects.toMatchObject({ code: 'failed-precondition' });
  });

  it('should throw PROVIDER_NOT_CONFIGURED even when env var is set (stub)', async () => {
    process.env['APPLE_IAP_SHARED_SECRET'] = 'fake-secret';
    const provider = getPaymentProvider('apple_iap', 'corr-1');
    await expect(provider.verify(VERIFY_PARAMS)).rejects.toMatchObject({ code: 'failed-precondition' });
  });

  it('should throw PROVIDER_NOT_CONFIGURED for refundStatus', async () => {
    const provider = getPaymentProvider('apple_iap', 'corr-1');
    await expect(provider.refundStatus(REFUND_PARAMS)).rejects.toMatchObject({ code: 'failed-precondition' });
  });

  it('should have correct providerName', () => {
    const provider = getPaymentProvider('apple_iap', 'corr-1');
    expect(provider.providerName).toBe('apple_iap');
  });
});

// ── Stripe — fail closed ──────────────────────────────────────────────────────

describe('StripePaymentProvider', () => {
  beforeEach(clearProviderEnvVars);

  it('should throw PROVIDER_NOT_CONFIGURED when env var is absent', async () => {
    const provider = getPaymentProvider('stripe', 'corr-1');
    await expect(provider.verify(VERIFY_PARAMS)).rejects.toMatchObject({ code: 'failed-precondition' });
  });

  it('should throw PROVIDER_NOT_CONFIGURED even when env var is set (stub)', async () => {
    process.env['STRIPE_SECRET_KEY'] = 'sk_test_fake';
    const provider = getPaymentProvider('stripe', 'corr-1');
    await expect(provider.verify(VERIFY_PARAMS)).rejects.toMatchObject({ code: 'failed-precondition' });
  });

  it('should throw PROVIDER_NOT_CONFIGURED for refundStatus', async () => {
    const provider = getPaymentProvider('stripe', 'corr-1');
    await expect(provider.refundStatus(REFUND_PARAMS)).rejects.toMatchObject({ code: 'failed-precondition' });
  });

  it('should have correct providerName', () => {
    const provider = getPaymentProvider('stripe', 'corr-1');
    expect(provider.providerName).toBe('stripe');
  });
});

// ── Factory — unknown provider ────────────────────────────────────────────────

describe('getPaymentProvider factory', () => {
  it('should return sandbox provider for "sandbox"', () => {
    const provider = getPaymentProvider('sandbox', 'corr-1');
    expect(provider.providerName).toBe('sandbox');
  });

  it('should return google_play provider for "google_play"', () => {
    const provider = getPaymentProvider('google_play', 'corr-1');
    expect(provider.providerName).toBe('google_play');
  });

  it('should return apple_iap provider for "apple_iap"', () => {
    const provider = getPaymentProvider('apple_iap', 'corr-1');
    expect(provider.providerName).toBe('apple_iap');
  });

  it('should return stripe provider for "stripe"', () => {
    const provider = getPaymentProvider('stripe', 'corr-1');
    expect(provider.providerName).toBe('stripe');
  });
});
