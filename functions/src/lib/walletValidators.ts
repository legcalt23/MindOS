/**
 * walletValidators.ts
 * Strict input validation for all wallet / economic Cloud Function callables.
 *
 * Security rules:
 *  - Reject unknown/extra fields on economic endpoints.
 *  - Never accept `amount`, `newBalance`, or `price` from the client.
 *  - Pack ID must be from the server-side catalog.
 *  - Idempotency keys must be safe IDs.
 *  - All string fields validated for length and control characters.
 */

import * as functions from 'firebase-functions';

function reject(msg: string): never {
  throw new functions.https.HttpsError('invalid-argument', msg);
}

const SAFE_ID = /^[A-Za-z0-9_-]{3,128}$/;
const UUID_V4 = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const CONTROL_CHARS = /[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]/;

// Fields that a client must NEVER send on economic endpoints.
const FORBIDDEN_CLIENT_FIELDS = new Set([
  'amount',
  'newBalance',
  'balance',
  'price',
  'priceCents',
  'coinsAmount',
  'coins',
  'walletId',
  'userId',
  'ledgerTransactionId',
  'status',
]);

function assertObject(data: unknown): Record<string, unknown> {
  if (!data || typeof data !== 'object' || Array.isArray(data)) {
    reject('Payload must be a non-null object.');
  }
  return data as Record<string, unknown>;
}

function rejectForbiddenFields(data: Record<string, unknown>, allowedKeys: Set<string>): void {
  for (const key of Object.keys(data)) {
    if (key === 'correlationId') continue;
    if (FORBIDDEN_CLIENT_FIELDS.has(key)) {
      reject(`Field "${key}" is not accepted from the client.`);
    }
    if (!allowedKeys.has(key)) {
      reject(`Unknown field "${key}".`);
    }
  }
}

function assertSafeId(value: unknown, field: string): string {
  if (typeof value !== 'string' || value.length === 0) {
    reject(`"${field}" must be a non-empty string.`);
  }
  const s = (value as string).trim();
  if (!SAFE_ID.test(s)) {
    reject(`"${field}" contains invalid characters or invalid length (3-128, alphanumeric/dash/underscore).`);
  }
  if (CONTROL_CHARS.test(s)) {
    reject(`"${field}" contains control characters.`);
  }
  return s;
}

function assertIdempotencyKey(value: unknown): string {
  if (typeof value !== 'string' || value.length === 0) {
    reject('"idempotencyKey" is required.');
  }
  const s = (value as string).trim();
  if (s.length < 8 || s.length > 128) {
    reject('"idempotencyKey" must be between 8 and 128 characters.');
  }
  if (!SAFE_ID.test(s) && !UUID_V4.test(s)) {
    reject('"idempotencyKey" must be alphanumeric/dash/underscore or a valid UUID v4.');
  }
  if (CONTROL_CHARS.test(s)) {
    reject('"idempotencyKey" contains control characters.');
  }
  return s;
}

// ── Pack catalog (server-authoritative) ──────────────────────────────────────

export interface PackDefinition {
  packId: string;
  coins: number;
  priceCents: number;
  currency: string;
}

export const PACK_CATALOG: Record<string, PackDefinition> = {
  coins_1:   { packId: 'coins_1',   coins: 1,   priceCents: 0, currency: 'SANDBOX' },
  coins_10:  { packId: 'coins_10',  coins: 10,  priceCents: 0, currency: 'SANDBOX' },
  coins_100: { packId: 'coins_100', coins: 100, priceCents: 0, currency: 'SANDBOX' },
};

// ── Validators ───────────────────────────────────────────────────────────────

export interface GetBalanceInput {
  correlationId?: string;
}

const GET_BALANCE_KEYS = new Set<string>([]);

export function validateGetBalanceInput(data: unknown): GetBalanceInput {
  if (data === undefined || data === null) return {};
  const d = assertObject(data);
  rejectForbiddenFields(d, GET_BALANCE_KEYS);
  return { correlationId: typeof d['correlationId'] === 'string' ? d['correlationId'] as string : undefined };
}

