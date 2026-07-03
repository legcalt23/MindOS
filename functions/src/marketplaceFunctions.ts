/**
 * marketplaceFunctions.ts
 * Callable Cloud Functions for the MindOS Marketplace.
 *
 * SECURITY CONTRACT:
 *  1. requireAppCheck  — reject unverified clients
 *  2. requireAuth      — reject unauthenticated callers
 *  3. validate input   — strict schema validation (safe IDs)
 *  4. checkRateLimit   — fail-closed rate limiting
 *  5. checkIdempotency — transaction replay prevention scoped by UID
 *  6. catalog check    — verify product exists, is active, is in availability window
 *  7. entitlement check— verify user doesn't already own non-consumable
 *  8. Admin SDK write  — writes entitlement, inventory projection, and idempotency record atomically
 */

import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import * as crypto from 'crypto';
import { requireAppCheck, requireAuth } from './lib/guards';
import { checkRateLimitFailClosed } from './lib/rateLimiter';

const db = () => admin.firestore();
const SAFE_ID = /^[A-Za-z0-9_-]{3,128}$/;

// ── Time Mock Wrapper ────────────────────────────────────────────────────────
let mockTime: number | null = null;

export function setMockTime(time: number | null) {
  mockTime = time;
}

export function nowMillis(): number {
  return mockTime !== null ? mockTime : Date.now();
}

// ── Payload Hash Helper ──────────────────────────────────────────────────────
function hashPayload(data: unknown): string {
  return crypto.createHash('sha256').update(JSON.stringify(data)).digest('hex');
}

