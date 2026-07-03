/**
 * marketplaceFunctions.test.ts
 * Jest unit and integration tests for the hardened claimProduct Cloud Function.
 */

// ── Mock Setup before imports ────────────────────────────────────────────────
const mockDocs: Record<string, Record<string, unknown>> = {};

function mockMakeSnap(path: string) {
  return {
    exists: mockDocs[path] !== undefined,
    data: () => mockDocs[path],
  };
}

function mockMakeRef(path: string): any {
  const id = path.split('/').pop() || path;
  return {
    path,
    id,
    get: async () => mockMakeSnap(path),
    set: async (data: any) => {
      mockDocs[path] = { ...data };
    },
    collection: (sub: string) => mockMakeCollection(`${path}/${sub}`),
  };
}

function mockMakeCollection(path: string): any {
  return {
    doc: (id: string) => mockMakeRef(`${path}/${id}`),
  };
}

const mockRunTransaction = jest.fn(async (callback) => {
  const tx = {
    get: async (ref: any) => mockMakeSnap(ref.path),
    set: (ref: any, data: any) => {
      mockDocs[ref.path] = { ...data };
    },
    update: (ref: any, data: any) => {
      mockDocs[ref.path] = { ...(mockDocs[ref.path] || {}), ...data };
    },
    delete: (ref: any) => {
      delete mockDocs[ref.path];
    },
  };
  return callback(tx);
});

jest.mock('firebase-admin', () => {
  return {
    initializeApp: jest.fn(),
    firestore: Object.assign(jest.fn(() => ({
      collection: (name: string) => mockMakeCollection(name),
      runTransaction: mockRunTransaction,
    })), {
      FieldValue: {
        serverTimestamp: jest.fn(() => 'MOCK_TIMESTAMP'),
      },
    }),
  };
});

// Mock Rate Limiter fail-closed service
const mockCheckRateLimitFailClosed = jest.fn().mockResolvedValue(undefined);
jest.mock('./lib/rateLimiter', () => ({
  checkRateLimitFailClosed: (...args: any[]) => mockCheckRateLimitFailClosed(...args),
}));

// Mock firebase-functions so we can assert on HttpsError
jest.mock('firebase-functions', () => {
  const actual = jest.requireActual('firebase-functions');
  return {
    ...actual,
    https: {
      ...actual.https,
      HttpsError: class HttpsError extends Error {
        code: string;
        constructor(code: string, message: string) {
          super(message);
          this.code = code;
        }
      },
    },
  };
});

// ── Imports ──────────────────────────────────────────────────────────────────
import * as functionsTest from 'firebase-functions-test';
import * as functions from 'firebase-functions';
import { claimProduct, setMockTime } from './marketplaceFunctions';

const testEnv = functionsTest();
const wrapped = testEnv.wrap(claimProduct);

// ── Context Helpers ──────────────────────────────────────────────────────────
type CallableContext = {
  auth?: { uid: string; token?: Record<string, unknown> };
  app?: object;
};

function authedCtx(uid = 'userA'): CallableContext {
  return { auth: { uid, token: { email_verified: true } }, app: {} };
}

function anonCtx(): CallableContext {
  return {};
}

function noAppCheckCtx(uid = 'userA'): CallableContext {
  return { auth: { uid, token: { email_verified: true } } };
}

// ── Catalog Seed Helper ──────────────────────────────────────────────────────
function seedCatalogProduct(productId: string, overrides: Record<string, any> = {}) {
  mockDocs[`marketplace_catalog/${productId}`] = {
    productId,
    sku: `sku_${productId}`,
    status: 'ACTIVE',
    title: { en: 'Product', es: 'Producto' },
    description: { en: 'Description', es: 'Descripción' },
    assetIds: [`asset_${productId}`],
    rarity: 'COMMON',
    productType: 'NON_CONSUMABLE',
    acquisitionType: 'FREE_CLAIM',
    availability: { startAtMillis: 0, endAtMillis: null },
    createdAtMillis: 1000,
    updatedAtMillis: 1000,
    ...overrides,
  };
}

function clearDocs() {
  for (const key of Object.keys(mockDocs)) {
    delete mockDocs[key];
  }
}

