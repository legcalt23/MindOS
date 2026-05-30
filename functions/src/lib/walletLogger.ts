/**
 * walletLogger.ts
 * Structured logging for wallet operations with sanitization.
 *
 * Rules:
 *  - Never log full purchase tokens, secrets, or API keys.
 *  - Truncate token-like values to first 8 chars + "***".
 *  - Never log raw PII beyond userId.
 *  - Always include correlationId and action in log entries.
 */

import * as functions from 'firebase-functions';

export interface WalletLogContext {
  correlationId: string;
  action: string;
  userId?: string;
  walletId?: string;
  [key: string]: unknown;
}

const SENSITIVE_KEYS = new Set([
  'token',
  'purchaseToken',
  'providerToken',
  'secret',
  'apiKey',
  'password',
  'credential',
  'authorization',
  'accessToken',
  'refreshToken',
  'idToken',
]);

function sanitizeValue(key: string, value: unknown): unknown {
  if (typeof value !== 'string') return value;

  const lowerKey = key.toLowerCase();
  for (const sensitive of SENSITIVE_KEYS) {
    if (lowerKey.includes(sensitive.toLowerCase())) {
      if (value.length <= 8) return '***';
      return value.substring(0, 8) + '***';
    }
  }
  return value;
}

function sanitizeObject(obj: Record<string, unknown>): Record<string, unknown> {
  const result: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(obj)) {
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      result[key] = sanitizeObject(value as Record<string, unknown>);
    } else {
      result[key] = sanitizeValue(key, value);
    }
  }
  return result;
}

function buildLogEntry(ctx: WalletLogContext, extra?: Record<string, unknown>): Record<string, unknown> {
  const base: Record<string, unknown> = {
    correlationId: ctx.correlationId,
    action: ctx.action,
    ...(ctx.userId ? { userId: ctx.userId } : {}),
    ...(ctx.walletId ? { walletId: ctx.walletId } : {}),
  };

  if (extra) {
    Object.assign(base, sanitizeObject(extra));
  }

  return base;
}

export const walletLog = {
  info(ctx: WalletLogContext, message: string, extra?: Record<string, unknown>): void {
    functions.logger.info(message, buildLogEntry(ctx, extra));
  },

  warn(ctx: WalletLogContext, message: string, extra?: Record<string, unknown>): void {
    functions.logger.warn(message, buildLogEntry(ctx, extra));
  },

  error(ctx: WalletLogContext, message: string, extra?: Record<string, unknown>): void {
    functions.logger.error(message, buildLogEntry(ctx, extra));
  },
};
