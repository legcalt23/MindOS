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
