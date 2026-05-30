/**
 * walletErrors.ts
 * Unified error model for wallet / economic operations.
 *
 * Every wallet callable returns errors in a consistent shape so the KMP
 * client can parse them uniformly. Firebase callable errors already carry
 * a `code` (FunctionsErrorCode) and `message`. We add a structured
 * `details` payload for machine-readable context.
 */

import * as functions from 'firebase-functions';

export interface WalletErrorDetails {
  /** Stable machine-readable error code (e.g., "INSUFFICIENT_BALANCE"). */
  errorCode: string;
  /** Correlation ID for tracing across client ↔ server. */
  correlationId: string;
  /** ISO-8601 timestamp of the error. */
  timestamp: string;
  /** Optional: which field caused the error. */
  field?: string;
}

export type WalletErrorCode =
  | 'INSUFFICIENT_BALANCE'
  | 'WALLET_NOT_FOUND'
  | 'WALLET_FROZEN'
  | 'WALLET_SUSPENDED'
  | 'DUPLICATE_IDEMPOTENCY_KEY'
  | 'IDEMPOTENCY_KEY_CONFLICT'
  | 'PURCHASE_ALREADY_CREDITED'
  | 'PURCHASE_NOT_FOUND'
  | 'INVALID_PACK_ID'
  | 'PROVIDER_NOT_CONFIGURED'
  | 'PROVIDER_VERIFICATION_FAILED'
  | 'INVALID_WALLET_INPUT'
  | 'RATE_LIMITED'
  | 'FRAUD_DETECTED'
  | 'CONCURRENCY_CONFLICT';

const ERROR_CODE_TO_GRPC: Record<WalletErrorCode, functions.https.FunctionsErrorCode> = {
  INSUFFICIENT_BALANCE: 'failed-precondition',
  WALLET_NOT_FOUND: 'not-found',
  WALLET_FROZEN: 'failed-precondition',
  WALLET_SUSPENDED: 'permission-denied',
  DUPLICATE_IDEMPOTENCY_KEY: 'already-exists',
  IDEMPOTENCY_KEY_CONFLICT: 'aborted',
  PURCHASE_ALREADY_CREDITED: 'already-exists',
  PURCHASE_NOT_FOUND: 'not-found',
  INVALID_PACK_ID: 'invalid-argument',
  PROVIDER_NOT_CONFIGURED: 'failed-precondition',
  PROVIDER_VERIFICATION_FAILED: 'internal',
  INVALID_WALLET_INPUT: 'invalid-argument',
  RATE_LIMITED: 'resource-exhausted',
  FRAUD_DETECTED: 'permission-denied',
  CONCURRENCY_CONFLICT: 'aborted',
};

const SAFE_USER_MESSAGES: Record<WalletErrorCode, string> = {
  INSUFFICIENT_BALANCE: 'Insufficient balance for this operation.',
  WALLET_NOT_FOUND: 'Wallet not found.',
  WALLET_FROZEN: 'Wallet is temporarily frozen.',
  WALLET_SUSPENDED: 'Wallet has been suspended.',
  DUPLICATE_IDEMPOTENCY_KEY: 'This operation has already been processed.',
  IDEMPOTENCY_KEY_CONFLICT: 'Idempotency key conflict: same key used with different request.',
  PURCHASE_ALREADY_CREDITED: 'This purchase has already been credited.',
  PURCHASE_NOT_FOUND: 'Purchase not found.',
  INVALID_PACK_ID: 'Invalid pack identifier.',
  PROVIDER_NOT_CONFIGURED: 'Payment provider is not available.',
  PROVIDER_VERIFICATION_FAILED: 'Purchase verification failed.',
  INVALID_WALLET_INPUT: 'Invalid input.',
  RATE_LIMITED: 'Too many requests. Please wait and try again.',
  FRAUD_DETECTED: 'This operation has been blocked.',
  CONCURRENCY_CONFLICT: 'Temporary conflict. Please retry.',
};

export function walletError(
  errorCode: WalletErrorCode,
  correlationId: string,
  field?: string,
): functions.https.HttpsError {
  const details: WalletErrorDetails = {
    errorCode,
    correlationId,
    timestamp: new Date().toISOString(),
    ...(field ? { field } : {}),
  };

  return new functions.https.HttpsError(
    ERROR_CODE_TO_GRPC[errorCode],
    SAFE_USER_MESSAGES[errorCode],
    details,
  );
}
