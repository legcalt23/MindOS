#!/usr/bin/env node
// Reproducible JUnit-XML aggregator. Derives tests/failures/errors/skipped from PRIMARY XML bytes.
// Usage: node scripts/parse-junit-xml.mjs <dir-or-file> [<dir-or-file>...]
// Exit 0 always (reporting tool); prints per-suite rows + totals as JSON to stdout.
import { readFileSync, readdirSync, statSync, existsSync } from 'node:fs'
import { createHash } from 'node:crypto'
import { join } from 'node:path'

const targets = process.argv.slice(2)
if (!targets.length) { process.stderr.write('usage: parse-junit-xml.mjs <path...>\n'); process.exit(2) }
const files = []
for (const t of targets) {
  if (!existsSync(t)) continue
  if (statSync(t).isDirectory()) for (const f of readdirSync(t)) { if (/^TEST-.*\.xml$/.test(f)) files.push(join(t, f)) }
  else files.push(t)
}
files.sort()
let T = 0, F = 0, E = 0, S = 0
const suites = []
for (const f of files) {
  const x = readFileSync(f, 'utf8')
  const m = x.match(/<testsuite\b[^>]*\btests="(\d+)"[^>]*\bskipped="(\d+)"[^>]*\bfailures="(\d+)"[^>]*\berrors="(\d+)"/)
  if (!m) { suites.push({ file: f, error: 'UNPARSED' }); continue }
  const nm = (x.match(/<testsuite\b[^>]*\bname="([^"]*)"/) || [, '?'])[1]
  T += +m[1]; S += +m[2]; F += +m[3]; E += +m[4]
  suites.push({ name: nm, tests: +m[1], skipped: +m[2], failures: +m[3], errors: +m[4],
    xml_sha256: createHash('sha256').update(readFileSync(f)).digest('hex') })
}
process.stdout.write(JSON.stringify({
  parser: 'parse-junit-xml', xml_count: files.length,
  totals: { tests: T, failures: F, errors: E, skipped: S }, suites,
}, null, 2) + '\n')
process.stderr.write(`XML=${files.length} tests=${T} failures=${F} errors=${E} skipped=${S}\n`)
