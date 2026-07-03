// @ts-check
import { test } from 'node:test';
import assert from 'node:assert/strict';

import { parseFrontmatter, serializeFrontmatter, entryToFile, fileToEntry } from '../lib/frontmatter.mjs';
import { normalizeEntry, validateEntry, makeId, slugify, MEMORY_TYPES } from '../lib/schema.mjs';
import { guardText, guardEntry, redactForModel, findingPatternNames } from '../lib/redaction.mjs';
import { chunkMarkdown, estimateTokens } from '../lib/chunker.mjs';

// Realistic-but-fake secrets, assembled at runtime so the verbatim key never
// appears in source. Tagged for any scanner that reads this file.
const FAKE_AWS_KEY = 'AKIA' + 'IOSFODNN7EXAMPLE'; // FAKE_SECRET_FIXTURE
const FAKE_GOOGLE_KEY = 'AIza' + 'SyA1234567890123456789012345678901234'; // FAKE_SECRET_FIXTURE

// ── frontmatter round-trip ───────────────────────────────────────────────────
test('frontmatter: round-trips scalars, arrays, and metadata JSON', () => {
  const entry = normalizeEntry({
    type: 'ADR', title: 'Some: decision with colon', tags: ['a', 'b-c'],
    content: 'Body line one.\n\nBody line two.', importance: 'high',
    source: 'docs/x.md', metadata: { k: 'v', n: 3 },
  });
  const file = entryToFile(entry);
  const back = fileToEntry(file);
  assert.equal(back.type, 'ADR');
  assert.equal(back.title, 'Some: decision with colon'); // colon survives via quoting
  assert.deepEqual(back.tags, ['a', 'b-c']);
  assert.equal(back.importance, 'high');
  assert.deepEqual(back.metadata, { k: 'v', n: 3 });
  assert.match(back.content, /Body line one/);
  assert.match(back.content, /Body line two/);
});

test('frontmatter: body without frontmatter parses as content', () => {
  const { data, body } = parseFrontmatter('# Just markdown\n\ntext');
  assert.deepEqual(data, {});
  assert.match(body, /Just markdown/);
});

test('frontmatter: serialize emits canonical field order', () => {
  const text = serializeFrontmatter({ title: 'T', id: 'x', type: 'ADR' }, 'body');
  const idLine = text.indexOf('id:');
  const titleLine = text.indexOf('title:');
  assert.ok(idLine < titleLine, 'id should serialize before title');
});

// ── schema ────────────────────────────────────────────────────────────────────
test('schema: makeId is stable and kebab', () => {
  assert.equal(makeId('ADR', 'Virtual Currency Ledger'), 'adr-virtual-currency-ledger');
  assert.equal(slugify('Áćçèntéd Tîtle!!'), 'accented-title');
});

test('schema: validateEntry rejects unknown type and missing fields', () => {
  assert.equal(validateEntry(normalizeEntry({ type: 'NOPE', title: 't', content: 'c' })).ok, false);
  assert.equal(validateEntry(normalizeEntry({ type: 'ADR', title: '', content: 'c' })).ok, false);
  assert.equal(validateEntry(normalizeEntry({ type: 'ADR', title: 't', content: 'c' })).ok, true);
});

test('schema: 15 types are defined', () => {
  assert.equal(MEMORY_TYPES.length, 15);
});

test('schema: encrypted is always false in v1', () => {
  const e = normalizeEntry({ type: 'SECURITY_NOTE', title: 't', content: 'c', encrypted: true });
  assert.equal(e.encrypted, false);
});

// ── redaction (the hardest invariant) ────────────────────────────────────────
test('redaction: catches AWS key, Google key, email, and credit card', () => {
  assert.equal(guardText('key = "' + FAKE_AWS_KEY + '"').ok, false);
  assert.equal(guardText(FAKE_GOOGLE_KEY).ok, false);
  assert.equal(guardText('reach me at jane.doe@example.com').ok, false);
  assert.equal(guardText('card 4111 1111 1111 1111').ok, false); // Luhn-valid
});

test('redaction: allows ordinary prose and benign 16-digit non-card numbers', () => {
  assert.equal(guardText('A coding standard about Compose state hoisting and detekt.').ok, true);
  assert.equal(guardText('order id 1234567890123456 was not a card').ok, true); // not Luhn-valid
});

test('redaction: honors the inline allow tag', () => {
  assert.equal(guardText('key = "' + FAKE_AWS_KEY + '" // FAKE' + '_SECRET_FIXTURE').ok, true);
});

test('redaction: guardEntry scans content, title and metadata', () => {
  const r = guardEntry({ title: 'ok', content: 'fine', metadata: { note: 'ping 8.8.8.8' } });
  assert.ok(findingPatternNames(r.findings).includes('ipv4'));
});

test('redaction: redactForModel scrubs secrets/PII to placeholders', () => {
  const scrubbed = redactForModel('email a@b.com and key ' + FAKE_AWS_KEY);
  assert.match(scrubbed, /‹redacted:email›/);
  assert.doesNotMatch(scrubbed, /a@b\.com/);
});

// ── chunker ──────────────────────────────────────────────────────────────────
test('chunker: splits on headings and carries anchors', () => {
  const md = '# Title\n\nintro\n\n## Section A\n\nbody a\n\n## Section B\n\nbody b';
  const chunks = chunkMarkdown(md, { targetTokens: 100, maxTokens: 200 });
  assert.ok(chunks.length >= 2);
  assert.ok(chunks.some((c) => c.anchor === 'Section A'));
});

test('chunker: estimateTokens is monotonic', () => {
  assert.ok(estimateTokens('short') < estimateTokens('a much much longer piece of text here'));
});
