/**
 * wallet-validators.test.ts
 * Tests for strict wallet input validation:
 *  - Valid inputs pass
 *  - Invalid inputs rejected
 *  - Forbidden client fields rejected
 *  - Unknown fields rejected
 *  - Control characters rejected
 *  - Pack catalog enforced
 *  - Idempotency key format enforced
 */

jest.mock('firebase-functions', () => ({
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

import {
  validateGetBalanceInput,
  validateGetTransactionsInput,
  validateSpendCoinsInput,
  validateInitiatePurchaseInput,
  validateVerifyPurchaseInput,
  PACK_CATALOG,
} from './lib/walletValidators';

// ── PACK_CATALOG ─────────────────────────────────────────────────────────────

describe('PACK_CATALOG', () => {
  it('should define coins_1 with 1 coin, 0 price, SANDBOX currency', () => {
    expect(PACK_CATALOG['coins_1']).toEqual({
      packId: 'coins_1', coins: 1, priceCents: 0, currency: 'SANDBOX',
    });
  });

  it('should define coins_10 with 10 coins', () => {
    expect(PACK_CATALOG['coins_10'].coins).toBe(10);
  });

  it('should define coins_100 with 100 coins', () => {
    expect(PACK_CATALOG['coins_100'].coins).toBe(100);
  });

  it('should only have 3 packs', () => {
    expect(Object.keys(PACK_CATALOG)).toHaveLength(3);
  });

  it('should have all packs at 0 price (sandbox)', () => {
    for (const pack of Object.values(PACK_CATALOG)) {
      expect(pack.priceCents).toBe(0);
      expect(pack.currency).toBe('SANDBOX');
    }
  });
});

// ── validateGetBalanceInput ──────────────────────────────────────────────────

describe('validateGetBalanceInput', () => {
  it('should accept null data', () => {
    expect(validateGetBalanceInput(null)).toEqual({});
  });

  it('should accept undefined data', () => {
    expect(validateGetBalanceInput(undefined)).toEqual({});
  });

  it('should accept empty object', () => {
    expect(validateGetBalanceInput({})).toEqual({});
  });

  it('should accept correlationId', () => {
    const result = validateGetBalanceInput({ correlationId: 'corr-123' });
    expect(result.correlationId).toBe('corr-123');
  });

  it('should reject amount field', () => {
    expect(() => validateGetBalanceInput({ amount: 100 })).toThrow('not accepted');
  });

  it('should reject newBalance field', () => {
    expect(() => validateGetBalanceInput({ newBalance: 999 })).toThrow('not accepted');
  });

  it('should reject unknown fields', () => {
    expect(() => validateGetBalanceInput({ hackField: 'value' })).toThrow('Unknown field');
  });
});

// ── validateGetTransactionsInput ─────────────────────────────────────────────

describe('validateGetTransactionsInput', () => {
  it('should accept null data', () => {
    expect(validateGetTransactionsInput(null)).toEqual({});
  });

  it('should accept valid limit and cursor', () => {
    const result = validateGetTransactionsInput({ limit: 20, cursor: 'abc-def-123' });
    expect(result.limit).toBe(20);
    expect(result.cursor).toBe('abc-def-123');
  });

  it('should reject limit > 100', () => {
    expect(() => validateGetTransactionsInput({ limit: 101 })).toThrow('limit');
  });

  it('should reject limit < 1', () => {
    expect(() => validateGetTransactionsInput({ limit: 0 })).toThrow('limit');
  });

  it('should reject non-integer limit', () => {
    expect(() => validateGetTransactionsInput({ limit: 10.5 })).toThrow('limit');
  });

  it('should reject cursor with invalid characters', () => {
    expect(() => validateGetTransactionsInput({ cursor: 'invalid cursor!' })).toThrow('cursor');
  });

  it('should reject forbidden fields', () => {
    expect(() => validateGetTransactionsInput({ balance: 100 })).toThrow('not accepted');
  });
});

// ── validateSpendCoinsInput ──────────────────────────────────────────────────

describe('validateSpendCoinsInput', () => {
  const valid = {
    itemId: 'feature_unlock_premium',
    quantity: 1,
    idempotencyKey: 'a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d',
  };

  it('should accept valid spend input', () => {
    const result = validateSpendCoinsInput(valid);
    expect(result.itemId).toBe('feature_unlock_premium');
    expect(result.quantity).toBe(1);
    expect(result.idempotencyKey).toBe('a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d');
  });

  it('should reject null data', () => {
    expect(() => validateSpendCoinsInput(null)).toThrow('non-null object');
  });

  it('should reject missing itemId', () => {
    expect(() => validateSpendCoinsInput({ ...valid, itemId: undefined })).toThrow('itemId');
  });

  it('should reject empty itemId', () => {
    expect(() => validateSpendCoinsInput({ ...valid, itemId: '' })).toThrow('itemId');
  });

  it('should reject itemId with control characters', () => {
    expect(() => validateSpendCoinsInput({ ...valid, itemId: 'item\x00id' })).toThrow('invalid');
  });

  it('should reject missing quantity', () => {
    expect(() => validateSpendCoinsInput({ ...valid, quantity: undefined })).toThrow('quantity');
  });

  it('should reject quantity = 0', () => {
    expect(() => validateSpendCoinsInput({ ...valid, quantity: 0 })).toThrow('quantity');
  });

  it('should reject negative quantity', () => {
    expect(() => validateSpendCoinsInput({ ...valid, quantity: -5 })).toThrow('quantity');
  });

  it('should reject quantity > 1000', () => {
    expect(() => validateSpendCoinsInput({ ...valid, quantity: 1001 })).toThrow('quantity');
  });

  it('should reject non-integer quantity', () => {
    expect(() => validateSpendCoinsInput({ ...valid, quantity: 1.5 })).toThrow('quantity');
  });

  it('should reject missing idempotencyKey', () => {
    expect(() => validateSpendCoinsInput({ ...valid, idempotencyKey: undefined })).toThrow('idempotencyKey');
  });

  it('should reject short idempotencyKey (< 8 chars)', () => {
    expect(() => validateSpendCoinsInput({ ...valid, idempotencyKey: 'short' })).toThrow('idempotencyKey');
  });

  it('should reject idempotencyKey > 128 chars', () => {
    expect(() => validateSpendCoinsInput({
      ...valid,
      idempotencyKey: 'a'.repeat(129),
    })).toThrow('idempotencyKey');
  });

  it('should accept idempotencyKey as safe ID', () => {
    const result = validateSpendCoinsInput({ ...valid, idempotencyKey: 'my-safe-key-12345678' });
    expect(result.idempotencyKey).toBe('my-safe-key-12345678');
  });

  it('should reject amount field (forbidden)', () => {
    expect(() => validateSpendCoinsInput({ ...valid, amount: 50 })).toThrow('not accepted');
  });

  it('should reject price field (forbidden)', () => {
    expect(() => validateSpendCoinsInput({ ...valid, price: 99 })).toThrow('not accepted');
  });

  it('should reject newBalance field (forbidden)', () => {
    expect(() => validateSpendCoinsInput({ ...valid, newBalance: 999 })).toThrow('not accepted');
  });

  it('should reject userId field (forbidden)', () => {
    expect(() => validateSpendCoinsInput({ ...valid, userId: 'hacker' })).toThrow('not accepted');
  });

  it('should reject walletId field (forbidden)', () => {
    expect(() => validateSpendCoinsInput({ ...valid, walletId: 'fake' })).toThrow('not accepted');
  });

  it('should reject unknown fields', () => {
    expect(() => validateSpendCoinsInput({ ...valid, extraField: 'hack' })).toThrow('Unknown field');
  });
});

// ── validateInitiatePurchaseInput ────────────────────────────────────────────

describe('validateInitiatePurchaseInput', () => {
  const valid = {
    packId: 'coins_10',
    idempotencyKey: 'a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d',
  };

  it('should accept valid purchase input', () => {
    const result = validateInitiatePurchaseInput(valid);
    expect(result.packId).toBe('coins_10');
    expect(result.idempotencyKey).toBe('a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d');
  });

  it('should accept coins_1', () => {
    expect(validateInitiatePurchaseInput({ ...valid, packId: 'coins_1' }).packId).toBe('coins_1');
  });

  it('should accept coins_100', () => {
    expect(validateInitiatePurchaseInput({ ...valid, packId: 'coins_100' }).packId).toBe('coins_100');
  });

  it('should reject invalid packId', () => {
    expect(() => validateInitiatePurchaseInput({ ...valid, packId: 'coins_999' })).toThrow('packId');
  });

  it('should reject empty packId', () => {
    expect(() => validateInitiatePurchaseInput({ ...valid, packId: '' })).toThrow('packId');
  });

  it('should reject numeric packId', () => {
    expect(() => validateInitiatePurchaseInput({ ...valid, packId: 10 })).toThrow('packId');
  });

  it('should reject amount field (client cannot set price)', () => {
    expect(() => validateInitiatePurchaseInput({ ...valid, amount: 100 })).toThrow('not accepted');
  });

  it('should reject coins field (client cannot set coins)', () => {
    expect(() => validateInitiatePurchaseInput({ ...valid, coins: 999 })).toThrow('not accepted');
  });

  it('should reject coinsAmount field', () => {
    expect(() => validateInitiatePurchaseInput({ ...valid, coinsAmount: 999 })).toThrow('not accepted');
  });

  it('should reject priceCents field', () => {
    expect(() => validateInitiatePurchaseInput({ ...valid, priceCents: 100 })).toThrow('not accepted');
  });

  it('should reject unknown fields', () => {
    expect(() => validateInitiatePurchaseInput({ ...valid, provider: 'google' })).toThrow('Unknown field');
  });

  it('should reject missing idempotencyKey', () => {
    expect(() => validateInitiatePurchaseInput({ packId: 'coins_10' })).toThrow('idempotencyKey');
  });
});

// ── validateVerifyPurchaseInput ──────────────────────────────────────────────

describe('validateVerifyPurchaseInput', () => {
  const valid = {
    sandboxPurchaseId: 'purchase-abc-def-123456',
    idempotencyKey: 'a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d',
  };

  it('should accept valid verify input', () => {
    const result = validateVerifyPurchaseInput(valid);
    expect(result.sandboxPurchaseId).toBe('purchase-abc-def-123456');
  });

  it('should reject missing sandboxPurchaseId', () => {
    expect(() => validateVerifyPurchaseInput({ idempotencyKey: valid.idempotencyKey })).toThrow('sandboxPurchaseId');
  });

  it('should reject sandboxPurchaseId with special chars', () => {
    expect(() => validateVerifyPurchaseInput({
      ...valid,
      sandboxPurchaseId: 'id with spaces!',
    })).toThrow('sandboxPurchaseId');
  });

  it('should reject status field (forbidden)', () => {
    expect(() => validateVerifyPurchaseInput({ ...valid, status: 'verified' })).toThrow('not accepted');
  });

  it('should reject unknown fields', () => {
    expect(() => validateVerifyPurchaseInput({ ...valid, hackField: 'x' })).toThrow('Unknown field');
  });
});

// ── Cross-cutting forbidden field tests ──────────────────────────────────────

describe('forbidden fields across all economic validators', () => {
  const forbiddenFields = [
    'amount', 'newBalance', 'balance', 'price', 'priceCents',
    'coinsAmount', 'coins', 'walletId', 'userId', 'ledgerTransactionId', 'status',
  ];

  const validSpend = {
    itemId: 'test-item-123',
    quantity: 1,
    idempotencyKey: 'a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d',
  };

  for (const field of forbiddenFields) {
    it(`should reject "${field}" on spendCoins`, () => {
      expect(() => validateSpendCoinsInput({ ...validSpend, [field]: 'hack' }))
        .toThrow(/not accepted|Unknown field/);
    });
  }
});
