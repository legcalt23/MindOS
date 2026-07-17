# ADR — Threat model for the Phase 8 freeze oracle

**Status:** Accepted (R3-A4) · **Scope:** V2d audit evidence only. No production code. · **Supersedes:** nothing.

## Why this ADR exists

Three reviews used the word *attacker* without ever saying what the attacker may touch. That vagueness is not
cosmetic — it is the reason F-01 kept being "fixed" and kept reappearing. Every fix implicitly assumed a different
adversary:

- P8A-12 assumed an adversary who edits `record()`'s **body** (so a literal string-match on that body sufficed).
- The reviewer then forced `ok=true` at the **call site**, leaving `record()` untouched — a different adversary,
  and the fix was blind to it.

An oracle can only be sound relative to a stated adversary. So we state three, and we say for each exactly what is
mutable and what is not.

## The three categories

### A — Accidental regression

**Mutable:** the runner (`phase8-freeze.mjs`), the registry (`phase8-checks.mjs`), the aggregator, the publisher.
**Immutable:** the external contract, the oracle, the fixtures — because nobody is *trying* to defeat them.

**Defence:** any discriminating test. This is the easy case and was already handled before R3-A4.

**What it looks like:** a refactor moves a predicate and silently inverts it; a `&&` becomes `||`; a check is
renamed and the aggregator stops finding it.

### B — Adversarial mutation of the audited component  ← **the category F-01 is about**

**Mutable:** *everything the audit describes* — `phase8-freeze.mjs`, `phase8-checks.mjs`, `EXPECTED_CHECK_COUNT`,
the predicates, `record()`, the `ok` argument at each call site, the aggregation, the publisher, the report schema.
**Immutable:** the external contract authority, the oracle that reads it, and the oracle's fixtures.

**Defence:** the oracle must **re-derive every protected property from the artifacts itself** and compare against a
contract it does **not** import from the audited modules. It must never accept the runner's own account of what
happened.

**This is the only category in which "the numerator is unprotected" is a meaningful sentence,** and it is the
category R3-A4 must close.

Concretely, an adversary in category B may:

| May | May not |
|---|---|
| edit any predicate, any `ok`, `record()`, the aggregator, the publisher | edit `phase8-contract.mjs` (the authority) |
| delete a check, renumber it, rename it | edit the oracle or its fixtures |
| change `EXPECTED_CHECK_COUNT` and every constant that agrees with it | change the artifacts the oracle corrupts |
| make the report claim anything at all | forge the oracle's own out-of-band evidence |

### C — Malicious author with write access to runner *and* tests

**Mutable:** everything in the repository, including the oracle and this ADR.

**Defence:** **none from inside the repository — and saying otherwise would be the same lie F-01 is made of.**
A test in the same tree as the code it polices cannot be an absolute root of trust: the author edits both.

What actually defends category C is *outside* the code:
1. the **input snapshot** (an id over bytes, cited by three independent reviewers);
2. **independent human/agent review** of the diff;
3. an **external authority** (the container hash delivered out-of-band, per `ADR_ROOT_OF_TRUST.md`).

This ADR does **not** claim category C is defended. It claims category C is defended *procedurally*, not
*mechanically*, and that the distinction is stated rather than blurred.

## Consequence for the oracle's design

Category B forces three properties, and they are the whole of R3-A4:

1. **The contract is a separate authority.** `phase8-contract.mjs` declares the twelve ids, each protected
   property, each branch, and each fixture — and imports **nothing** from `phase8-checks.mjs` or
   `phase8-freeze.mjs`. If the runner and the oracle derive their target from the same module, F-01 is open by
   construction, regardless of how many tests pass.

2. **The oracle re-derives, never reads.** For each branch the oracle (a) damages the artifact in a disposable
   copy, (b) proves out-of-band that the damage landed, (c) runs the **real** runner, (d) requires that specific
   `checkId` to be FAIL and the freeze to refuse. Step (b) is what makes it an oracle rather than a mirror: the
   evidence that the property is false does not come from the thing being asked about the property.

3. **State must not be a boolean.** A boolean `ok` is a single point of laundering: one edit and every branch of
   every check collapses to PASS. Twelve tests cannot fix that; a structural change can. State must be *derived*
   from structured violations, so that `state: PASS` alongside `detail: "artifact(s) drifted"` — an observed,
   published contradiction — becomes representationally impossible rather than merely untested.

## Explicit non-claims

- **We do not claim category C is defended.**
- **We do not claim power-loss durability.** `fsync` on the file is done; the containing directory is not synced,
  and Windows/NTFS semantics for directory sync differ from POSIX. What is demonstrated is atomicity against
  **process termination**, which is what the fixtures actually exercise. Claiming more would be exactly the
  overclaim this audit exists to prevent.
- **We do not claim the oracle is complete.** Branches with no isolating falsifier are declared `NOT_TESTED` in
  the matrix, not silently folded into a neighbouring branch's coverage.
