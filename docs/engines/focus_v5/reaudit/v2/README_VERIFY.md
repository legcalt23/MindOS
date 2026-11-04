# README_VERIFY — how a third party verifies the S5·R3·V2 package

This package is **worktree bytes**, not a signed release. Its integrity is self-contained (manifest); its
**authenticity is NOT established** (`PROVENANCE_STATUS = FAIL_UNANCHORED`) until anchored in a signed commit/CI
artifact. Do not read "reproducible" as "authentic" — see the provenance ladder in the final CEO response / §5.

## 0. Prerequisites
- Node ≥ 18 (for the `.mjs` gates). No npm install needed (zero deps).
- For the Kotlin gates only: JDK 21 + Android SDK + Gradle wrapper (the repo's `./gradlew`). macOS + Xcode 16 for iOS (BLOCKED here).

## 1. Extract
The bundle `S5_R3_V2_BUNDLE.tar.gz` unpacks to the repo-relative paths in the manifest. Or verify in-place in a checkout of HEAD `8a06b9a` with the worktree V2 files applied.

## 2. Verify every hash
```bash
node docs/engines/focus_v5/reaudit/build_external_manifest.mjs   # rebuilds manifest; prints package_root_sha256
# compare each member sha256 in S5_R3_EXTERNAL_MANIFEST.md to your own:
#   node -e 'const c=require("crypto");console.log(c.createHash("sha256").update(require("fs").readFileSync(process.argv[1])).digest("hex"))' <path>
grep -v '^Canonical SHA-256' MINDBOOST_AUDIT_LEDGER.md | tr -d '\r' | sha256sum   # must equal the §0 Canonical SHA-256
```
EOL note: hashes are over worktree bytes. Under a different `core.autocrlf`, CRLF/MIXED members shift; the manifest reports per-member EOL to reconcile. The ledger is pinned `eol=lf` (`.gitattributes`).

## 3. Run the Node gates (all must exit 0)
```bash
node --test scripts/validate-authority-entrypoints.test.mjs          # 19/19
node scripts/validate-authority-entrypoints.mjs                       # AUTHORITY CHAIN OK
node scripts/authority-scanner.mutation.mjs                           # 9/10 killed (1 equivalent survivor) → exit 1 by design; inspect JSON
node docs/engines/focus_v5/reaudit/v2/validate_cross_artifact.mjs     # CROSS-ARTIFACT (BIDIRECTIONAL) OK
node scripts/validate-audit-ledger.mjs                                # LEDGER OK — hash matches
node docs/engines/focus_v5/reaudit/r2_falsification_harness.mjs       # self_digest_sha256=2df300f3…
```
(The mutation runner exits 1 because 1 mutant survives; that survivor is EQUIVALENT — see `S5_R3_ROOT_OF_TRUST_AUDIT.md`. Correctness-mutant score = 9/9.)

## 4. Gates that require Gradle / macOS (NOT runnable with Node alone)
| gate | requires | this session |
|---|---|---|
| golden production-scorer parity (`DescriptiveScorerGoldenReauditTest`) | JDK21+Gradle | **RAN, 2/2 green** (isolated worktree) — evidence: `logs/gradle_TEST-*DescriptiveScorerGoldenReauditTest.xml` |
| R3 contract tests (`FocusTemporalEligibilityTest`, `FocusEvidenceStatePolicyTest`) | JDK21+Gradle + live WIP overlay | **RAN, 16/16 green** — `logs/gradle_TEST-*Focus{TemporalEligibility,EvidenceStatePolicy}Test.xml` |
| Android compile/unit | JDK21+Android SDK | NOT_RUN |
| iOS compile/sim/device | macOS+Xcode 16 | BLOCKED |
| physical device matrix | real devices | NOT_RUN |

To reproduce the Kotlin gates: `git worktree add --detach <tmp> 8a06b9a`, copy the golden test in, add `local.properties` with `sdk.dir`, then `./gradlew :shared:desktopTest --tests …DescriptiveScorerGoldenReauditTest --no-daemon --offline --project-cache-dir <tmp>/.gpc`. Full method: `S5_R3_KOTLIN_EXECUTION_REPORT.md`.

## 5. Compare PRE / POST
- PRE snapshot: `reaudit/v2/phase0_inventory.json` (worktree SHAs at Phase 0; 3 tracked, 26 untracked; pre-V1 authority-doc bytes = `PREIMAGE_IRRECOVERABLE`).
- POST closure: `reaudit/S5_R3_EXTERNAL_MANIFEST.json` (`package_root_sha256`).
- Bidirectional coherence: `validate_cross_artifact.mjs` asserts no PRE INCLUDE item was dropped from the manifest and every manifest member exists.

## 6. Reconstruct the package root
`package_root_sha256 = sha256( utf8( sort_asc( "<relpath>\t<member_sha256>\t<bytes>" for each PRESENT member ).join("\n") ) )`. Deterministic, order-independent. Rebuild with the command in §2; it must reprint the same root recorded in the manifest and in the final CEO response.
