# Memory Ownership Manifest — one fact, one owner

> Registry for **ADR-MEM-001**. Each durable fact category has exactly ONE `canonical_owner`.
> Every other doc may **reference** the owner (link / ID) but must **not restate** the fact in
> independent prose. Enforced (partially) by `scripts/memoryctl.mjs`. Status: PROPOSAL — the
> irreversible consolidation (Auto-Memory off, Memory-Layer demotion) is BLOCKED pending CEO.

| fact_category | canonical_owner | allowed_references | forbidden_duplicates | update_trigger | authority |
|---|---|---|---|---|---|
| Product requirements / pricing | `docs/context_pack/01_MINDBOOST_PRODUCT_PRD.md` | ledger, work items, ADRs | Auto-Memory `project_*`, CLAUDE.md prose | product decision | CEO / PRD |
| Scientific measurement / claims | `docs/context_pack/03_SCIENTIFIC_MEASUREMENT_CHARTER.md` | PRD, exercise dossiers | restated claims in code comments/docs | science ruling | Charter |
| Exercise quality bar | `docs/context_pack/04_EXERCISE_QUALITY_STANDARD.md` | dossiers, QA gates | ad-hoc "ready" criteria | standard revision | Charter |
| Decisions / rationale | `docs/context_pack/09_DECISION_LOG.md` + `docs/**/adr/` | ledger, work items | Auto-Memory decisions, README prose | decision made | Decision Log / ADR |
| Technical current state | `docs/context_pack/05_TECHNICAL_CURRENT_STATE.md` | Graphify (for "where") | Auto-Memory status, parity `*_AUDIT.md` | verified state change | Tech state doc |
| Findings / risks / audit continuity | `MINDBOOST_AUDIT_LEDGER.md` | reports link in | second findings list | any task (mandatory) | Ledger (single writer) |
| Exercise-catalog program state | `MINDBOOST_121_EXERCISE_PORTFOLIO.md` | ledger `MB-CATALOG-*` | tone/file-count inferences | portfolio audit | Portfolio + ledger |
| Code structure (who calls what) | **code + Graphify** (never a hand note) | ADR/tech-state (link) | hand-maintained call maps in Markdown | code change → `graphify update` | code |
| Per-exercise status (of 120) | per-exercise dossier / canonical record | global generated view | Auto-Memory `project_*` claims | verified milestone | dossier |
| Claude operating rules | `CLAUDE.md` + `.claude/rules` + `.claude/skills` | — | Auto-Memory duplications | rule change | project |
| Personal working prefs | Auto Memory `feedback_*` (ONLY) | — | project facts of any kind | user feedback | user |

## Epistemic labels (ledger-aligned)
`CEO_APPROVED` · `REPO_VERIFIED` · `TEST_VERIFIED` · `RUNTIME_VERIFIED` · `CLAIMED_BY_CLAUDE` ·
`INFERRED` · `PROVISIONAL` · `TARGET` · `UNKNOWN` · `BLOCKED` · `CONTRADICTED` · `SUPERSEDED`.
Never elevate a label without evidence. A recent file mtime is **not** proof of conceptual currency.

## Known ownership items (open and resolved — see ledger)
- `MB-MEMORY-0001` — OPEN — Auto Memory `project_*` holds status/decisions (should reference owners).
- `MB-MEMORY-0002` — OPEN — Graphify indexes prose authorities (should own code structure only).
- `MB-ENG-0006` — RESOLVED 2026-07-17: docs/context_pack (underscore, packRelease 2.0.2) is the sole git-tracked live canon; docs/context-pack is SUPERSEDED and remains only in quarantine.
- `MB-MEMORY-0004` — SUPERSEDED → `MB-ENG-0006`: earlier (2026-07-14) record of the same context-pack duplication; folded into `MB-ENG-0006` and resolved by the same single-git-tracked-canon evidence (see ledger §6).
