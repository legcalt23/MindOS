// @ts-check
/**
 * embedder.mjs — provider-decoupled embeddings. The rest of the system depends
 * ONLY on the small Embedder shape below, never on a vendor SDK. Default is
 * LOCAL/OFFLINE (Transformers.js): zero egress, $0, no API key. If the optional
 * dependency is missing, createEmbedder returns null and the engine runs
 * keyword-only.
 *
 * @typedef {Object} Embedder
 * @property {string} id        Provider id (e.g. 'local').
 * @property {string} model
 * @property {number} dims
 * @property {(texts: string[], opts?: { inputType?: 'document'|'query' }) => Promise<number[][]>} embed
 */

import { modelCacheDir, ensureDir } from './paths.mjs';

// bge-v1.5 retrieval instruction for the QUERY side (asymmetric embeddings).
const BGE_QUERY_INSTRUCTION = 'Represent this sentence for searching relevant passages: ';

/**
 * Build an Embedder from config, or null to signal keyword-only.
 * @param {object} cfg
 * @returns {Promise<Embedder|null>}
 */
export async function createEmbedder(cfg) {
  const kind = cfg?.embedder || 'local';
  if (kind === 'none') return null;
  if (kind === 'local') return createLocalEmbedder(cfg);
  // 'api:<provider>' is reserved for a future opt-in; degrade safely for now.
  return null;
}

async function createLocalEmbedder(cfg) {
  const model = cfg?.embedModel || 'Xenova/bge-small-en-v1.5';
  const dims = Number(cfg?.dimensions || 384);
  let extractor;
  try {
    const tf = await import('@huggingface/transformers');
    // Cache the model under the gitignored index dir so it travels per-machine.
    if (tf.env) {
      tf.env.cacheDir = ensureDir(modelCacheDir(cfg));
      tf.env.allowLocalModels = true;
    }
    extractor = await tf.pipeline('feature-extraction', model);
  } catch {
    return null; // optional dependency unavailable -> keyword-only
  }
  const isBge = /bge/i.test(model);
  return {
    id: 'local',
    model,
    dims,
    async embed(texts, opts = {}) {
      const prepared = texts.map((t) =>
        (isBge && opts.inputType === 'query') ? BGE_QUERY_INSTRUCTION + t : t);
      const out = await extractor(prepared, { pooling: 'mean', normalize: true });
      return tensorToVectors(out, prepared.length, dims);
    },
  };
}

/** Convert a Transformers.js tensor into number[][]. */
function tensorToVectors(out, count, dims) {
  const data = out?.data ?? out;
  const flat = Array.from(data);
  const stride = flat.length / count;
  const vectors = [];
  for (let i = 0; i < count; i++) {
    vectors.push(flat.slice(i * stride, i * stride + Math.min(stride, dims)));
  }
  return vectors;
}

/** JSON encoding sqlite-vec accepts directly for a vector value. */
export function vectorToJson(vec) {
  return JSON.stringify(Array.from(vec));
}
