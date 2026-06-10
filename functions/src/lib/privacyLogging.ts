/**
 * Redaction helpers for Cloud Functions logs.
 *
 * Never interpolate UIDs, document IDs, emails, Storage paths or follow pairs
 * directly into logs. Use short keyed digests so operators can correlate events
 * without retaining raw identifiers in Cloud Logging.
 */

import { createHmac, createHash } from 'crypto';

const DIGEST_BYTES = 12;

function logSecret(): string | null {
  return process.env['LOG_REDACTION_SECRET'] || process.env['PII_LOG_HMAC_SECRET'] || null;
}

export function logSafeRef(value: unknown, label = 'id'): string {
  const normalized = typeof value === 'string' ? value : String(value ?? '');
  if (!normalized) return `<redacted:${label}:empty>`;
  const secret = logSecret();
  const digest = secret
    ? createHmac('sha256', secret).update(normalized).digest('hex')
    : createHash('sha256').update(`mindos-log-redaction:${normalized}`).digest('hex');
  return `<redacted:${label}:${digest.slice(0, DIGEST_BYTES)}>`;
}

export function logSafeStoragePrefix(prefix: string): string {
  const root = prefix.split('/').filter(Boolean)[0] || 'storage';
  return logSafeRef(prefix, root);
}

function safeErrorText(value: unknown, fallback: string): string {
  return typeof value === 'string' && /^[A-Za-z0-9_.:/ -]{1,96}$/.test(value)
    ? value
    : fallback;
}

/**
 * Returns a log-safe error descriptor. Never log raw Error objects: stacks and
 * messages can include UIDs, Storage paths, tokens, emails or provider payloads.
 */
export function logSafeError(error: unknown): Record<string, string> {
  const record = error && typeof error === 'object'
    ? error as { code?: unknown; name?: unknown; message?: unknown }
    : {};
  const code = safeErrorText(record.code, 'unknown');
  const name = safeErrorText(record.name, error instanceof Error ? error.name : typeof error);
  const message = typeof record.message === 'string'
    ? record.message
    : typeof error === 'string'
      ? error
      : '';

  return {
    errorCode: code,
    errorName: name,
    ...(message ? { errorMessageRef: logSafeRef(message, 'errorMessage') } : {}),
  };
}
