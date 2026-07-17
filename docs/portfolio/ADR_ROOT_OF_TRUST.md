# ADR — Root of trust for the V2d portfolio bundle

**Status:** Accepted (R3-A3) · **Scope:** V2d audit evidence package only. No production code.

## Context

Phase 8 claims a bundle reproduces itself from its own bytes. That claim is worth nothing unless something outside
the bundle anchors it. A bundle that vouches for itself is a signature written by the forger.

## Decision

The root of trust is **`BUNDLE_CONTAINER_HASH.txt`, published outside the container it describes.**

Everything else chains from it:

```
BUNDLE_CONTAINER_HASH.txt        ← THE ROOT. Outside the ZIP. Not covered by anything inside it.
  └── candidate ZIP (sha256 must equal the root)
        └── BUNDLE_MANIFEST.json   ← self-excluded (a file cannot carry its own digest)
              ├── BUNDLE_SHA256SUMS.txt   ← IS hashed by the manifest
              │     └── every artifact (path → sha256)
              └── every artifact (path → sha256, byteLength, role, regular-file)
```

### Why the manifest is the only self-exclusion

A file cannot contain its own digest — that is arithmetic, not policy. So exactly one file is unhashed from
within, and its integrity comes from the container hash above it.

`BUNDLE_SHA256SUMS.txt` is **not** in that category, and treating it as if it were was a real defect (fixed in
R2). The reasoning that excused it — "it cannot hash itself" — is true of its own list and irrelevant to the
manifest's. `BUNDLE_SHA256SUMS.txt` references neither itself nor the manifest, so hashing it from the manifest
closes no cycle. It is hashed. The two-pass build exists for exactly this: sums first, then the manifest hashes
the sums.

### Integrity vs authenticity — stated, not blurred

| Property | Provided? | By what |
|---|---|---|
| **Integrity** — "these bytes are the bytes that were audited" | **YES** | container hash → manifest → sums → artifacts |
| **Authenticity** — "these bytes came from MindOS and no one else" | **NO** | would need a signature over the container hash |

The container hash is a **digest, not a signature**. It proves nothing about origin. An attacker who can rewrite
both the ZIP and `BUNDLE_CONTAINER_HASH.txt` defeats the whole chain, and nothing here detects it.

### Threat model

| Threat | Covered |
|---|---|
| Artifact silently edited in the bundle | YES — manifest + sums recompute from disk |
| Artifact added to the bundle | YES — closed-set equality (`actual − listed` AND `listed − actual`) |
| Artifact removed from the bundle | YES — same, `missing` direction (the VM-02 fix) |
| Manifest forged to match a tampered artifact | YES — container hash covers the manifest |
| ZIP swapped wholesale | YES — **only if** the verifier holds the expected container hash from a trusted channel |
| ZIP **and** container-hash file both rewritten | **NO** — needs a signature. Out of scope; declared, not hidden |
| Malicious archive entry (traversal, link, ADS, hardlink) | YES — build- and read-side checks (P5-08a…f) |

### Where the expected hash comes from

`BUNDLE_CONTAINER_HASH.txt` is trusted **only** when it reaches the verifier out-of-band. Read from beside the
ZIP it proves the ZIP matches a hash the same actor wrote — a tautology. Phase 8 P8-04 therefore verifies the
container hash file is **absent from the ZIP** (`CONTAINER_HASH_SELF_REFERENTIAL`) but cannot verify the channel;
that is a human step and is stated as such.

## Schema compatibility

| Version | Status | Rule |
|---|---|---|
| **1.x** | Rejected | Its manifest excluded `SHA256SUMS.txt` from `artifacts[]`. Under 2.0.0 that same manifest is INVALID — the file must be hashed. Accepting a 1.x manifest would reinstate the exact gap R2 closed. |
| **2.0.0** | Current | `BUNDLE_SHA256SUMS.txt` in `artifacts[]`; `BUNDLE_MANIFEST.json` the only self-exclusion. |

MAJOR bump, not minor: the same bytes change validity verdict. That is a semantic break by definition.

**No migration path, deliberately.** A 1.x bundle is re-built, never up-converted — up-converting would mean
trusting a manifest produced under the rules being replaced.

**Rollback:** re-run `build-bundle.mjs` at the older commit. Bundles are reproducible artifacts, not state.

## V2c history

The V2c handoff ZIP is pinned by a constant and checked every run (P8-02). It is not governed by this ADR: it is
a historical artifact whose only job is to stay byte-identical. It earned that status by recovering the poisoned
CSV (MB-PORTFOLIO-0018) — it is evidence, and evidence that can be edited is not evidence.

## Validator pinning

The bundle validator is executed **from `inputRoot`** (P8-06, P8-10) — the bundled copy, not the repo's. A repo
validator could pass a bundle the bundle's own validator would reject, and the shipped copy is what a third party
would run. Both are invoked with an **explicit `--mode=bundle`**; no default exists (see the call-site inventory).

## Consequences

- A verifier needs exactly one thing through a trusted channel: the container hash.
- Authenticity is unsolved and openly so. Signing is the follow-up, and it is not claimed today.
- 1.x bundles are refused rather than migrated; the cost is a rebuild, the benefit is no legacy trust path.
