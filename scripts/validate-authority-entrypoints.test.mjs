// node:test suite for the registry-driven authority validator.
// Run: node --test scripts/validate-authority-entrypoints.test.mjs
// Proves: (1) each error code fires on a one-defect fixture with NO collateral codes;
//         (2) the mandate's 4-part contraejemplo now FAILS; (3) RULE-ABLATION adequacy — disabling any rule
//         makes at least one targeted fixture stop reporting (every rule is load-bearing).
// NOTE: (3) is RULE ABLATION (whole-rule disable), NOT operator mutation. Real operator-level mutation
//       testing lives in scripts/authority-scanner.mutation.mjs (9/10 killed; 1 equivalent survivor).
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { scanContent, RULE_IDS } from './validate-authority-entrypoints.mjs';

const BASE_REG = {
  block_start: '<!-- FOCUS_V5_AUTHORITY_BLOCK_START -->',
  block_end: '<!-- FOCUS_V5_AUTHORITY_BLOCK_END -->',
  marker: 'FOCUS_V5_CURRENT_AUTHORITY_INDEX: docs/engines/focus_v5/CURRENT_AUTHORITY_INDEX.md',
  current_role_line: 'AUTHORITY_INDEX_ROLE: CURRENT',
  current_index: 'docs/engines/focus_v5/CURRENT_AUTHORITY_INDEX.md',
  entrypoints: ['CLAUDE.md', 'docs/engines/focus_v5/ADR_REGISTER_V5.md'],
  authority_dir: 'docs/engines/focus_v5',
  forbidden_legacy_vocab: ['SOLE AND CURRENT GATE AUTHORITY', 'I AM THE SOLE', 'SOLE\\s+(?:AND\\s+CURRENT\\s+)?[A-Z ]{0,25}AUTHORITY', 'AUTORIDAD\\s+ÚNICA[^\\n]{0,25}VIGENTE'],
  quarantine_negation_patterns: ['NOT dispositioned', 'no dispositioned', 'sin disposicionar'],
  docs: [
    { path: 'docs/engines/focus_v5/CURRENT_AUTHORITY_INDEX.md', role: 'CURRENT_INDEX', status: 'CURRENT', superseded_by: null },
    { path: 'docs/engines/focus_v5/GATE_CATALOG_V5.md', role: 'LEGACY', status: 'QUARANTINED', superseded_by: 'docs/engines/focus_v5/CURRENT_AUTHORITY_INDEX.md' },
    { path: 'docs/engines/focus_v5/FOCUS_ENGINE_MASTER_PLAN_V5.md', role: 'LEGACY', status: 'QUARANTINED', superseded_by: 'docs/engines/focus_v5/CURRENT_AUTHORITY_INDEX.md' },
  ],
};
const R = () => JSON.parse(JSON.stringify(BASE_REG));
const blk = (reg, line, eol = '\n') => ['x', reg.block_start, line, reg.block_end, 'y'].join(eol);
function baseFiles(reg, eol = '\n') {
  return new Map([
    ['CLAUDE.md', blk(reg, reg.marker, eol)],
    ['docs/engines/focus_v5/ADR_REGISTER_V5.md', blk(reg, reg.marker, eol)],
    [reg.current_index, blk(reg, reg.current_role_line, eol)],
    ['docs/engines/focus_v5/GATE_CATALOG_V5.md', 'gate catalog, clean vocabulary'],
    ['docs/engines/focus_v5/FOCUS_ENGINE_MASTER_PLAN_V5.md', 'master plan, clean'],
  ]);
}
const baseDir = (reg, eol = '\n') => new Map([
  ['CURRENT_AUTHORITY_INDEX.md', blk(reg, reg.current_role_line, eol)],
  ['GATE_CATALOG_V5.md', 'gate catalog, clean vocabulary'],
  ['FOCUS_ENGINE_MASTER_PLAN_V5.md', 'master plan, clean'],
]);
const scan = (files, reg, dirMd, disabled) => scanContent(files, reg, { dirMd, disabledRules: disabled });

// each negative fixture: exactly one defect -> ok=false, EVERY error has the expected code (no collateral).
function expectOnly(res, code) {
  assert.equal(res.ok, false, `expected FAIL for ${code}`);
  assert.ok(res.errors.some((e) => e.code === code), `expected code ${code}; got ${res.errors.map((e) => e.code).join(',') || '(none)'}`);
  const other = res.errors.filter((e) => e.code !== code);
  assert.equal(other.length, 0, `unexpected collateral errors: ${other.map((e) => e.code + '@' + e.path).join(', ')}`);
}

