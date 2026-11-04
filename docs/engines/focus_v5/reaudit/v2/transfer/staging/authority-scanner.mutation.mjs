#!/usr/bin/env node
// REAL mutation testing for validate-authority-entrypoints.mjs (Part 6 of the S5 R3 V2 completion).
//
// Distinct from the in-suite RULE_ABLATION_ADEQUACY test (which disables whole rules): this applies
// OPERATOR-LEVEL mutants to the scanner SOURCE and checks the node:test suite KILLS each one.
// A mutant is KILLED if the suite fails (exit != 0), SURVIVED if it still passes (exit 0),
// SKIPPED if its pattern is not present (not applicable), and flagged EQUIVALENT only by manual note.
//
// Run from repo root: node scripts/authority-scanner.mutation.mjs
import { readFileSync, writeFileSync, mkdirSync, rmSync, cpSync } from 'node:fs'
import { execFileSync } from 'node:child_process'
import { join } from 'node:path'

const SCANNER = 'scripts/validate-authority-entrypoints.mjs'
const TEST = 'scripts/validate-authority-entrypoints.test.mjs'
const src = readFileSync(SCANNER, 'utf8')
const testSrc = readFileSync(TEST, 'utf8')
const TMP = join(process.env.TEMP || '/tmp', 'authscan-mut')

// operator -> [find, replace] applied to the FIRST occurrence. Each is a real semantic change.
const MUTANTS = [
  ['negation_removed_marker', 'if (!declaresMarker(c, reg))', 'if (declaresMarker(c, reg))'],
  ['negation_removed_missing_index', 'if (!files.has(reg.current_index))', 'if (files.has(reg.current_index))'],
  ['and_to_or_superseded', 'd.superseded_by && !reg.docs.some', 'd.superseded_by || !reg.docs.some'],
  ['eq_flip_index_wrong_file', 'claimers[0] !== idxBase', 'claimers[0] === idxBase'],
  ['threshold_duplicate_weakened', 'claimers.length > 1', 'claimers.length > 2'],
  ['successor_check_removed', 'if (!d.superseded_by)', 'if (false)'],
  ['some_to_every_vocab', 'reg.forbidden_legacy_vocab.some', 'reg.forbidden_legacy_vocab.every'],
  ['break_removed_vocab', "break; }\n        }\n      }\n      return e;\n    },\n  },\n  {\n    id: 'quarantine_positive'", "; }\n        }\n      }\n      return e;\n    },\n  },\n  {\n    id: 'quarantine_positive'"],
  ['early_return_hashpin', "run({ reg, files }) {\n      const e = [];\n      for (const d of reg.docs) {\n        if (!d.worktree_sha256) continue;", "run({ reg, files }) {\n      const e = [];\n      return e;\n      for (const d of reg.docs) {\n        if (!d.worktree_sha256) continue;"],
  ['dupe_check_removed', "if (seen.has(d.path)) e.push({ code: 'DUPLICATE_REGISTRY_RECORD'", "if (false) e.push({ code: 'DUPLICATE_REGISTRY_RECORD'"],
]

rmSync(TMP, { recursive: true, force: true })
mkdirSync(TMP, { recursive: true })
writeFileSync(join(TMP, 'validate-authority-entrypoints.test.mjs'), testSrc)

const results = []
for (const [name, find, replace] of MUTANTS) {
  if (!src.includes(find)) { results.push({ name, status: 'SKIPPED', reason: 'pattern absent' }); continue }
  const mutated = src.replace(find, replace)
  writeFileSync(join(TMP, 'validate-authority-entrypoints.mjs'), mutated)
  let killed
  try {
    execFileSync(process.execPath, ['--test', join(TMP, 'validate-authority-entrypoints.test.mjs')], { stdio: 'pipe' })
    killed = false // suite passed => mutant SURVIVED
  } catch { killed = true } // suite failed => mutant KILLED
  results.push({ name, status: killed ? 'KILLED' : 'SURVIVED' })
}
rmSync(TMP, { recursive: true, force: true })

const applied = results.filter((r) => r.status !== 'SKIPPED')
const killed = applied.filter((r) => r.status === 'KILLED').length
const survived = applied.filter((r) => r.status === 'SURVIVED')
const score = applied.length ? (killed / applied.length) : 0
const out = {
  tool: 'authority-scanner.mutation',
  operators: MUTANTS.map((m) => m[0]),
  total_defined: MUTANTS.length,
  applied: applied.length,
  skipped: results.filter((r) => r.status === 'SKIPPED').length,
  killed,
  survived: survived.length,
  equivalent: 0,
  timeout: 0,
  mutation_score: +score.toFixed(4),
  survivors: survived.map((r) => r.name),
  detail: results,
}
process.stdout.write(JSON.stringify(out, null, 2) + '\n')
process.exit(survived.length === 0 ? 0 : 1)
