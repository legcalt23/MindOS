/**
 * wallet-schema.test.ts
 * Tests for schema constants, type guards, and collection name correctness.
 */

import {
  COLLECTIONS,
  SCHEMA_VERSION,
  isValidWalletStatus,
  isValidLedgerTransactionType,
  isValidBucket,
  isValidPurchaseProvider,
  isValidPurchaseStatus,
} from './lib/walletSchema';

describe('COLLECTIONS', () => {
  it('should define all required wallet collections', () => {
    expect(COLLECTIONS.WALLETS).toBe('wallets');
    expect(COLLECTIONS.BALANCE_SNAPSHOTS).toBe('balance_snapshots');
    expect(COLLECTIONS.LEDGER_TRANSACTIONS).toBe('ledger_transactions');
    expect(COLLECTIONS.LEDGER_ENTRIES).toBe('ledger_entries');
    expect(COLLECTIONS.IDEMPOTENCY_KEYS).toBe('idempotency_keys');
    expect(COLLECTIONS.PURCHASES).toBe('purchases');
    expect(COLLECTIONS.PURCHASE_TOKEN_INDEX).toBe('purchase_token_index');
    expect(COLLECTIONS.PAYMENT_PROVIDER_EVENTS).toBe('payment_provider_events');
    expect(COLLECTIONS.FRAUD_SIGNALS).toBe('fraud_signals');
    expect(COLLECTIONS.WALLET_AUDIT_LOGS).toBe('wallet_audit_logs');
    expect(COLLECTIONS.ADMIN_ACTIONS).toBe('admin_actions');
  });

  it('should have exactly 11 wallet collections', () => {
    expect(Object.keys(COLLECTIONS)).toHaveLength(11);
  });
});

describe('SCHEMA_VERSION', () => {
  it('should be 1', () => {
    expect(SCHEMA_VERSION).toBe(1);
  });
});

describe('isValidWalletStatus', () => {
  it('should accept active', () => expect(isValidWalletStatus('active')).toBe(true));
  it('should accept frozen', () => expect(isValidWalletStatus('frozen')).toBe(true));
  it('should accept suspended', () => expect(isValidWalletStatus('suspended')).toBe(true));
  it('should reject unknown', () => expect(isValidWalletStatus('deleted')).toBe(false));
  it('should reject null', () => expect(isValidWalletStatus(null)).toBe(false));
  it('should reject number', () => expect(isValidWalletStatus(1)).toBe(false));
});

describe('isValidLedgerTransactionType', () => {
  const valid = ['purchase_credit', 'spend', 'refund', 'admin_adjustment', 'promotional_credit'];
  for (const t of valid) {
    it(`should accept ${t}`, () => expect(isValidLedgerTransactionType(t)).toBe(true));
  }
  it('should reject unknown type', () => expect(isValidLedgerTransactionType('mint')).toBe(false));
  it('should reject null', () => expect(isValidLedgerTransactionType(null)).toBe(false));
});

describe('isValidBucket', () => {
  it('should accept purchased', () => expect(isValidBucket('purchased')).toBe(true));
  it('should accept promotional', () => expect(isValidBucket('promotional')).toBe(true));
  it('should accept earned', () => expect(isValidBucket('earned')).toBe(true));
  it('should reject unknown', () => expect(isValidBucket('bonus')).toBe(false));
});

describe('isValidPurchaseProvider', () => {
  const valid = ['sandbox', 'google_play', 'apple_iap', 'stripe'];
  for (const p of valid) {
    it(`should accept ${p}`, () => expect(isValidPurchaseProvider(p)).toBe(true));
  }
  it('should reject unknown', () => expect(isValidPurchaseProvider('paypal')).toBe(false));
});

describe('isValidPurchaseStatus', () => {
  const valid = ['initiated', 'verified', 'credited', 'failed', 'refunded'];
  for (const s of valid) {
    it(`should accept ${s}`, () => expect(isValidPurchaseStatus(s)).toBe(true));
  }
  it('should reject unknown', () => expect(isValidPurchaseStatus('pending')).toBe(false));
});
