// @ts-check
/**
 * schema.mjs — the canonical memory data model. One place defines the 15 memory
 * types, the field set, allowed enum values, validation, and id derivation.
 * Everything else (CLI, store, indexer) imports from here.
 *
 * @typedef {(
 *   'PRODUCT_VISION'|'ARCHITECTURE'|'ADR'|'FEATURE_SPEC'|'ROADMAP'|'BUG_REPORT'|
 *   'SESSION_SUMMARY'|'CODING_STANDARD'|'SECURITY_NOTE'|'PROMPT_PATTERN'|
 *   'RESEARCH_NOTE'|'USER_PERSONA'|'API_CONTRACT'|'DATA_MODEL'|'DEPLOYMENT_NOTE'
 * )} MemoryType
 *
 * @typedef {'low'|'medium'|'high'|'critical'} Importance
 * @typedef {'public'|'internal'|'sensitive'} AccessLevel
 *
 * @typedef {Object} MemoryEntry
 * @property {string} id                 Stable, unique, kebab. Drives the file path.
 * @property {MemoryType} type
 * @property {string} title
 * @property {string} content            Markdown body (NOT stored in frontmatter).
 * @property {string[]} tags
 * @property {string|null} source        Backref to a source file (docs/…) — never a copy.
 * @property {Importance} importance
 * @property {string} created_at         YYYY-MM-DD.
 * @property {string} updated_at         YYYY-MM-DD.
 * @property {number} version            Bumped on edit; git holds the full chain.
 * @property {boolean} encrypted         Reserved. Always false in v1 (no encryption subsystem).
 * @property {AccessLevel} access_level
 * @property {Object<string, unknown>} metadata
 * @property {string[]} [related]        Optional backlinks (lightweight knowledge graph).
 * @property {string|null} [supersedes]  Optional id this entry replaces.
 * @property {string|null} [superseded_by]
 */

/** All 15 memory types (order is the display/enum order). */
export const MEMORY_TYPES = /** @type {MemoryType[]} */ ([
  'PRODUCT_VISION',
  'ARCHITECTURE',
  'ADR',
  'FEATURE_SPEC',
  'ROADMAP',
  'BUG_REPORT',
  'SESSION_SUMMARY',
  'CODING_STANDARD',
  'SECURITY_NOTE',
  'PROMPT_PATTERN',
  'RESEARCH_NOTE',
  'USER_PERSONA',
  'API_CONTRACT',
  'DATA_MODEL',
  'DEPLOYMENT_NOTE',
]);

export const IMPORTANCE_LEVELS = /** @type {Importance[]} */ (['low', 'medium', 'high', 'critical']);
export const ACCESS_LEVELS = /** @type {AccessLevel[]} */ (['public', 'internal', 'sensitive']);

/** One-line human descriptions (used by templates + `memory types`). */
export const TYPE_DESCRIPTIONS = {
  PRODUCT_VISION: 'Vision, principles, value proposition, product boundaries.',
  ARCHITECTURE: 'System architecture, module boundaries, data flow.',
  ADR: 'Architectural Decision Record: decision, context, trade-offs, consequences.',
  FEATURE_SPEC: 'Specification of a feature: scope, behavior, acceptance criteria.',
  ROADMAP: 'Priorities, phases, what is in/out of scope and why.',
  BUG_REPORT: 'A known bug: symptom, root cause, status, affected files.',
  SESSION_SUMMARY: 'Dated summary of a work session: what changed, decisions, next steps.',
  CODING_STANDARD: 'A reusable technical rule/convention for this codebase.',
  SECURITY_NOTE: 'A security decision, threat, or hardening rule.',
  PROMPT_PATTERN: 'A prompt/phrasing that works well for this codebase with Claude Code.',
  RESEARCH_NOTE: 'Relevant research, comparison, or external finding (with sources).',
  USER_PERSONA: 'A target user profile (no real PII).',
  API_CONTRACT: 'An API contract: endpoint/callable, inputs, outputs, errors.',
  DATA_MODEL: 'A data model / schema and its invariants.',
  DEPLOYMENT_NOTE: 'Deployment, infra, environments, release procedure.',
};

