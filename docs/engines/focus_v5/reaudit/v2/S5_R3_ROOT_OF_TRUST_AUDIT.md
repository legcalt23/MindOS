# S5_R3_ROOT_OF_TRUST_AUDIT — authority scanner + registry (Part 6)

```
ARTIFACTS: scripts/validate-authority-entrypoints.mjs · .test.mjs · authority-scanner.mutation.mjs ·
           docs/engines/focus_v5/authority-registry.json
GUARANTEE NAMING (honest): SENTINEL_SCOPED_REGISTRY_CONSISTENCY + KEYWORD_AUTHORITY_HEURISTIC.
                           NOT "complete semantic authority detection". It is a keyword + structural validator.
```

## Adequacy evidence (executed)
- `node --test scripts/validate-authority-entrypoints.test.mjs` → **19/19 pass** (0 fail); PASS(LF)+PASS(CRLF),
  13 one-defect fixtures with NO collateral, the mandate's 4-part contraejemplo FAILS, RULE_ABLATION_ADEQUACY.
- **RULE_ABLATION_ADEQUACY = PASS** — disabling each of the 8 rules removes its code (every rule load-bearing).
  This is rule ablation (whole-rule disable), correctly NOT labeled MUTATION_ADEQUATE.
- **REAL MUTATION TESTING** (`authority-scanner.mutation.mjs`): 10 operator-level mutants, **9 KILLED, 1 SURVIVED,
  score 0.90**. Operators: negation-removed (×2), `&&`→`||`, `===`→`!==`, threshold `>1`→`>2`, successor-check
  removed, `some`→`every`, `break` removed, early-return, dupe-check removed. The lone survivor
  `break_removed_vocab` is **EQUIVALENT**: the removed `break` is a per-doc dedup optimization inside
  `legacy_no_forbidden_vocab`; without it the rule still flags the offending doc (only emits duplicate same-code
  errors) → it cannot cause a false PASS. Effective correctness-mutant score = **9/9**.

## The 14 questions — answered honestly (with limitations)

1. **How is the registry discovered?** `scanRepo()` reads a **hardcoded path** `docs/engines/focus_v5/authority-registry.json`. One fixed location; not discovered by search.
2. **Who validates exactly one exists?** *Nothing.* The hardcoded path means structurally one is used, but **no check forbids a second registry** elsewhere. **GAP-RT-1** (follow-up).
3. **Can registry+index be co-edited and keep PASS?** **Yes.** The scanner validates *internal consistency*, not external truth: a consistent co-rewrite of registry + index passes. This is precisely why provenance is `FAIL_UNANCHORED` — only an **external anchor** (signed commit / CI) defeats silent co-rewrite. `hash_pin` would catch drift of a *pinned* doc, but the registry currently pins no `worktree_sha256` (opt-in), so it is not active. **GAP-RT-2**.
4. **Is the registry in inventory, manifest, anchor?** Manifest: **yes** (member `authority-registry.json`, full-SHA). Inventory: added as a POST member. Anchor: **no** (`FAIL_UNANCHORED`).
5. **Circularity registry↔manifest↔index?** **Yes, acknowledged:** the manifest hashes the registry; the registry names the index; the index is a manifest member. This proves *mutual consistency*, not authenticity. The break in the loop must be external (commit/CI). Documented, not hidden.
6. **EOL/encoding/BOM/symlinks canonicalization?** Block parsing splits on `/\r?\n/` (EOL-agnostic) and trims lines, so LF/CRLF markers both match (tested: PASS-CRLF). `hash_pin` and the manifest hash **raw bytes** (EOL-sensitive; manifest reports per-member EOL for reconciliation). **BOM is NOT stripped** — a BOM-prefixed marker line would fail the trimmed exact-match → **GAP-RT-3**. Symlinks: manifest asserts none in the package; the scanner uses `readFileSync` (would follow one) — not defended. **GAP-RT-4**.
7. **How is a new authority-like doc detected?** `no_unregistered_authority_doc` scans every `.md` in `authority_dir`; flags any unregistered file that **declares the CURRENT role inside a sentinel block** OR **matches a forbidden-vocab regex**. Merely carrying the marker (pointer) is NOT a claim.
8. **What vocabulary/languages?** `forbidden_legacy_vocab` = 4 case-insensitive regexes: EN (`SOLE AND CURRENT GATE AUTHORITY`, `I AM THE SOLE`, `SOLE…AUTHORITY`) + ES (`AUTORIDAD ÚNICA…VIGENTE`). Small, explicit set.
9. **What can it NOT detect?** (a) authority claims in **unlisted phrasing or other languages**; (b) a **second registry**; (c) **BOM-prefixed** markers; (d) authority docs **outside** `authority_dir`; (e) **content truth** — only internal consistency; (f) registry↔code drift for **unpinned** docs.
10. **Can a legacy self-declare authority with unlisted words?** **YES** — if it avoids the marker/role sentinel block and the 4 vocab regexes, it is not flagged. This is the KEYWORD_AUTHORITY_HEURISTIC ceiling; no semantic understanding is claimed.
11. **How is a historical citation distinguished from a declaration?** By the **sentinel block**: a marker/role counts ONLY between `<!-- FOCUS_V5_AUTHORITY_BLOCK_START -->` / `…END -->` as a trimmed exact line. A quoted "obsolete" marker in prose (outside the block) does not count — this defeats the mandate's contraejemplo (tested).
12. **How are two competing sentinels prevented?** `index_role_unique`: exactly one file in `authority_dir` may carry the CURRENT-role line inside a block; 0→`INDEX_ROLE_MISSING`, ≥2→`INDEX_ROLE_DUPLICATE`, wrong file→`INDEX_ROLE_WRONG_FILE`. (The *marker* pointer may legitimately appear in multiple entrypoints — that is expected, not a competition.)
13. **How is `superseded_by` checked to exist and be current?** `registry_wellformed`: `superseded_by` must be a **registered doc path or the current_index**, else `SUPERSEDED_BY_TARGET_MISSING`. **Limitation:** it does not verify the successor's *status* is CURRENT (a successor that is itself QUARANTINED would pass). **GAP-RT-5**.
14. **What prevents the successor pointing back to the legacy?** **Nothing explicit** — no cycle detection (A→B, B→A both registered would pass). **GAP-RT-6** (follow-up: add acyclicity + successor-must-be-CURRENT).

## Verdict
The scanner is a **strong structural + sentinel-scoped keyword validator** that reliably defeats the specific V1
failure modes (quoted-obsolete marker, prose authority claim, negated quarantine, listed authority vocabulary),
proven by 19/19 tests + 9/9 effective mutation kills. It is **NOT** a semantic authority detector and does **NOT**
establish external authenticity (that is `FAIL_UNANCHORED`, F-06). Open follow-ups GAP-RT-1…6 above are hygiene,
not launch blockers, and are tracked for the S5-close re-seal.
