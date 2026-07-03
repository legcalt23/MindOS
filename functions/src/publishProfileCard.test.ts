/**
 * publishProfileCard.test.ts
 * Jest unit and integration tests for the `/publishProfileCard` Cloud Function.
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
      doc: (id: string) => mockMakeRef(id),
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
import { publishProfileCard, setMockTime } from './marketplaceFunctions';

const testEnv = functionsTest();
const wrapped = testEnv.wrap(publishProfileCard);

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

// ── Seeding Helpers ──────────────────────────────────────────────────────────
function seedPrivateDraft(uid: string, payload: string) {
  mockDocs[`profile_cards_private/${uid}`] = {
    schemaVersion: 2,
    payload,
    updatedAt: 1000,
  };
}

function seedCatalogProduct(productId: string, assetIds: string[] = [], overrides: Record<string, any> = {}) {
  mockDocs[`marketplace_catalog/${productId}`] = {
    productId,
    sku: `sku_${productId}`,
    status: 'ACTIVE',
    assetIds,
    rarity: 'COMMON',
    productType: 'BACKGROUND',
    acquisitionType: 'FREE_CLAIM',
    createdAtMillis: 1000,
    updatedAtMillis: 1000,
    ...overrides,
  };
}

function seedAsset(assetId: string, compatibleSlots: string[] = [], overrides: Record<string, any> = {}) {
  mockDocs[`marketplace_assets/${assetId}`] = {
    assetId,
    format: 'WEBP_STATIC',
    status: 'ACTIVE',
    storagePath: `marketplace/assets/${assetId}/original.webp`,
    hashSha256: `hash_${assetId}`,
    sizeBytes: 1024,
    compatibleSlots,
    createdAtMillis: 1000,
    ...overrides,
  };
}

function seedEntitlement(uid: string, productId: string, overrides: Record<string, any> = {}) {
  mockDocs[`user_entitlements/${uid}/items/${productId}`] = {
    schemaVersion: 2,
    entitlementId: productId,
    uid,
    productId,
    assetIds: [`asset_${productId}`],
    source: 'FREE_PROMOTION',
    status: 'ACTIVE',
    grantedAtMillis: 1000,
    ...overrides,
  };
}

function clearDocs() {
  for (const key of Object.keys(mockDocs)) {
    delete mockDocs[key];
  }
}

// ── Test Suite ────────────────────────────────────────────────────────────────
describe('publishProfileCard Cloud Function', () => {
  beforeEach(() => {
    clearDocs();
    setMockTime(null);
    jest.clearAllMocks();
    mockCheckRateLimitFailClosed.mockResolvedValue(undefined);
  });

  // 1. publishProfileCard_requires_auth
  it('publishProfileCard_requires_auth: should reject unauthenticated calls', async () => {
    await expect(
      wrapped({}, anonCtx() as never)
    ).rejects.toMatchObject({ code: 'unauthenticated' });
  });

  // 2. publishProfileCard_requires_app_check
  it('publishProfileCard_requires_app_check: should reject calls without App Check', async () => {
    await expect(
      wrapped({}, noAppCheckCtx() as never)
    ).rejects.toMatchObject({ code: 'unauthenticated' });
  });

  // 3. publishProfileCard_rate_limited
  it('publishProfileCard_rate_limited: should reject calls when rate limited', async () => {
    mockCheckRateLimitFailClosed.mockRejectedValueOnce(
      new functions.https.HttpsError('resource-exhausted', 'Too many requests.')
    );
    await expect(
      wrapped({}, authedCtx() as never)
    ).rejects.toMatchObject({ code: 'resource-exhausted' });
  });

  // 4. publishProfileCard_rejects_missing_private_draft
  it('publishProfileCard_rejects_missing_private_draft: should fail if user has no private card draft', async () => {
    await expect(
      wrapped({}, authedCtx('userA') as never)
    ).rejects.toMatchObject({ code: 'not-found', message: 'Private profile card draft not found.' });
  });

  // 5. publishProfileCard_rejects_malformed_json_payload
  it('publishProfileCard_rejects_malformed_json_payload: should reject malformed JSON payload string', async () => {
    seedPrivateDraft('userA', '{invalid json}');
    await expect(
      wrapped({}, authedCtx('userA') as never)
    ).rejects.toMatchObject({ code: 'invalid-argument', message: 'Private profile card payload is not valid JSON.' });
  });

  // 6. publishProfileCard_rejects_oversized_payload
  it('publishProfileCard_rejects_oversized_payload: should reject payload string larger than 16,000 bytes UTF-8', async () => {
    const hugePayload = JSON.stringify({
      slots: {},
      stickers: [],
      padding: 'x'.repeat(16000)
    });
    seedPrivateDraft('userA', hugePayload);
    await expect(
      wrapped({}, authedCtx('userA') as never)
    ).rejects.toMatchObject({ code: 'invalid-argument', message: 'The profile payload exceeds the maximum permitted size.' });
  });

  // 7. publishProfileCard_rejects_excessive_stickers
  it('publishProfileCard_rejects_excessive_stickers: should reject drafts with more than 8 stickers', async () => {
    const badPayload = JSON.stringify({
      slots: {},
      stickers: Array(9).fill({ productId: 'prod_st', assetId: 'asset_st', x: 0, y: 0 })
    });
    seedPrivateDraft('userA', badPayload);
    await expect(
      wrapped({}, authedCtx('userA') as never)
    ).rejects.toMatchObject({ code: 'invalid-argument', message: 'The profile canvas exceeds the maximum sticker count.' });
  });

  // 8. publishProfileCard_rejects_unowned_product
  it('publishProfileCard_rejects_unowned_product: should reject if user does not own entitlement for equipped item', async () => {
    seedCatalogProduct('prod_bg', ['asset_bg']);
    seedAsset('asset_bg', ['profile.background']);
    // Entitlement is missing

    const payload = JSON.stringify({
      slots: { 'profile.background': { productId: 'prod_bg', assetId: 'asset_bg' } },
      stickers: []
    });
    seedPrivateDraft('userA', payload);

    await expect(
      wrapped({}, authedCtx('userA') as never)
    ).rejects.toMatchObject({ code: 'failed-precondition', message: 'The user does not own this entitlement.' });
  });

  // 9. publishProfileCard_rejects_expired_entitlement
  it('publishProfileCard_rejects_expired_entitlement: should reject if entitlement has expired', async () => {
    seedCatalogProduct('prod_bg', ['asset_bg']);
    seedAsset('asset_bg', ['profile.background']);
    seedEntitlement('userA', 'prod_bg', { expiresAtMillis: 2000 });
    setMockTime(3000); // Past expiration

    const payload = JSON.stringify({
      slots: { 'profile.background': { productId: 'prod_bg', assetId: 'asset_bg' } },
      stickers: []
    });
    seedPrivateDraft('userA', payload);

    await expect(
      wrapped({}, authedCtx('userA') as never)
    ).rejects.toMatchObject({ code: 'failed-precondition', message: 'The entitlement has expired.' });
  });

  // 10. publishProfileCard_rejects_revoked_entitlement
  it('publishProfileCard_rejects_revoked_entitlement: should reject if entitlement status is not ACTIVE', async () => {
    seedCatalogProduct('prod_bg', ['asset_bg']);
    seedAsset('asset_bg', ['profile.background']);
    seedEntitlement('userA', 'prod_bg', { status: 'REVOKED' });

    const payload = JSON.stringify({
      slots: { 'profile.background': { productId: 'prod_bg', assetId: 'asset_bg' } },
      stickers: []
    });
    seedPrivateDraft('userA', payload);

    await expect(
      wrapped({}, authedCtx('userA') as never)
    ).rejects.toMatchObject({ code: 'failed-precondition', message: 'The entitlement has been revoked.' });
  });

  // 11. publishProfileCard_rejects_inactive_product
  it('publishProfileCard_rejects_inactive_product: should reject if catalog product status is not ACTIVE', async () => {
    seedCatalogProduct('prod_bg', ['asset_bg'], { status: 'INACTIVE' });
    seedAsset('asset_bg', ['profile.background']);
    seedEntitlement('userA', 'prod_bg');

    const payload = JSON.stringify({
      slots: { 'profile.background': { productId: 'prod_bg', assetId: 'asset_bg' } },
      stickers: []
    });
    seedPrivateDraft('userA', payload);

    await expect(
      wrapped({}, authedCtx('userA') as never)
    ).rejects.toMatchObject({ code: 'failed-precondition', message: 'The requested product is currently unavailable.' });
  });

  // 12. publishProfileCard_rejects_inactive_asset
  it('publishProfileCard_rejects_inactive_asset: should reject if asset status is not ACTIVE', async () => {
    seedCatalogProduct('prod_bg', ['asset_bg']);
    seedAsset('asset_bg', ['profile.background'], { status: 'INACTIVE' });
    seedEntitlement('userA', 'prod_bg');

    const payload = JSON.stringify({
      slots: { 'profile.background': { productId: 'prod_bg', assetId: 'asset_bg' } },
      stickers: []
    });
    seedPrivateDraft('userA', payload);

    await expect(
      wrapped({}, authedCtx('userA') as never)
    ).rejects.toMatchObject({ code: 'failed-precondition', message: 'The requested asset is suspended.' });
  });

  // 13. publishProfileCard_rejects_soft_suspended_asset
  it('publishProfileCard_rejects_soft_suspended_asset: should reject soft suspended assets', async () => {
    seedCatalogProduct('prod_bg', ['asset_bg']);
    seedAsset('asset_bg', ['profile.background'], { status: 'SUSPENDED_SOFT' });
    seedEntitlement('userA', 'prod_bg');

    const payload = JSON.stringify({
      slots: { 'profile.background': { productId: 'prod_bg', assetId: 'asset_bg' } },
      stickers: []
    });
    seedPrivateDraft('userA', payload);

    await expect(
      wrapped({}, authedCtx('userA') as never)
    ).rejects.toMatchObject({ code: 'failed-precondition', message: 'The requested asset is suspended.' });
  });

  // 13. publishProfileCard_rejects_hard_suspended_asset
  it('publishProfileCard_rejects_hard_suspended_asset: should reject hard suspended assets', async () => {
    seedCatalogProduct('prod_bg', ['asset_bg']);
    seedAsset('asset_bg', ['profile.background'], { status: 'SUSPENDED_HARD' });
    seedEntitlement('userA', 'prod_bg');

    const payload = JSON.stringify({
      slots: { 'profile.background': { productId: 'prod_bg', assetId: 'asset_bg' } },
      stickers: []
    });
    seedPrivateDraft('userA', payload);

    await expect(
      wrapped({}, authedCtx('userA') as never)
    ).rejects.toMatchObject({ code: 'failed-precondition', message: 'The requested asset is suspended.' });
  });

  // 14. publishProfileCard_rejects_incompatible_slot
  it('publishProfileCard_rejects_incompatible_slot: should reject if asset compatibleSlots doesn\'t match slot equipped', async () => {
    seedCatalogProduct('prod_bg', ['asset_bg']);
    seedAsset('asset_bg', ['profile.frame']); // Compatible with frame, but equipped in background
    seedEntitlement('userA', 'prod_bg');

    const payload = JSON.stringify({
      slots: { 'profile.background': { productId: 'prod_bg', assetId: 'asset_bg' } },
      stickers: []
    });
    seedPrivateDraft('userA', payload);

    await expect(
      wrapped({}, authedCtx('userA') as never)
    ).rejects.toMatchObject({ code: 'failed-precondition', message: 'The asset is not compatible with the designated slot.' });
  });

  // 15. publishProfileCard_accepts_valid_setup
  it('publishProfileCard_accepts_valid_setup: should successfully publish a valid card setup', async () => {
    seedCatalogProduct('prod_bg', ['asset_bg']);
    seedAsset('asset_bg', ['profile.background']);
    seedEntitlement('userA', 'prod_bg');

    seedCatalogProduct('prod_sticker', ['asset_sticker']);
    seedAsset('asset_sticker', ['profile.canvas.sticker'], { type: 'STICKER' });
    seedEntitlement('userA', 'prod_sticker');

    const payload = JSON.stringify({
      slots: { 'profile.background': { productId: 'prod_bg', assetId: 'asset_bg' } },
      stickers: [{ productId: 'prod_sticker', assetId: 'asset_sticker', x: 10, y: 20 }]
    });
    seedPrivateDraft('userA', payload);
    setMockTime(5555);

    const res = await wrapped({}, authedCtx('userA') as never);
    expect(res).toEqual({ success: true });

    // Verify public card doc matches
    expect(mockDocs['profile_cards_public/userA']).toEqual({
      schemaVersion: 2,
      ownerUid: 'userA',
      payload,
      updatedAtMillis: 5555
    });
  });

  // 16. publishProfileCard_uid_strictly_derived_from_auth
  it('publishProfileCard_uid_strictly_derived_from_auth: should write to public card using auth uid, ignoring custom parameters', async () => {
    seedCatalogProduct('prod_bg', ['asset_bg']);
    seedAsset('asset_bg', ['profile.background']);
    seedEntitlement('userA', 'prod_bg');

    const payload = JSON.stringify({
      slots: { 'profile.background': { productId: 'prod_bg', assetId: 'asset_bg' } },
      stickers: []
    });
    seedPrivateDraft('userA', payload);

    await wrapped({ uid: 'user_spoof', ownerUid: 'user_spoof' }, authedCtx('userA') as never);

    expect(mockDocs['profile_cards_public/userA']).toBeDefined();
    expect(mockDocs['profile_cards_public/user_spoof']).toBeUndefined();
  });

  // 17. publishProfileCard_rejects_unknown_root_fields
  it('publishProfileCard_rejects_unknown_root_fields: should reject drafts with unknown root fields', async () => {
    const badPayload = JSON.stringify({
      slots: {},
      stickers: [],
      evilRoot: 'hacker'
    });
    seedPrivateDraft('userA', badPayload);
    await expect(
      wrapped({}, authedCtx('userA') as never)
    ).rejects.toMatchObject({ code: 'invalid-argument', message: 'Unknown root field: "evilRoot".' });
  });

  // 18. publishProfileCard_rejects_unknown_slot_key
  it('publishProfileCard_rejects_unknown_slot_key: should reject drafts with unknown slot IDs', async () => {
    const badPayload = JSON.stringify({
      slots: { 'profile.background': { productId: 'prod_bg', assetId: 'asset_bg' }, 'profile.evilSlot': { productId: 'p', assetId: 'a' } },
      stickers: []
    });
    seedPrivateDraft('userA', badPayload);
    await expect(
      wrapped({}, authedCtx('userA') as never)
    ).rejects.toMatchObject({ code: 'invalid-argument', message: 'Unknown slot key: "profile.evilSlot".' });
  });

  // 19. publishProfileCard_rejects_unknown_sticker_fields
  it('publishProfileCard_rejects_unknown_sticker_fields: should reject drafts containing unknown sticker fields', async () => {
    const badPayload = JSON.stringify({
      slots: {},
      stickers: [{ productId: 'prod_st', assetId: 'asset_st', x: 0, y: 0, evilField: 'hack' }]
    });
    seedPrivateDraft('userA', badPayload);
    await expect(
      wrapped({}, authedCtx('userA') as never)
    ).rejects.toMatchObject({ code: 'invalid-argument', message: 'Unknown sticker field: "evilField".' });
  });

  // 20. publishProfileCard_rejects_non_sticker_asset_in_sticker_canvas
  it('publishProfileCard_rejects_non_sticker_asset_in_sticker_canvas: should reject if asset equipped as sticker is not a STICKER type', async () => {
    seedCatalogProduct('prod_st', ['asset_st']);
    seedAsset('asset_st', ['profile.canvas.sticker'], { type: 'FRAME' }); // Mismatched type
    seedEntitlement('userA', 'prod_st');

    const payload = JSON.stringify({
      slots: {},
      stickers: [{ productId: 'prod_st', assetId: 'asset_st', x: 0, y: 0 }]
    });
    seedPrivateDraft('userA', payload);

    await expect(
      wrapped({}, authedCtx('userA') as never)
    ).rejects.toMatchObject({ code: 'failed-precondition', message: 'The asset is not a sticker.' });
  });

  // 21. publishProfileCard_rejects_asset_not_linked_to_product
  it('publishProfileCard_rejects_asset_not_linked_to_product: should reject if asset is not in catalog product\'s assetIds', async () => {
    seedCatalogProduct('prod_bg', ['asset_bg_legit']); // asset_bg is not linked
    seedAsset('asset_bg', ['profile.background']);
    seedEntitlement('userA', 'prod_bg');

    const payload = JSON.stringify({
      slots: { 'profile.background': { productId: 'prod_bg', assetId: 'asset_bg' } },
      stickers: []
    });
    seedPrivateDraft('userA', payload);

    await expect(
      wrapped({}, authedCtx('userA') as never)
    ).rejects.toMatchObject({ code: 'failed-precondition', message: 'The asset does not belong to the requested product.' });
  });

  // 22. publishProfileCard_does_not_update_public_snapshot_on_validation_failure
  it('publishProfileCard_does_not_update_public_snapshot_on_validation_failure: should not write to public card if transaction aborts due to validation failure', async () => {
    seedCatalogProduct('prod_bg', ['asset_bg']);
    seedAsset('asset_bg', ['profile.background']);
    // No entitlement

    const payload = JSON.stringify({
      slots: { 'profile.background': { productId: 'prod_bg', assetId: 'asset_bg' } },
      stickers: []
    });
    seedPrivateDraft('userA', payload);

    // Make sure we have an existing public snapshot
    mockDocs['profile_cards_public/userA'] = {
      schemaVersion: 2,
      ownerUid: 'userA',
      payload: 'original_snap',
      updatedAtMillis: 100
    };

    await expect(
      wrapped({}, authedCtx('userA') as never)
    ).rejects.toThrow();

    // Verify public card was not updated
    expect(mockDocs['profile_cards_public/userA'].payload).toBe('original_snap');
  });

  // 23. publishProfileCard_reads_payload_from_private_draft_not_request_body
  it('publishProfileCard_reads_payload_from_private_draft_not_request_body: should ignore payload sent in request data and pull exclusively from private collection', async () => {
    seedCatalogProduct('prod_bg', ['asset_bg']);
    seedAsset('asset_bg', ['profile.background']);
    seedEntitlement('userA', 'prod_bg');

    const draftPayload = JSON.stringify({
      slots: { 'profile.background': { productId: 'prod_bg', assetId: 'asset_bg' } },
      stickers: []
    });
    seedPrivateDraft('userA', draftPayload);

    // Request data contains a fake payload (empty slots)
    const fakePayload = JSON.stringify({ slots: {}, stickers: [] });
    await wrapped({ payload: fakePayload, slots: {}, stickers: [] }, authedCtx('userA') as never);

    // Verify it published the DRAFT payload, not the fake request payload
    expect(mockDocs['profile_cards_public/userA'].payload).toBe(draftPayload);
  });
});
