// @ts-check
/**
 * hashing.mjs — deterministic hashing used for content addressing, the
 * staleness manifest, the embedding cache key, and PII-free audit fields.
 * Pure node:crypto, no external deps.
 */

import crypto from 'node:crypto';

/** Full sha256 hex of a string. */
export function sha256(text) {
  return crypto.createHash('sha256').update(text, 'utf8').digest('hex');
}

/** Short (first 16 hex) digest — enough for de-dup / correlation, not reversible. */
export function shortHash(text) {
  return sha256(text).slice(0, 16);
}

/**
 * Normalize text before hashing so cosmetic whitespace changes don't churn
 * the cache: collapse runs of whitespace, trim, normalize line endings.
 */
export function normalizeForHash(text) {
  return String(text == null ? '' : text)
    .replace(/\r\n/g, '\n')
    .replace(/[ \t]+/g, ' ')
    .replace(/\n{3,}/g, '\n\n')
    .trim();
}

/** Content hash of an entry body / chunk text. */
export function contentHash(text) {
  return sha256(normalizeForHash(text));
}

/**
 * Embedding cache key. MUST include provider + model + dims so switching
 * providers can never return a vector from the wrong embedding space.
 */
export function embeddingCacheKey(provider, model, dims, text) {
  return sha256(`${provider}:${model}:${dims}:${normalizeForHash(text)}`);
}

/** Hash a query for audit correlation WITHOUT storing the query text. */
export function queryHash(query) {
  return shortHash(normalizeForHash(query));
}
