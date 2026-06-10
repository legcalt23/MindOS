/**
 * payment-sandbox.test.ts
 * Tests for SandboxPaymentProvider: feature flag, valid pack, invalid pack, refundStatus.
 */

// ── Mock firebase-admin ───────────────────────────────────────────────────────

jest.mock('firebase-admin', () => ({
  initializeApp: jest.fn(),
  firestore: jest.fn(() => ({})),
}));

import { SandboxPaymentProvider } from './lib/sandboxProvider';
import type { VerifyPurchaseParams } from './lib/paymentProviders';

// ── Helpers ───────────────────────────────────────────────────────────────────

const makeParams = (overrides: Partial<VerifyPurchaseParams> = {}): VerifyPurchaseParams => ({
  purchaseId:            'p-001',
  walletId:              'user-1',
  packId:                'coins_10',
  providerPurchaseToken: 'sandbox_abc123',
  correlationId:         'corr-1',
  ...overrides,
});

let provider: SandboxPaymentProvider;

beforeEach(() => {
  provider = new SandboxPaymentProvider();
  delete process.env['SANDBOX_PAYMENTS_ENABLED'];
  delete process.env['GCLOUD_PROJECT'];
  delete process.env['FIREBASE_CONFIG'];
});

// ── Feature flag ──────────────────────────────────────────────────────────────

describe('SandboxPaymentProvider — feature flag', () => {
  it('should throw PROVIDER_NOT_CONFIGURED when SANDBOX_PAYMENTS_ENABLED is not set', async () => {
    await expect(provider.verify(makeParams())).rejects.toMatchObject({ code: 'failed-precondition' });
  });

  it('should throw PROVIDER_NOT_CONFIGURED when SANDBOX_PAYMENTS_ENABLED is "false"', async () => {
    process.env['SANDBOX_PAYMENTS_ENABLED'] = 'false';
    await expect(provider.verify(makeParams())).rejects.toMatchObject({ code: 'failed-precondition' });
  });

  it('should proceed when SANDBOX_PAYMENTS_ENABLED is "true"', async () => {
    process.env['SANDBOX_PAYMENTS_ENABLED'] = 'true';
    const result = await provider.verify(makeParams());
    expect(result.isValid).toBe(true);
  });

  it('should reject sandbox mode in the production Firebase project', async () => {
    process.env['SANDBOX_PAYMENTS_ENABLED'] = 'true';
    process.env['GCLOUD_PROJECT'] = 'mindos-8452f';

    await expect(provider.verify(makeParams())).rejects.toMatchObject({ code: 'failed-precondition' });
  });
});

// ── Valid pack ────────────────────────────────────────────────────────────────

describe('SandboxPaymentProvider — valid pack', () => {
  beforeEach(() => { process.env['SANDBOX_PAYMENTS_ENABLED'] = 'true'; });

  it('should return isValid: true for coins_1', async () => {
    const result = await provider.verify(makeParams({ packId: 'coins_1' }));
    expect(result.isValid).toBe(true);
    expect(result.coinsToCredit).toBe(1);
  });

  it('should return isValid: true for coins_10', async () => {
    const result = await provider.verify(makeParams({ packId: 'coins_10' }));
    expect(result.isValid).toBe(true);
    expect(result.coinsToCredit).toBe(10);
  });

  it('should return isValid: true for coins_100', async () => {
    const result = await provider.verify(makeParams({ packId: 'coins_100' }));
    expect(result.isValid).toBe(true);
    expect(result.coinsToCredit).toBe(100);
  });

  it('should derive coinsToCredit from server-side PACK_CATALOG (not from client)', async () => {
    // Even if the client somehow passed a different amount, the server uses the catalog
    const result = await provider.verify(makeParams({ packId: 'coins_100' }));
    expect(result.coinsToCredit).toBe(100); // from catalog, not from any client field
  });

  it('should use providerPurchaseToken prefix as providerTransactionId', async () => {
    const result = await provider.verify(makeParams({ providerPurchaseToken: 'sandbox_test-token-uuid-1234' }));
    expect(result.providerTransactionId).toBe('sandbox_test-token-uuid-1234'.slice(0, 36));
  });

  it('should include sandbox: true in sanitizedResponse', async () => {
    const result = await provider.verify(makeParams());
    expect(result.sanitizedResponse['sandbox']).toBe(true);
  });

  it('should NOT include the raw token in sanitizedResponse', async () => {
    const result = await provider.verify(makeParams({ providerPurchaseToken: 'my-secret-sandbox-token' }));
    expect(JSON.stringify(result.sanitizedResponse)).not.toContain('my-secret-sandbox-token');
  });
});

// ── Invalid pack ──────────────────────────────────────────────────────────────

describe('SandboxPaymentProvider — invalid pack', () => {
  beforeEach(() => { process.env['SANDBOX_PAYMENTS_ENABLED'] = 'true'; });

  it('should throw INVALID_PACK_ID for unknown pack', async () => {
    await expect(
      provider.verify(makeParams({ packId: 'coins_9999' })),
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });

  it('should throw INVALID_PACK_ID for empty string', async () => {
    await expect(
      provider.verify(makeParams({ packId: '' })),
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });
});

// ── refundStatus ──────────────────────────────────────────────────────────────

describe('SandboxPaymentProvider — refundStatus', () => {
  it('should always return isRefunded: false (sandbox has no external refund)', async () => {
    const result = await provider.refundStatus({ providerTransactionId: 'tx-1', correlationId: 'corr-1' });
    expect(result.isRefunded).toBe(false);
    expect(result.providerRefundId).toBeNull();
  });
});

// ── providerName ──────────────────────────────────────────────────────────────

describe('SandboxPaymentProvider — metadata', () => {
  it('should have providerName "sandbox"', () => {
    expect(provider.providerName).toBe('sandbox');
  });
});
