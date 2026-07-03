// @ts-check
/**
 * frontmatter.mjs — a tiny, dependency-free reader/writer for the constrained
 * YAML-ish frontmatter the memory engine authors. We control the shape, so we
 * support exactly what we emit: scalars (string/number/bool/null), inline
 * string arrays `[a, b, c]`, block lists (`- item`), and a compact JSON object
 * for `metadata`. This avoids pulling js-yaml/gray-matter (one fewer supply
 * chain + Windows-install risk, per the design's "mostly stdlib" stance).
 *
 * @typedef {import('./schema.mjs').MemoryEntry} MemoryEntry
 */

import { FIELD_ORDER } from './schema.mjs';

const FENCE = '---';

/**
 * Split a memory file into { data, body }.
 * @param {string} fileText
 * @returns {{ data: Record<string, unknown>, body: string }}
 */
export function parseFrontmatter(fileText) {
  const text = String(fileText).replace(/\r\n/g, '\n');
  if (!text.startsWith(FENCE + '\n')) {
    return { data: {}, body: text.trim() };
  }
  const end = text.indexOf('\n' + FENCE, FENCE.length);
  if (end === -1) return { data: {}, body: text.trim() };
  const head = text.slice(FENCE.length + 1, end);
  const body = text.slice(end + 1 + FENCE.length + 1).replace(/^\n/, '');
  return { data: parseBlock(head), body: body.trim() };
}

/** Parse the frontmatter body (between the fences). */
function parseBlock(head) {
  /** @type {Record<string, unknown>} */
  const data = {};
  const lines = head.split('\n');
  let i = 0;
  while (i < lines.length) {
    const line = lines[i];
    if (!line.trim() || line.trim().startsWith('#')) { i++; continue; }
    const m = line.match(/^([A-Za-z0-9_]+):\s*(.*)$/);
    if (!m) { i++; continue; }
    const key = m[1];
    const rawValue = m[2];
    if (rawValue === '') {
      // Possible block list on following indented `- ` lines.
      const items = [];
      let j = i + 1;
      while (j < lines.length && /^\s*-\s+/.test(lines[j])) {
        items.push(stripQuotes(lines[j].replace(/^\s*-\s+/, '').trim()));
        j++;
      }
      data[key] = items.length ? items : null;
      i = items.length ? j : i + 1;
      continue;
    }
    data[key] = parseScalarOrArray(rawValue);
    i++;
  }
  return data;
}

function parseScalarOrArray(raw) {
  const v = raw.trim();
  if (v === 'null' || v === '~' || v === '') return null;
  if (v === 'true') return true;
  if (v === 'false') return false;
  if (v.startsWith('[') && v.endsWith(']')) {
    const inner = v.slice(1, -1).trim();
    if (!inner) return [];
    return inner.split(',').map((s) => stripQuotes(s.trim())).filter((s) => s !== '');
  }
  if (v.startsWith('{')) {
    try { return JSON.parse(v); } catch { return v; }
  }
  if (/^-?\d+(\.\d+)?$/.test(v)) return Number(v);
  return stripQuotes(v);
}

function stripQuotes(s) {
  if ((s.startsWith('"') && s.endsWith('"')) || (s.startsWith("'") && s.endsWith("'"))) {
    return s.slice(1, -1);
  }
  return s;
}

function needsQuote(s) {
  return /[:#]/.test(s) || /^[\s>|@`&*!%]/.test(s) || /[\n]/.test(s) || s.trim() !== s || s === '';
}

function emitScalar(value) {
  if (value === null || value === undefined) return 'null';
  if (typeof value === 'boolean' || typeof value === 'number') return String(value);
  const s = String(value);
  return needsQuote(s) ? JSON.stringify(s) : s;
}

function emitArray(arr) {
  if (!arr.length) return '[]';
  return '[' + arr.map((x) => emitScalar(x)).join(', ') + ']';
}

/**
 * Serialize an entry to a full Markdown file (frontmatter + body).
 * Fields are emitted in canonical order for stable git diffs.
 * @param {Record<string, unknown>} data
 * @param {string} body
 */
export function serializeFrontmatter(data, body) {
  const keys = [...FIELD_ORDER, ...Object.keys(data).filter((k) => !FIELD_ORDER.includes(k))];
  const out = [FENCE];
  for (const key of keys) {
    if (!(key in data)) continue;
    const value = data[key];
    if (Array.isArray(value)) {
      out.push(`${key}: ${emitArray(value)}`);
    } else if (value && typeof value === 'object') {
      out.push(`${key}: ${JSON.stringify(value)}`);
    } else {
      out.push(`${key}: ${emitScalar(value)}`);
    }
  }
  out.push(FENCE, '', String(body).trim(), '');
  return out.join('\n');
}

/**
 * Split an entry object into its frontmatter map (everything except content).
 * @param {MemoryEntry} entry
 */
export function entryToFile(entry) {
  const { content, ...frontmatter } = entry;
  return serializeFrontmatter(frontmatter, content || '');
}

/**
 * Parse a memory file back into a (loosely-typed) entry object.
 * @param {string} fileText
 * @returns {Record<string, unknown> & { content: string }}
 */
export function fileToEntry(fileText) {
  const { data, body } = parseFrontmatter(fileText);
  return { ...data, content: body };
}
