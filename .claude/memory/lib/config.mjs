// @ts-check
/**
 * config.mjs — load and normalize config.json with safe defaults, so a missing
 * or partial config never crashes the engine.
 */

import fs from 'node:fs';
import { configPath } from './paths.mjs';

const DEFAULTS = {
  embedder: 'local',
  embedModel: 'Xenova/bge-small-en-v1.5',
  dimensions: 384,
  retrieval: { k: 8, spliceTokenBudget: 1500, perHitCharCap: 600, rrfK: 60, keywordBias: 1.15, exactTokenBoost: true },
  chunking: { targetTokens: 400, maxTokens: 512, overlapTokens: 40 },
  paths: { indexDir: '.claude/.memory' },
  ingest: { external: [] },
  redaction: { failClosed: true, forbidSensitivePersistence: true },
  audit: { enabled: true },
};

let cached = null;

/** Load merged config (cached for the process). */
export function loadConfig(force = false) {
  if (cached && !force) return cached;
  let user = {};
  try {
    user = JSON.parse(fs.readFileSync(configPath(), 'utf8'));
  } catch {
    user = {};
  }
  cached = deepMerge(DEFAULTS, user);
  // Env overrides (useful for tests / forcing keyword-only without editing config).
  if (process.env.MINDOS_MEMORY_EMBEDDER) cached.embedder = process.env.MINDOS_MEMORY_EMBEDDER;
  return cached;
}

function deepMerge(base, over) {
  if (Array.isArray(over)) return over.slice();
  if (over && typeof over === 'object' && !Array.isArray(over)) {
    const out = { ...base };
    for (const [k, v] of Object.entries(over)) {
      if (k.startsWith('_') || k.startsWith('$')) continue;
      out[k] = (k in base && base[k] && typeof base[k] === 'object' && !Array.isArray(base[k]))
        ? deepMerge(base[k], v)
        : v;
    }
    return out;
  }
  return over === undefined ? base : over;
}
