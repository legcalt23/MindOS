# MindBoost V2d — clean-room verification runbook

Verify this bundle **without the original repository**. If any step needs a file that is not in the extract,
that is a finding, not a workaround: do not fetch it from elsewhere.

Requires Node 20+ and a SHA-256 tool. No network. No other checkout.

## 0. Verify the container before opening it

The container hash is published **outside** the container (a ZIP cannot contain its own hash). Compare the
value you were given out-of-band against the file you received:

```sh
sha256sum MINDBOOST_V2D_CANDIDATE_BUNDLE.zip
```

Stop if it disagrees with `BUNDLE_CONTAINER_HASH.txt` as transmitted separately.

## 1. Extract into an EMPTY directory

```sh
mkdir mindboost-verify && cd mindboost-verify
tar -xf ../MINDBOOST_V2D_CANDIDATE_BUNDLE.zip
ls
```

Everything below runs from this directory. Nothing outside it may be read.

## 2. Validate the bundle manifest — recomputed, not believed

`BUNDLE_MANIFEST.json` is not evidence of itself. This re-reads every byte from disk and checks existence,
regular-file, symlink, size, SHA-256, role, SHA256SUMS membership, duplicates, case collisions and extra files.

```sh
node scripts/portfolio/validate-bundle.mjs .
```

Expect `BUNDLE GATES: PASS`.

## 3. Verify every hash independently of the manifest

```sh
sha256sum -c SHA256SUMS.txt
```

## 4. Extract the source archive and verify all 500 entries

The archive carries the bytes the source manifest hashes. Its format (`MBSRC1`) deliberately omits mtime,
permissions and compression, so it is byte-identical wherever it is built.

```sh
node -e "const{extractArchive}=await import('./scripts/portfolio/build-source-archive.mjs');const fs=await import('fs');extractArchive(fs.readFileSync('PORTFOLIO_SOURCE_ARCHIVE.mbsrc'),'./sources');" --input-type=module
node -e "const fs=await import('fs'),p=await import('path'),c=await import('crypto');const i=JSON.parse(fs.readFileSync('PORTFOLIO_SOURCE_INDEX.json','utf8'));let ok=0,bad=[];for(const e of i.entries){const d=fs.readFileSync(p.join('./sources',e.path));c.createHash('sha256').update(d).digest('hex')===e.sha256?ok++:bad.push(e.path);}console.log('verified',ok,'of',i.entries.length,'| mismatched:',bad.length);" --input-type=module
```

Expect `verified 500 of 500 | mismatched: 0`. Extraction refuses any path that escapes `./sources`.

## 5. Regenerate the observations, the model and the published view

Point the tools at the extracted sources and confirm they reproduce what was shipped.

```sh
node scripts/portfolio/observe-code.mjs
node scripts/portfolio/derive-portfolio-model.mjs
node scripts/portfolio/build-portfolio.mjs
```

## 6. Run the validators

```sh
node scripts/portfolio/validate-portfolio.mjs
node scripts/portfolio/validate-evidence.mjs
node scripts/validate-audit-ledger.mjs
```

Expect `BLOCKING GATES: PASS`, `EVIDENCE VALIDATION: PASS`, `LEDGER OK`.

The completeness gates are **red by design** — they measure the real gap, not a defect:
`SLOT_ASSIGNMENT_COMPLETENESS_GATE` (37 slots unassigned), `STATIC_COMPONENTS_COMPLETENESS_GATE`
(56 unresolved), `EXERCISE_ROUTE_COMPLETENESS_GATE` (0/121 routed), `TARGET_LAUNCH_READINESS_GATE`
(TRAINING_READY 0/121).

## 7. Run the mutation oracles — a gate that cannot go red is decoration

```sh
node scripts/portfolio/authority-tests.mjs
node scripts/portfolio/evidence-mutation-tests.mjs
node scripts/portfolio/phase4-mutation-tests.mjs
node scripts/portfolio/phase5-mutation-tests.mjs
```

Each mutation must change its target, fail the expected gate with the expected diagnostic, and restore bytes.

## 8. Compare against the shipped artifacts

```sh
sha256sum PORTFOLIO_DERIVED_MODEL.json MINDBOOST_EXERCISE_PORTFOLIO.md PORTFOLIO_SOURCE_ARCHIVE.mbsrc
```

## What the numbers mean

| Metric | Value | Meaning |
|---|---|---|
| Static components verified | 9/121 | declaration + mechanic + generator each bound to a real construct in hashed source. **NOT** reachable, playable, tested or ready. |
| Static components unresolved | 56/121 | the strategy cannot resolve that architecture. **Not** evidence of absence. |
| Exercise route verified | 0/121 | no per-exercise dispatch proof exists. Block reachability does not substitute. |
| Runtime verified | 0/121 | no run exists. Absence of evidence, not failure. |
| TRAINING_READY | 0/121 | derived; any UNRESOLVED / NOT_RUN / UNKNOWN blocks it. |

`PORTFOLIO_V2C_LEGACY_CLAIMS.csv` is `HISTORICAL_UNVERIFIED_CLAIMS`. It is the provenance of the retired
"43 live / 44 mechanic" numbers — preserved so they can be traced, not believed. No active tool parses it.
