# MEMORY_ROUTER — inject this at SessionStart (keep it small)

> The one memory doc a session needs up front. Classify the task, hit the FIRST source, verify in code.
> Full architecture: [[../MEMORY_ARCHITECTURE_BASELINE_AND_ADR_2026-07-14|ADR-MEM-001]] · owners: [[../MEMORY_OWNERSHIP|manifest]].

## Two authorities — never one for everything
- **GRAPHIFY = structure of the code** (who calls what, blast radius). Never decisions/status/rationale.
- **OBSIDIAN over the repo Markdown = the canon** (what/why/state/decisions/risks/history). No copies.
- Code + tests = the only thing that proves reality. Git = history. Ledger = the transaction register.

## Classify the task, then route
| Task is about… | FIRST source | Verify with |
|---|---|---|
| Code structure / "where is X" / blast radius | `graphify query`/`explain`/`affected` → code | tests / run |
| Exact implementation | Graphify to locate → the code file | tests / run |
| Product requirement / pricing | `docs/context_pack/01_…PRD` | code only for a gap |
| Science / measurement / claims | `docs/context_pack/03_…CHARTER` | code / tests |
| Decision / rationale | `docs/context_pack/09_DECISION_LOG` + `adr/` | original evidence |
| Progress / status | canonical entity record → `MINDBOOST_AUDIT_LEDGER.md` | commits / tests |
| Exercise catalog state | `MINDBOOST_121_EXERCISE_PORTFOLIO.md` (+ ledger `MB-CATALOG-*`) | code |
| Historical audit | `MINDBOOST_AUDIT_LEDGER.md` → linked report | hashes / commit |
| Mixed | Obsidian for *what/why*, Graphify for *where/how* | code / tests |

## Budget (progressive retrieval — don't over-pull)
- Direct task: this router + ≤2 canonical notes + ≤2 Graphify queries + the exact files.
- Feature: + ≤5 canonical sections + ≤4 Graphify queries, expand only on contradiction / high risk.
- Never: dump the whole vault, re-read all of the Context Pack by rote, or Grep before a Graphify query.

## Do NOT trust as a source of truth
Auto Memory `project_*` (project facts — being turned off, see [[../AUTO_MEMORY_MIGRATION]]),
the Memory-Layer RAG as a writer, a repeated Claude claim, or a note whose fact has a canonical owner
([[../MEMORY_OWNERSHIP|manifest]]). A recent mtime is not proof of currency.

## Close every task with a ledger transaction
Update `MINDBOOST_AUDIT_LEDGER.md` (finding delta or `NO_DURABLE_MEMORY_DELTA`) + recompute its hash.
Check: `node scripts/memoryctl.mjs`.