// fixture builders keyed to the code they should produce -----------------------
const FIX = {
  MISSING_ENTRYPOINT_FILE() { const reg = R(); const f = baseFiles(reg); f.delete('CLAUDE.md'); return { reg, f, d: baseDir(reg) }; },
  ENTRYPOINT_MARKER_MISSING() { const reg = R(); const f = baseFiles(reg); f.set('CLAUDE.md', 'This marker is obsolete and must be ignored: `' + reg.marker + '`'); return { reg, f, d: baseDir(reg) }; },
  MISSING_INDEX_FILE() { const reg = R(); const f = baseFiles(reg); f.delete(reg.current_index); /* keep dirMd index so role stays declared → isolates MISSING_INDEX_FILE */ return { reg, f, d: baseDir(reg) }; },
  INDEX_ROLE_MISSING() { const reg = R(); const f = baseFiles(reg); f.set(reg.current_index, 'no role here'); const d = baseDir(reg); d.set('CURRENT_AUTHORITY_INDEX.md', 'no role here'); return { reg, f, d }; },
  INDEX_ROLE_DUPLICATE() { const reg = R(); reg.docs.push({ path: 'docs/engines/focus_v5/EVID.md', role: 'EVIDENCE', status: 'CURRENT', superseded_by: null }); const f = baseFiles(reg); const d = baseDir(reg); d.set('EVID.md', blk(reg, reg.current_role_line)); return { reg, f, d }; },
  INDEX_ROLE_WRONG_FILE() { const reg = R(); reg.docs.push({ path: 'docs/engines/focus_v5/EVID.md', role: 'EVIDENCE', status: 'CURRENT', superseded_by: null }); const f = baseFiles(reg); f.set(reg.current_index, 'no role'); const d = baseDir(reg); d.set('CURRENT_AUTHORITY_INDEX.md', 'no role'); d.set('EVID.md', blk(reg, reg.current_role_line)); return { reg, f, d }; },
  LEGACY_CARRIES_MARKER() { const reg = R(); const f = baseFiles(reg); f.set('docs/engines/focus_v5/GATE_CATALOG_V5.md', blk(reg, reg.marker)); return { reg, f, d: baseDir(reg) }; },
  LEGACY_CARRIES_ROLE() { const reg = R(); const f = baseFiles(reg); f.set('docs/engines/focus_v5/GATE_CATALOG_V5.md', blk(reg, reg.current_role_line)); /* dirMd GATE_CATALOG left clean so index_role_unique (reads dirMd) sees no duplicate → isolates LEGACY_CARRIES_ROLE */ return { reg, f, d: baseDir(reg) }; },
  LEGACY_FORBIDDEN_VOCAB() { const reg = R(); const f = baseFiles(reg); f.set('docs/engines/focus_v5/GATE_CATALOG_V5.md', 'I AM THE SOLE AND CURRENT GATE AUTHORITY'); return { reg, f, d: baseDir(reg) }; },
  QUARANTINE_NEGATED() { const reg = R(); const f = baseFiles(reg); f.set(reg.current_index, blk(reg, reg.current_role_line) + '\nThese files are NOT dispositioned: GATE_CATALOG_V5.md and FOCUS_ENGINE_MASTER_PLAN_V5.md'); const d = baseDir(reg); d.set('CURRENT_AUTHORITY_INDEX.md', f.get(reg.current_index)); return { reg, f, d }; },
  QUARANTINE_SUCCESSOR_MISSING() { const reg = R(); reg.docs[1].superseded_by = null; const f = baseFiles(reg); return { reg, f, d: baseDir(reg) }; },
  SUPERSEDED_BY_TARGET_MISSING() { const reg = R(); reg.docs[1].superseded_by = 'docs/engines/focus_v5/DOES_NOT_EXIST.md'; const f = baseFiles(reg); return { reg, f, d: baseDir(reg) }; },
  UNREGISTERED_AUTHORITY_DOC() { const reg = R(); const f = baseFiles(reg); const d = baseDir(reg); d.set('SNEAKY_NEW.md', 'I AM THE SOLE AND CURRENT GATE AUTHORITY'); /* vocab (not role) so index_role_unique stays clean → isolates UNREGISTERED */ return { reg, f, d }; },
  DUPLICATE_REGISTRY_RECORD() { const reg = R(); reg.docs.push({ ...reg.docs[1] }); const f = baseFiles(reg); return { reg, f, d: baseDir(reg) }; },
  REGISTRY_HASH_MISMATCH() { const reg = R(); reg.docs[1].worktree_sha256 = 'deadbeef'.repeat(8); const f = baseFiles(reg); return { reg, f, d: baseDir(reg) }; },
  // Two LIVE authorities (an entrypoint and the current index) stating different values for the
  // CEO-owned field. The quarantined docs are left alone on purpose: their value is history.
  CEO_FIELD_DISAGREEMENT() {
    const reg = R();
    const f = baseFiles(reg);
    f.set('CLAUDE.md', blk(reg, reg.marker) + '\nEXECUTION_AUTHORIZATION = NOT_GRANTED');
    const idx = blk(reg, reg.current_role_line) + '\nEXECUTION_AUTHORIZATION = GRANTED (scope plan V5)';
    f.set(reg.current_index, idx);
    const d = baseDir(reg);
    d.set('CURRENT_AUTHORITY_INDEX.md', idx);
    return { reg, f, d };
  },
};
// which rule each code belongs to (for mutation adequacy)
const CODE_RULE = {
  MISSING_ENTRYPOINT_FILE: 'entrypoint_marker', ENTRYPOINT_MARKER_MISSING: 'entrypoint_marker',
  MISSING_INDEX_FILE: 'registry_wellformed', DUPLICATE_REGISTRY_RECORD: 'registry_wellformed',
  INDEX_ROLE_MISSING: 'index_role_unique', INDEX_ROLE_DUPLICATE: 'index_role_unique', INDEX_ROLE_WRONG_FILE: 'index_role_unique',
  LEGACY_CARRIES_MARKER: 'legacy_no_marker_no_role', LEGACY_CARRIES_ROLE: 'legacy_no_marker_no_role',
  LEGACY_FORBIDDEN_VOCAB: 'legacy_no_forbidden_vocab',
  QUARANTINE_NEGATED: 'quarantine_positive', QUARANTINE_SUCCESSOR_MISSING: 'quarantine_positive',
  SUPERSEDED_BY_TARGET_MISSING: 'registry_wellformed',
  UNREGISTERED_AUTHORITY_DOC: 'no_unregistered_authority_doc',
  REGISTRY_HASH_MISMATCH: 'hash_pin',
  CEO_FIELD_DISAGREEMENT: 'ceo_owned_field_agreement',
};