/** Canonical field order for frontmatter serialization (stable git diffs). */
export const FIELD_ORDER = [
  'id', 'type', 'title', 'tags', 'source', 'importance',
  'created_at', 'updated_at', 'version', 'encrypted', 'access_level',
  'related', 'supersedes', 'superseded_by', 'metadata',
];

/** kebab-case slug from arbitrary text. */
export function slugify(text) {
  return String(text)
    .normalize('NFKD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 60) || 'untitled';
}

/**
 * Derive a stable id from type + title (+ optional date for dated types).
 * e.g. ADR "Virtual currency ledger" -> "adr-virtual-currency-ledger".
 *      SESSION_SUMMARY on 2026-06-30 -> "session-2026-06-30-<slug>".
 */
export function makeId(type, title, date) {
  const slug = slugify(title);
  if (type === 'SESSION_SUMMARY') {
    const d = date || todayIso();
    return `session-${d}-${slug}`;
  }
  const prefix = type.toLowerCase().replace(/_/g, '-');
  return `${prefix}-${slug}`;
}

/** YYYY-MM-DD for "now" (host local date). */
export function todayIso(now = new Date()) {
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, '0');
  const d = String(now.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

/**
 * Fill defaults + coerce a partial entry into a full MemoryEntry.
 * @param {Partial<MemoryEntry>} input
 * @returns {MemoryEntry}
 */
export function normalizeEntry(input) {
  const type = /** @type {MemoryType} */ (String(input.type || '').toUpperCase());
  const title = String(input.title || '').trim();
  const created = input.created_at || todayIso();
  return {
    id: input.id || makeId(type, title, created),
    type,
    title,
    content: String(input.content || '').trim(),
    tags: dedupeStrings(input.tags),
    source: input.source ? String(input.source) : null,
    importance: /** @type {Importance} */ (input.importance || 'medium'),
    created_at: created,
    updated_at: input.updated_at || todayIso(),
    version: Number.isFinite(input.version) ? Number(input.version) : 1,
    encrypted: false, // v1 invariant
    access_level: /** @type {AccessLevel} */ (input.access_level || 'internal'),
    metadata: input.metadata && typeof input.metadata === 'object' ? input.metadata : {},
    related: dedupeStrings(input.related),
    supersedes: input.supersedes ? String(input.supersedes) : null,
    superseded_by: input.superseded_by ? String(input.superseded_by) : null,
  };
}

function dedupeStrings(arr) {
  if (!Array.isArray(arr)) return [];
  return [...new Set(arr.map((x) => String(x).trim()).filter(Boolean))];
}

/**
 * Validate an entry. Returns { ok, errors }. Fail-closed callers reject on !ok.
 * @param {Partial<MemoryEntry>} e
 */
export function validateEntry(e) {
  const errors = [];
  if (!e || typeof e !== 'object') return { ok: false, errors: ['entry is not an object'] };
  if (!e.id || !/^[a-z0-9][a-z0-9-]*$/.test(String(e.id))) errors.push(`invalid id: ${e.id}`);
  if (!MEMORY_TYPES.includes(/** @type {MemoryType} */ (e.type))) errors.push(`unknown type: ${e.type}`);
  if (!e.title || !String(e.title).trim()) errors.push('title is required');
  if (!e.content || !String(e.content).trim()) errors.push('content is required');
  if (e.importance && !IMPORTANCE_LEVELS.includes(/** @type {Importance} */ (e.importance))) {
    errors.push(`invalid importance: ${e.importance}`);
  }
  if (e.access_level && !ACCESS_LEVELS.includes(/** @type {AccessLevel} */ (e.access_level))) {
    errors.push(`invalid access_level: ${e.access_level}`);
  }
  if (e.created_at && !/^\d{4}-\d{2}-\d{2}$/.test(String(e.created_at))) errors.push('created_at must be YYYY-MM-DD');
  return { ok: errors.length === 0, errors };
}

export function isMemoryType(value) {
  return MEMORY_TYPES.includes(/** @type {MemoryType} */ (value));
}
