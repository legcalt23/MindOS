# V2d / R3-A / R3-A5 / branch-registry — `PAUSED_REOPEN_KNOWN_RISKS`

**Authority:** CEO pivot directive, 2026-07-15. Supersedes R3-A5 §1C and every open R3-A cursor.
**This is not a close-out.** V2d is paused with its defects open and named. Nothing below is claimed fixed.

## Status

`PAUSED_REOPEN_KNOWN_RISKS` — no further work without a new CEO authorization.

All files, evidence and findings are preserved unmodified. The last verifiable baseline remains
`INPUT_SNAPSHOT_ID = 5cae6b907d067464b98b571c3f60df06260a9eee422b677ff768ecf346e414ca` (71 inputs, 0 drift),
which **no reviewer has ever approved**. Earlier snapshots `8dbd523d` / `1214818288` / `d7ecf71e` remain
`INVALIDATED_UNRECOVERABLE` (make-snapshot overwrote the single copy and archived nothing).

## Open risks carried forward — none of these are closed

| ID | Risk | Status at pause |
|---|---|---|
| F-01 | The numerator is unprotected: `record(checkId, ok, …)` launders FAIL→PASS in one line, all tests still green | **OPEN.** P8A-12 guards 1 of 12 checks |
| — | Publisher laundering: editing the `publish` payload leaves stdout/exit honest while the durable report lies | **OPEN — reproduced live** |
| — | Phantom contract `MB-V2D-R3-A2 §4` cited by 4 files, absent from the repo | **OPEN** |
| F-07 | `readReport` has zero production call sites — the consumer never runs | **OPEN** |
| F-08 | `currentSnapshotId()` copies the id out of the snapshot file instead of measuring the tree | **OPEN** |
| F-09 | P8-10 gates on an unbound `LOCK_SAFETY_RESULT.json` | **REACHABLE** |
| F-REG-01…12 | Registry: inert schema, duplicate legacy claims, coordinated-shrink false green, tautological E07/E08/E09, denominators summing 47≠56, covert authority in `build-branch-registry.mjs` | **ALL OPEN.** `R3A5_BRANCH_REGISTRY.json` = `SUPERSEDED_NO_APTO` |
| — | 50 enumerated branches, 22 `NOT_TESTED` | **OPEN** |

## The meta-failure, recorded so it is not repeated

Across R3-A I repeatedly fixed the *reproduction* instead of the *property*, and twice built the very defect
class I had just documented (F-REG-03 is P8-01.b4 reimplemented by me; E07/E08/E09 are new tautologies inside a
validator written to catch tautologies). The pivot is the correct response: this system was consuming the budget
that product needed, and it was not converging.

## Prohibition

Do not resume R3-A5. Do not "fix the counter". Do not rewrite the audit system. Findings above have owners and
gates; they are registered, not forgotten.