test('PASS: clean tree validates (LF)', () => {
  const reg = R();
  assert.equal(scan(baseFiles(reg), reg, baseDir(reg)).ok, true);
});
test('PASS: clean tree validates (CRLF)', () => {
  const reg = R();
  assert.equal(scan(baseFiles(reg, '\r\n'), reg, baseDir(reg, '\r\n')).ok, true);
});

for (const [code, build] of Object.entries(FIX)) {
  test(`FAIL one-defect: ${code}`, () => {
    const { reg, f, d } = build();
    const res = scan(f, reg, d);
    expectOnly(res, code);
    assert.ok(res.errors.find((e) => e.code === code).path, 'error must carry a path');
  });
}

test('CONTRAEJEMPLO (mandate §3): quoted-obsolete marker + prose-authority legacy + negated quarantine all FAIL', () => {
  const reg = R();
  const f = baseFiles(reg);
  f.set('CLAUDE.md', 'This marker is obsolete and must be ignored: `' + reg.marker + '`');
  f.set('docs/engines/focus_v5/ADR_REGISTER_V5.md', 'historical quote only:\n   ' + reg.marker);
  f.set('docs/engines/focus_v5/GATE_CATALOG_V5.md', 'I AM THE SOLE AND CURRENT GATE AUTHORITY');
  const idx = blk(reg, reg.current_role_line) + '\nThese files are NOT dispositioned: GATE_CATALOG_V5.md and FOCUS_ENGINE_MASTER_PLAN_V5.md';
  f.set(reg.current_index, idx);
  const d = baseDir(reg);
  d.set('CURRENT_AUTHORITY_INDEX.md', idx);
  d.set('GATE_CATALOG_V5.md', 'I AM THE SOLE AND CURRENT GATE AUTHORITY');
  const res = scan(f, reg, d);
  assert.equal(res.ok, false);
  const codes = new Set(res.errors.map((e) => e.code));
  assert.ok(codes.has('ENTRYPOINT_MARKER_MISSING'), 'obsolete/quoted marker must fail');
  assert.ok(codes.has('LEGACY_FORBIDDEN_VOCAB'), 'legacy authority vocabulary must fail');
  assert.ok(codes.has('QUARANTINE_NEGATED'), 'negated quarantine must fail');
  // both entrypoints stripped -> two ENTRYPOINT_MARKER_MISSING
  assert.equal(res.errors.filter((e) => e.code === 'ENTRYPOINT_MARKER_MISSING').length, 2);
});

test('RULE_ABLATION_ADEQUACY: disabling ANY rule makes a targeted fixture stop reporting (every rule load-bearing)', () => {
  const covered = new Set();
  for (const [code, build] of Object.entries(FIX)) {
    const rule = CODE_RULE[code];
    const { reg, f, d } = build();
    const withRule = scan(f, reg, d);
    const withoutRule = scan(f, reg, d, new Set([rule]));
    const hadCode = withRule.errors.some((e) => e.code === code);
    const stillHasCode = withoutRule.errors.some((e) => e.code === code);
    assert.ok(hadCode, `${code} should fire with the rule enabled`);
    assert.ok(!stillHasCode, `disabling rule "${rule}" must remove ${code} (proves it is load-bearing)`);
    covered.add(rule);
  }
  // every rule with a checkable code is exercised (registry_wellformed via MISSING_INDEX_FILE/DUPLICATE)
  for (const id of RULE_IDS) {
    assert.ok(covered.has(id), `rule "${id}" has no fixture proving it is load-bearing`);
  }
});