export const claimProduct = functions.https.onCall(async (data, context) => {
  // 1. Auth Guard
  const uid = requireAuth(context);

  // 2. App Check Guard
  requireAppCheck(context);

  // 3. Payload Shape Validation
  if (!data || typeof data !== 'object') {
    throw new functions.https.HttpsError('invalid-argument', 'Payload must be an object');
  }

  const { productId, idempotencyKey } = data as Record<string, unknown>;

  // 4. Product ID Validation
  if (typeof productId !== 'string' || !productId || !SAFE_ID.test(productId)) {
    throw new functions.https.HttpsError('invalid-argument', 'Invalid productId reference');
  }

  // 5. Idempotency Key Validation
  if (typeof idempotencyKey !== 'string' || !idempotencyKey || !SAFE_ID.test(idempotencyKey)) {
    throw new functions.https.HttpsError('invalid-argument', 'Invalid idempotencyKey reference');
  }

  // 6. Rate Limit (Fail-Closed)
  await checkRateLimitFailClosed(uid, 'claimProduct');

  const now = nowMillis();
  const payloadHash = hashPayload({ productId });

  // References
  const txRef = db().collection('marketplace_transactions').doc(uid).collection('keys').doc(idempotencyKey);
  const catalogRef = db().collection('marketplace_catalog').doc(productId);
  const entitlementRef = db().collection('user_entitlements').doc(uid).collection('items').doc(productId);
  const inventoryRef = db().collection('user_inventory').doc(uid).collection('items').doc(productId);

  const result = await db().runTransaction(async (tx) => {
    // 7. Idempotency Lookup (Scoped by User UID)
    const txSnap = await tx.get(txRef);
    if (txSnap.exists) {
      const txData = txSnap.data()!;
      if (txData.uid !== uid || txData.productId !== productId || txData.payloadHash !== payloadHash) {
        throw new functions.https.HttpsError('aborted', 'Idempotency key conflict: same key used with different request.');
      }
      return {
        ...txData.result,
        replayed: true,
      };
    }

    // 8. Catalog Existence
    const catalogSnap = await tx.get(catalogRef);
    if (!catalogSnap.exists) {
      throw new functions.https.HttpsError('not-found', 'The requested product does not exist.');
    }

    const productData = catalogSnap.data()!;

    // 9. Status Active Check
    if (productData.status !== 'ACTIVE') {
      throw new functions.https.HttpsError('failed-precondition', 'The requested product is currently unavailable.');
    }

    // 10. Availability by Backend Time
    if (productData.availability) {
      const start = productData.availability.startAtMillis;
      const end = productData.availability.endAtMillis;
      if (typeof start === 'number' && now < start) {
        throw new functions.https.HttpsError('failed-precondition', 'The requested product is currently unavailable.');
      }
      if (typeof end === 'number' && end !== null && now > end) {
        throw new functions.https.HttpsError('failed-precondition', 'The requested product is currently unavailable.');
      }
    }

    // 11. Acquisition Type validations
    const acqType = productData.acquisitionType;

    // Reject Future Purchases in v1
    if (acqType === 'FUTURE_PURCHASE') {
      throw new functions.https.HttpsError('failed-precondition', 'The requested product cannot be claimed directly.');
    }

    // Reject Admin Grants from client calls
    if (acqType === 'ADMIN_GRANT') {
      throw new functions.https.HttpsError('failed-precondition', 'The requested product cannot be claimed directly.');
    }

    // Reject Progression Unlocks in v1 (fail closed to PRODUCT_UNAVAILABLE)
    if (acqType === 'PROGRESSION_UNLOCK') {
      throw new functions.https.HttpsError('failed-precondition', 'The requested product is currently unavailable.');
    }

    // Direct claim allowed types: FREE_CLAIM, DEFAULT
    if (acqType !== 'FREE_CLAIM' && acqType !== 'DEFAULT') {
      throw new functions.https.HttpsError('failed-precondition', 'The requested product cannot be claimed directly.');
    }

    // 15. Check if user already has entitlement (non-consumable)
    const entitlementSnap = await tx.get(entitlementRef);
    if (entitlementSnap.exists) {
      const existingData = entitlementSnap.data()!;
      // Write the idempotency transaction record for this new key so that subsequent calls with this key return cached success
      const resultObj = {
        success: true,
        entitlementId: productId,
        grantedAtMillis: existingData.grantedAtMillis,
      };
      tx.set(txRef, {
        idempotencyKey,
        payloadHash,
        productId,
        uid,
        createdAtMillis: now,
        status: 'completed',
        result: resultObj,
      });

      return {
        ...resultObj,
        replayed: false,
      };
    }

    // 16. Write Entitlement
    const assetIds = productData.assetIds || [];
    const source = acqType === 'DEFAULT' ? 'BOOTSTRAP' : 'FREE_PROMOTION';
    tx.set(entitlementRef, {
      schemaVersion: 2,
      entitlementId: productId,
      uid,
      productId,
      assetIds,
      source,
      status: 'ACTIVE',
      grantedAtMillis: now,
    });

    // 17. Write Inventory Projection (Atomic block)
    tx.set(inventoryRef, {
      schemaVersion: 2,
      inventoryItemId: productId,
      uid,
      productId,
      quantity: 1,
      acquiredAtMillis: now,
    });

    // 18. Write Transaction Log
    const resultObj = {
      success: true,
      entitlementId: productId,
      grantedAtMillis: now,
    };

    tx.set(txRef, {
      idempotencyKey,
      payloadHash,
      productId,
      uid,
      createdAtMillis: now,
      status: 'completed',
      result: resultObj,
    });

    return {
      ...resultObj,
      replayed: false,
    };
  });

  return result;
});