export interface GetTransactionsInput {
  limit?: number;
  cursor?: string;
  correlationId?: string;
}

const GET_TRANSACTIONS_KEYS = new Set(['limit', 'cursor']);

export function validateGetTransactionsInput(data: unknown): GetTransactionsInput {
  if (data === undefined || data === null) return {};
  const d = assertObject(data);
  rejectForbiddenFields(d, GET_TRANSACTIONS_KEYS);

  let limit: number | undefined;
  if (d['limit'] !== undefined) {
    if (typeof d['limit'] !== 'number' || !Number.isInteger(d['limit']) || d['limit'] < 1 || d['limit'] > 100) {
      reject('"limit" must be an integer between 1 and 100.');
    }
    limit = d['limit'] as number;
  }

  let cursor: string | undefined;
  if (d['cursor'] !== undefined) {
    cursor = assertSafeId(d['cursor'], 'cursor');
  }

  return { limit, cursor };
}

export interface SpendCoinsInput {
  itemId: string;
  quantity: number;
  idempotencyKey: string;
  correlationId?: string;
}

const SPEND_COINS_KEYS = new Set(['itemId', 'quantity', 'idempotencyKey']);

export function validateSpendCoinsInput(data: unknown): SpendCoinsInput {
  const d = assertObject(data);
  rejectForbiddenFields(d, SPEND_COINS_KEYS);

  const itemId = assertSafeId(d['itemId'], 'itemId');
  const idempotencyKey = assertIdempotencyKey(d['idempotencyKey']);

  if (typeof d['quantity'] !== 'number' || !Number.isInteger(d['quantity']) || d['quantity'] < 1 || d['quantity'] > 1000) {
    reject('"quantity" must be an integer between 1 and 1000.');
  }
  const quantity = d['quantity'] as number;

  return { itemId, quantity, idempotencyKey };
}

export interface InitiatePurchaseInput {
  packId: string;
  idempotencyKey: string;
  correlationId?: string;
}

const INITIATE_PURCHASE_KEYS = new Set(['packId', 'idempotencyKey']);

export function validateInitiatePurchaseInput(data: unknown): InitiatePurchaseInput {
  const d = assertObject(data);
  rejectForbiddenFields(d, INITIATE_PURCHASE_KEYS);

  const idempotencyKey = assertIdempotencyKey(d['idempotencyKey']);

  // SECURITY: use Object.prototype.hasOwnProperty to bypass the prototype chain.
  // The `in` operator would allow `__proto__`, `toString`, `constructor`, etc.
  // to satisfy the allowlist check, producing a PACK_CATALOG[packId] value
  // that is the inherited prototype method instead of a pack definition.
  // Even though the downstream Firestore write would eventually reject the
  // malformed document, defense in depth requires the validator itself to
  // refuse the input.
  if (
    typeof d['packId'] !== 'string' ||
    !Object.prototype.hasOwnProperty.call(PACK_CATALOG, d['packId'] as string)
  ) {
    reject(`"packId" must be one of: ${Object.keys(PACK_CATALOG).join(', ')}.`);
  }
  const packId = d['packId'] as string;

  return { packId, idempotencyKey };
}

export interface VerifyPurchaseInput {
  sandboxPurchaseId: string;
  idempotencyKey: string;
  correlationId?: string;
}

const VERIFY_PURCHASE_KEYS = new Set(['sandboxPurchaseId', 'idempotencyKey']);

export function validateVerifyPurchaseInput(data: unknown): VerifyPurchaseInput {
  const d = assertObject(data);
  rejectForbiddenFields(d, VERIFY_PURCHASE_KEYS);

  const sandboxPurchaseId = assertSafeId(d['sandboxPurchaseId'], 'sandboxPurchaseId');
  const idempotencyKey = assertIdempotencyKey(d['idempotencyKey']);

  return { sandboxPurchaseId, idempotencyKey };
}