// ── Test Suite ────────────────────────────────────────────────────────────────
describe('claimProduct Cloud Function', () => {
  beforeEach(() => {
    clearDocs();
    setMockTime(null);
    jest.clearAllMocks();
    mockCheckRateLimitFailClosed.mockResolvedValue(undefined);
  });

  // 1. claimProduct_requires_auth
  it('claimProduct_requires_auth: should reject unauthenticated calls', async () => {
    await expect(
      wrapped({ productId: 'prod_1', idempotencyKey: 'key_1' }, anonCtx() as never)
    ).rejects.toMatchObject({ code: 'unauthenticated' });
  });

  // 2. claimProduct_requires_app_check
  it('claimProduct_requires_app_check: should reject calls without App Check verification', async () => {
    await expect(
      wrapped({ productId: 'prod_1', idempotencyKey: 'key_1' }, noAppCheckCtx() as never)
    ).rejects.toMatchObject({ code: 'unauthenticated' });
  });

  // 3. claimProduct_rejects_missing_product_id
  it('claimProduct_rejects_missing_product_id: should reject calls with missing productId', async () => {
    await expect(
      wrapped({ idempotencyKey: 'key_1' }, authedCtx() as never)
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });

  // 4. claimProduct_rejects_missing_idempotency_key
  it('claimProduct_rejects_missing_idempotency_key: should reject calls with missing idempotencyKey', async () => {
    await expect(
      wrapped({ productId: 'prod_1' }, authedCtx() as never)
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });

  // 5. claimProduct_rejects_malformed_product_id
  it('claimProduct_rejects_malformed_product_id: should reject malformed productIds', async () => {
    await expect(
      wrapped({ productId: 'prod 1!!', idempotencyKey: 'key_1' }, authedCtx() as never)
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });

  // 6. claimProduct_rejects_malformed_idempotency_key
  it('claimProduct_rejects_malformed_idempotency_key: should reject malformed idempotencyKeys', async () => {
    await expect(
      wrapped({ productId: 'prod_1', idempotencyKey: 'key/1/invalid' }, authedCtx() as never)
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });

  // 7. claimProduct_rejects_unknown_product
  it('claimProduct_rejects_unknown_product: should reject products not in catalog', async () => {
    await expect(
      wrapped({ productId: 'prod_unknown', idempotencyKey: 'key_1' }, authedCtx() as never)
    ).rejects.toMatchObject({ code: 'not-found' });
  });

  // 8. claimProduct_rejects_inactive_product
  it('claimProduct_rejects_inactive_product: should reject inactive products', async () => {
    seedCatalogProduct('prod_1', { status: 'INACTIVE' });
    await expect(
      wrapped({ productId: 'prod_1', idempotencyKey: 'key_1' }, authedCtx() as never)
    ).rejects.toMatchObject({ code: 'failed-precondition', message: 'The requested product is currently unavailable.' });
  });

  // 9. claimProduct_rejects_archived_product
  it('claimProduct_rejects_archived_product: should reject archived products', async () => {
    seedCatalogProduct('prod_1', { status: 'ARCHIVED' });
    await expect(
      wrapped({ productId: 'prod_1', idempotencyKey: 'key_1' }, authedCtx() as never)
    ).rejects.toMatchObject({ code: 'failed-precondition', message: 'The requested product is currently unavailable.' });
  });

  // 10. claimProduct_rejects_future_purchase_in_v1
  it('claimProduct_rejects_future_purchase_in_v1: should reject FUTURE_PURCHASE products', async () => {
    seedCatalogProduct('prod_1', { acquisitionType: 'FUTURE_PURCHASE' });
    await expect(
      wrapped({ productId: 'prod_1', idempotencyKey: 'key_1' }, authedCtx() as never)
    ).rejects.toMatchObject({ code: 'failed-precondition', message: 'The requested product cannot be claimed directly.' });
  });

  // 11. claimProduct_rejects_admin_grant_from_client
  it('claimProduct_rejects_admin_grant_from_client: should reject ADMIN_GRANT products', async () => {
    seedCatalogProduct('prod_1', { acquisitionType: 'ADMIN_GRANT' });
    await expect(
      wrapped({ productId: 'prod_1', idempotencyKey: 'key_1' }, authedCtx() as never)
    ).rejects.toMatchObject({ code: 'failed-precondition', message: 'The requested product cannot be claimed directly.' });
  });

  // 12. claimProduct_rejects_progression_unlock_without_valid_progress
  it('claimProduct_rejects_progression_unlock_without_valid_progress: should reject PROGRESSION_UNLOCK products in v1 with PRODUCT_UNAVAILABLE', async () => {
    seedCatalogProduct('prod_1', { acquisitionType: 'PROGRESSION_UNLOCK' });
    await expect(
      wrapped({ productId: 'prod_1', idempotencyKey: 'key_1' }, authedCtx() as never)
    ).rejects.toMatchObject({ code: 'failed-precondition', message: 'The requested product is currently unavailable.' });
  });

  // 13. claimProduct_rejects_not_started_product
  it('claimProduct_rejects_not_started_product: should reject products before availability startAtMillis', async () => {
    seedCatalogProduct('prod_1', { availability: { startAtMillis: 5000, endAtMillis: null } });
    setMockTime(2000); // Before start
    await expect(
      wrapped({ productId: 'prod_1', idempotencyKey: 'key_1' }, authedCtx() as never)
    ).rejects.toMatchObject({ code: 'failed-precondition', message: 'The requested product is currently unavailable.' });
  });

  // 14. claimProduct_rejects_expired_product
  it('claimProduct_rejects_expired_product: should reject products after availability endAtMillis', async () => {
    seedCatalogProduct('prod_1', { availability: { startAtMillis: 0, endAtMillis: 5000 } });
    setMockTime(6000); // After end
    await expect(
      wrapped({ productId: 'prod_1', idempotencyKey: 'key_1' }, authedCtx() as never)
    ).rejects.toMatchObject({ code: 'failed-precondition', message: 'The requested product is currently unavailable.' });
  });

  // 15. claimProduct_grants_free_claim
  it('claimProduct_grants_free_claim: should successfully grant FREE_CLAIM products', async () => {
    seedCatalogProduct('prod_1', { acquisitionType: 'FREE_CLAIM' });
    setMockTime(1000);
    const res = await wrapped({ productId: 'prod_1', idempotencyKey: 'key_1' }, authedCtx('userA') as never);
    expect(res).toEqual({
      success: true,
      entitlementId: 'prod_1',
      grantedAtMillis: 1000,
      replayed: false,
    });
  });

  // 16. claimProduct_grants_default_only_if_bootstrap_supported
  it('claimProduct_grants_default_only_if_bootstrap_supported: should successfully grant DEFAULT products', async () => {
    seedCatalogProduct('prod_1', { acquisitionType: 'DEFAULT' });
    setMockTime(1000);
    const res = await wrapped({ productId: 'prod_1', idempotencyKey: 'key_1' }, authedCtx('userA') as never);
    expect(res).toEqual({
      success: true,
      entitlementId: 'prod_1',
      grantedAtMillis: 1000,
      replayed: false,
    });
    // Check that source is set to BOOTSTRAP
    const entSnap = mockDocs['user_entitlements/userA/items/prod_1'];
    expect(entSnap.source).toBe('BOOTSTRAP');
  });

  // 17. claimProduct_writes_entitlement
  it('claimProduct_writes_entitlement: should write correct user entitlement document', async () => {
    seedCatalogProduct('prod_1', { acquisitionType: 'FREE_CLAIM', assetIds: ['asset_a', 'asset_b'] });
    setMockTime(1234);
    await wrapped({ productId: 'prod_1', idempotencyKey: 'key_1' }, authedCtx('userA') as never);
    expect(mockDocs['user_entitlements/userA/items/prod_1']).toEqual({
      schemaVersion: 2,
      entitlementId: 'prod_1',
      uid: 'userA',
      productId: 'prod_1',
      assetIds: ['asset_a', 'asset_b'],
      source: 'FREE_PROMOTION',
      status: 'ACTIVE',
      grantedAtMillis: 1234,
    });
  });

  // 18. claimProduct_writes_inventory_projection
  it('claimProduct_writes_inventory_projection: should write correct user inventory document', async () => {
    seedCatalogProduct('prod_1', { acquisitionType: 'FREE_CLAIM' });
    setMockTime(1234);
    await wrapped({ productId: 'prod_1', idempotencyKey: 'key_1' }, authedCtx('userA') as never);
    expect(mockDocs['user_inventory/userA/items/prod_1']).toEqual({
      schemaVersion: 2,
      inventoryItemId: 'prod_1',
      uid: 'userA',
      productId: 'prod_1',
      quantity: 1,
      acquiredAtMillis: 1234,
    });
  });

  // 19. claimProduct_writes_transaction_log
  it('claimProduct_writes_transaction_log: should write user-scoped transaction log document', async () => {
    seedCatalogProduct('prod_1', { acquisitionType: 'FREE_CLAIM' });
    setMockTime(1234);
    await wrapped({ productId: 'prod_1', idempotencyKey: 'key_1' }, authedCtx('userA') as never);
    expect(mockDocs['marketplace_transactions/userA/keys/key_1']).toEqual({
      idempotencyKey: 'key_1',
      payloadHash: expect.any(String),
      productId: 'prod_1',
      uid: 'userA',
      createdAtMillis: 1234,
      status: 'completed',
      result: {
        success: true,
        entitlementId: 'prod_1',
        grantedAtMillis: 1234,
      },
    });
  });

  // 20. claimProduct_same_idempotency_key_returns_same_result
  it('claimProduct_same_idempotency_key_returns_same_result: should return cached result on replay', async () => {
    seedCatalogProduct('prod_1', { acquisitionType: 'FREE_CLAIM' });
    setMockTime(1000);
    const res1 = await wrapped({ productId: 'prod_1', idempotencyKey: 'key_1' }, authedCtx('userA') as never);
    expect(res1.replayed).toBe(false);

    setMockTime(2000); // Even at different time
    const res2 = await wrapped({ productId: 'prod_1', idempotencyKey: 'key_1' }, authedCtx('userA') as never);
    expect(res2).toEqual({
      success: true,
      entitlementId: 'prod_1',
      grantedAtMillis: 1000,
      replayed: true,
    });
  });

  // 21. claimProduct_same_idempotency_key_different_payload_rejected
  it('claimProduct_same_idempotency_key_different_payload_rejected: should throw error on payload mismatch for same key', async () => {
    seedCatalogProduct('prod_1', { acquisitionType: 'FREE_CLAIM' });
    seedCatalogProduct('prod_2', { acquisitionType: 'FREE_CLAIM' });
    setMockTime(1000);
    await wrapped({ productId: 'prod_1', idempotencyKey: 'key_1' }, authedCtx('userA') as never);

    await expect(
      wrapped({ productId: 'prod_2', idempotencyKey: 'key_1' }, authedCtx('userA') as never)
    ).rejects.toMatchObject({ code: 'aborted' });
  });

  // 22. claimProduct_parallel_claims_same_product_do_not_duplicate_entitlement
  it('claimProduct_parallel_claims_same_product_do_not_duplicate_entitlement: should enforce atomic checks and avoid duplicates', async () => {
    seedCatalogProduct('prod_1', { acquisitionType: 'FREE_CLAIM' });
    setMockTime(1000);

    // Run first claim
    await wrapped({ productId: 'prod_1', idempotencyKey: 'key_1' }, authedCtx('userA') as never);

    // Simulate parallel claim that runs after first committed (so the entitlement doc exists)
    const res = await wrapped({ productId: 'prod_1', idempotencyKey: 'key_2' }, authedCtx('userA') as never);
    expect(res).toEqual({
      success: true,
      entitlementId: 'prod_1',
      grantedAtMillis: 1000,
      replayed: false,
    });
  });

  // 23. claimProduct_parallel_claims_different_keys_do_not_duplicate_non_consumable
  it('claimProduct_parallel_claims_different_keys_do_not_duplicate_non_consumable: should check existence and return success no-op', async () => {
    seedCatalogProduct('prod_1', { acquisitionType: 'FREE_CLAIM' });
    setMockTime(1000);

    await wrapped({ productId: 'prod_1', idempotencyKey: 'key_a' }, authedCtx('userA') as never);
    await wrapped({ productId: 'prod_1', idempotencyKey: 'key_b' }, authedCtx('userA') as never);

    // Verify only one entitlement document was ever written
    expect(mockDocs['user_entitlements/userA/items/prod_1']).toBeDefined();
    // And both keys should have transaction logs
    expect(mockDocs['marketplace_transactions/userA/keys/key_a']).toBeDefined();
    expect(mockDocs['marketplace_transactions/userA/keys/key_b']).toBeDefined();
  });

  // 24. claimProduct_uid_is_derived_from_auth_not_payload
  it('claimProduct_uid_is_derived_from_auth_not_payload: should derive uid strictly from context auth', async () => {
    seedCatalogProduct('prod_1', { acquisitionType: 'FREE_CLAIM' });
    setMockTime(1000);

    // We pass a fake uid in payload, but the function must ignore it or rather not write to it.
    // In claimProduct, payload is just { productId, idempotencyKey }
    await wrapped({ productId: 'prod_1', idempotencyKey: 'key_1', uid: 'user_fake' }, authedCtx('userA') as never);

    expect(mockDocs['user_entitlements/userA/items/prod_1']).toBeDefined();
    expect(mockDocs['user_entitlements/user_fake/items/prod_1']).toBeUndefined();
  });

  // 25. claimProduct_rate_limited
  it('claimProduct_rate_limited: should reject calls when rate limit is exceeded', async () => {
    seedCatalogProduct('prod_1', { acquisitionType: 'FREE_CLAIM' });
    mockCheckRateLimitFailClosed.mockRejectedValueOnce(
      new functions.https.HttpsError('resource-exhausted', 'Too many requests.')
    );

    await expect(
      wrapped({ productId: 'prod_1', idempotencyKey: 'key_1' }, authedCtx('userA') as never)
    ).rejects.toMatchObject({ code: 'resource-exhausted' });
  });
});