// ── publishProfileCard Cloud Function ─────────────────────────────────────────
export const publishProfileCard = functions.https.onCall(async (data, context) => {
  // 1. Auth Guard
  const uid = requireAuth(context);

  // 2. App Check Guard
  requireAppCheck(context);

  // 3. Rate Limit (Fail-Closed)
  await checkRateLimitFailClosed(uid, 'publishProfileCard');

  // 4. Source of Payload: read exclusively from profile_cards_private/{uid}
  // Clients are NOT allowed to pass the payload in the request body.
  const privateCardRef = db().collection('profile_cards_private').doc(uid);
  const privateCardSnap = await privateCardRef.get();

  if (!privateCardSnap.exists) {
    throw new functions.https.HttpsError('not-found', 'Private profile card draft not found.');
  }

  const privateCardData = privateCardSnap.data()!;
  const payloadString = privateCardData.payload;

  if (typeof payloadString !== 'string') {
    throw new functions.https.HttpsError('invalid-argument', 'Private profile card payload is missing or invalid.');
  }

  // 5. Size check: Payload serializado debe pesar máximo 16,000 bytes UTF-8
  const payloadBuffer = Buffer.from(payloadString, 'utf-8');
  if (payloadBuffer.length > 16000) {
    throw new functions.https.HttpsError('invalid-argument', 'The profile payload exceeds the maximum permitted size.');
  }

  // 6. Parse JSON and validate payload shape
  let payloadObj: any;
  try {
    payloadObj = JSON.parse(payloadString);
  } catch (err) {
    throw new functions.https.HttpsError('invalid-argument', 'Private profile card payload is not valid JSON.');
  }

  if (typeof payloadObj !== 'object' || payloadObj === null) {
    throw new functions.https.HttpsError('invalid-argument', 'Private profile card payload is not a JSON object.');
  }

  // Reject unknown keys in the JSON root
  const allowedRootKeys = new Set(['slots', 'stickers']);
  for (const key of Object.keys(payloadObj)) {
    if (!allowedRootKeys.has(key)) {
      throw new functions.https.HttpsError('invalid-argument', `Unknown root field: "${key}".`);
    }
  }

  const slots = payloadObj.slots || {};
  const stickers = payloadObj.stickers || [];

  if (typeof slots !== 'object' || slots === null || Array.isArray(slots)) {
    throw new functions.https.HttpsError('invalid-argument', 'Slots must be a JSON object.');
  }

  if (!Array.isArray(stickers)) {
    throw new functions.https.HttpsError('invalid-argument', 'Stickers must be a JSON array.');
  }

  // Validate allowed slot keys
  const allowedSlotKeys = new Set([
    'profile.background',
    'profile.frame',
    'profile.badge.primary',
    'profile.badge.secondary',
    'profile.avatar.skin'
  ]);

  for (const slotId of Object.keys(slots)) {
    if (!allowedSlotKeys.has(slotId)) {
      throw new functions.https.HttpsError('invalid-argument', `Unknown slot key: "${slotId}".`);
    }
    const val = slots[slotId];
    if (val !== null && (typeof val !== 'object' || !val.productId || !val.assetId)) {
      throw new functions.https.HttpsError('invalid-argument', `Invalid value for slot "${slotId}".`);
    }
    // Reject unknown keys inside slot value object
    if (val !== null) {
      const allowedSlotItemKeys = new Set(['productId', 'assetId']);
      for (const k of Object.keys(val)) {
        if (!allowedSlotItemKeys.has(k)) {
          throw new functions.https.HttpsError('invalid-argument', `Unknown key "${k}" inside slot "${slotId}".`);
        }
      }
    }
  }

  // Validate stickers
  if (stickers.length > 8) {
    throw new functions.https.HttpsError('invalid-argument', 'The profile canvas exceeds the maximum sticker count.');
  }

  const allowedStickerKeys = new Set(['productId', 'assetId', 'x', 'y', 'scale', 'rotation']);
  for (const sticker of stickers) {
    if (typeof sticker !== 'object' || sticker === null) {
      throw new functions.https.HttpsError('invalid-argument', 'Each sticker must be a JSON object.');
    }
    for (const key of Object.keys(sticker)) {
      if (!allowedStickerKeys.has(key)) {
        throw new functions.https.HttpsError('invalid-argument', `Unknown sticker field: "${key}".`);
      }
    }
    const { productId: spid, assetId: said, x, y } = sticker;
    if (typeof spid !== 'string' || !spid || !SAFE_ID.test(spid)) {
      throw new functions.https.HttpsError('invalid-argument', 'Invalid sticker productId reference');
    }
    if (typeof said !== 'string' || !said || !SAFE_ID.test(said)) {
      throw new functions.https.HttpsError('invalid-argument', 'Invalid sticker assetId reference');
    }
    if (typeof x !== 'number' || !Number.isFinite(x)) {
      throw new functions.https.HttpsError('invalid-argument', 'Invalid sticker x coordinate');
    }
    if (typeof y !== 'number' || !Number.isFinite(y)) {
      throw new functions.https.HttpsError('invalid-argument', 'Invalid sticker y coordinate');
    }
  }

  // Compile list of items to validate
  const itemsToValidate: { productId: string; assetId: string; slotId: string }[] = [];

  for (const slotId of Object.keys(slots)) {
    const item = slots[slotId];
    if (item) {
      itemsToValidate.push({
        productId: item.productId,
        assetId: item.assetId,
        slotId: slotId
      });
    }
  }

  for (const sticker of stickers) {
    itemsToValidate.push({
      productId: sticker.productId,
      assetId: sticker.assetId,
      slotId: 'profile.canvas.sticker'
    });
  }

  const now = nowMillis();

  // 7. Transactional Validation
  await db().runTransaction(async (tx) => {
    for (const item of itemsToValidate) {
      const { productId: pid, assetId: aid, slotId: sid } = item;

      // References
      const catalogRef = db().collection('marketplace_catalog').doc(pid);
      const assetRef = db().collection('marketplace_assets').doc(aid);
      const entitlementRef = db().collection('user_entitlements').doc(uid).collection('items').doc(pid);

      // fetch entitlement
      const entitlementSnap = await tx.get(entitlementRef);
      if (!entitlementSnap.exists) {
        throw new functions.https.HttpsError('failed-precondition', 'The user does not own this entitlement.');
      }
      const entitlementData = entitlementSnap.data()!;
      if (entitlementData.status !== 'ACTIVE') {
        throw new functions.https.HttpsError('failed-precondition', 'The entitlement has been revoked.');
      }
      if (entitlementData.expiresAtMillis && now > entitlementData.expiresAtMillis) {
        throw new functions.https.HttpsError('failed-precondition', 'The entitlement has expired.');
      }

      // fetch catalog product
      const catalogSnap = await tx.get(catalogRef);
      if (!catalogSnap.exists) {
        throw new functions.https.HttpsError('failed-precondition', 'The requested product does not exist.');
      }
      const productData = catalogSnap.data()!;
      if (productData.status !== 'ACTIVE') {
        throw new functions.https.HttpsError('failed-precondition', 'The requested product is currently unavailable.');
      }

      // Verify asset belongs to the product
      const assetIds: string[] = productData.assetIds || [];
      if (!assetIds.includes(aid)) {
        throw new functions.https.HttpsError('failed-precondition', 'The asset does not belong to the requested product.');
      }

      // Fetch asset
      const assetSnap = await tx.get(assetRef);
      if (!assetSnap.exists) {
        throw new functions.https.HttpsError('failed-precondition', 'The requested asset does not exist.');
      }
      const assetData = assetSnap.data()!;
      // Soft or hard suspended assets cannot be published (must be strictly ACTIVE)
      if (assetData.status !== 'ACTIVE') {
        throw new functions.https.HttpsError('failed-precondition', 'The requested asset is suspended.');
      }

      // Verify slot compatibility
      const compatibleSlots: string[] = assetData.compatibleSlots || [];
      if (!compatibleSlots.includes(sid)) {
        throw new functions.https.HttpsError('failed-precondition', 'The asset is not compatible with the designated slot.');
      }

      // Sticker format/type validation
      if (sid === 'profile.canvas.sticker') {
        if (assetData.type !== 'STICKER') {
          throw new functions.https.HttpsError('failed-precondition', 'The asset is not a sticker.');
        }
      }
    }

    // 8. Write public snapshot (server-only write, using updatedAtMillis)
    const publicCardRef = db().collection('profile_cards_public').doc(uid);
    tx.set(publicCardRef, {
      schemaVersion: 2,
      ownerUid: uid,
      payload: payloadString,
      updatedAtMillis: now
    });
  });

  return { success: true };
});
