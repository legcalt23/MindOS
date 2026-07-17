# Memory System — Baseline + Architecture ADR (Graphify × Obsidian, no third memory)

> **Status:** PROPOSAL — Phases A (baseline) + B (ADR) complete. Downstream execution
> (Phases C–H) is **BLOCKED pending CEO decision** (see §7). Nothing in the governance
> surface (CLAUDE.md, Auto Memory config, Memory Layer, hooks, settings, docs tree) has
> been mutated by this task. This file is **additive**.
> **Task:** `TASK-2026-07-14-MEMORY-ARCH-BASELINE` · **ADR:** `ADR-MEM-001` (draft, unratified).
> **Ledger:** recorded as findings `MB-MEMORY-0001`, `MB-MEMORY-0002` in `MINDBOOST_AUDIT_LEDGER.md`.

---

## 1. Phase A — Real baseline (all facts `REPO_VERIFIED` via commands run 2026-07-14)

### 1.1 Git / environment
| Fact | Value | Label |
|---|---|---|
| HEAD | `71e17038` (**detached HEAD**, not a branch) | REPO_VERIFIED |
| Working tree | **DIRTY** — dozens of `M` files across `functions/`, `shared/`, `firestore.*`, `CLAUDE.md`, `.claude/settings.json`, `.claude/hooks/mindos-guard.cjs`, `.claude/skills/ceo/SKILL.md`, `AGENTS.md`, `graphify-out/GRAPH_REPORT.md` | REPO_VERIFIED |
| Worktrees (5) | main (detached), **Gemini Antigravity** `cognitive-memory-training-system`, **Claude** `determined-burnell-2714b8` + `gifted-allen-da6183`, **Codex** `platform-hardening` | REPO_VERIFIED |
| Node | v24.14.1 | REPO_VERIFIED |
| **Implication** | The tree mutates in parallel; this is the documented hazard (CLAUDE.md §4.10). Sweeping governance edits here would collide with foreign WIP and be unreviewable as a diff. | CLAIMED |

### 1.2 Graphify
| Fact | Value | Label |
|---|---|---|
| Binary | `graphify 0.9.4` at `~/.local/bin/graphify` | REPO_VERIFIED |
| Git hooks | `post-commit: not installed`, `post-checkout: not installed` (deliberate per CLAUDE.md §6) | REPO_VERIFIED |
| Graph | 33 259 nodes · 50 095 edges · built from commit **`35016318`** | REPO_VERIFIED |
| **STALE** | HEAD is `71e17038` ≠ `35016318` → the graph is stale relative to HEAD | REPO_VERIFIED |
| **CONTAMINATED** | `.graphifyignore` excludes only `.gradle/`, `.kotlin/`, `.codex-security-worktrees/`, `*.class`, `*.bak-*`. It does **not** exclude `docs/`, `CLAUDE.md`, `AGENTS.md`. `GRAPH_REPORT.md` lists a community hub `[[_COMMUNITY_Aprendizajes acumulados del proyecto]]` — a **CLAUDE.md prose section** indexed as structure. | REPO_VERIFIED |

### 1.3 Obsidian
| Fact | Value | Label |
|---|---|---|
| Vault | **NONE** — no `.obsidian` anywhere in repo, no `.base` files, no Obsidian MCP in `.mcp.json` | REPO_VERIFIED |
| **Implication** | The prompt's premise ("Graphify × Obsidian") is **greenfield for Obsidian**. Obsidian is a proposed *viewer*, not an existing system to reconcile. The real fire is memory duplication (§1.4), not Obsidian. | CLAIMED |

### 1.4 Memory systems inventory (the core problem: 5–6 overlapping stores)
| # | System | Location | What it holds | Writer? | Label |
|---|---|---|---|---|---|
| 1 | **Auto Memory** | `~/.claude/projects/…MindOS/memory/` — `MEMORY.md` + ~30 `project_*`/`feedback_*`/`reference_*` | **Project facts**: module status, decisions, feedback. Loaded every session. | Claude (auto) | REPO_VERIFIED |
| 2 | **Memory Layer RAG** | `.claude/memory/` (`cli.mjs`, `mcp-server.mjs`, `config.json`) | Local `bge-small-en-v1.5` embeddings; **ingests `CLAUDE.md`+`AGENTS.md` read-only**; index in `.claude/.memory/`. Commands `/session-start /recall /session-end /adr /memory`. | Claude (via CLI) | REPO_VERIFIED |
| 3 | `.claude/.memory/audit/` | `audit-2026*.jsonl` | Memory Layer audit trail | Memory Layer | REPO_VERIFIED |
| 4 | **Audit Ledger** | `MINDBOOST_AUDIT_LEDGER.md` (root) | Canonical append-only findings/checkpoints, SHA-256, validator. **Well-formed, single-writer.** | Primary session | REPO_VERIFIED |
| 5 | **CLAUDE.md** | root, **294 KB** | Governance + huge "Aprendizajes acumulados" learnings ledger (itself a memory) | Claude | REPO_VERIFIED |
| 6 | **AGENTS.md** | root, **155 KB** (+`.bak`) | Non-Claude agent doctrine + its own learning ledger | agents | REPO_VERIFIED |
| 7 | **Context Pack** | `docs/context-pack/` (00–09: PRD, charters, Decision Log) | **Normative authority** (product/science/decisions) | humans/CEO | REPO_VERIFIED |
| 8 | **Duplicate dir** | `docs/context_pack/` (**underscore**) *and* `docs/context-pack/` (**hyphen**) both exist | Two context-pack directories — duplication smell | — | REPO_VERIFIED |
| 9 | Root doc sprawl | ~25 stray `*_AUDIT.md` / `PARITY_*.md` at repo root + `*.class`, `*.java`, `CLAUDE.md.bak`, `AGENTS.md.bak` | Historic audits, no single owner | mixed | REPO_VERIFIED |

**Conclusion:** at least **three writer-memories** overlap on the same durable facts (Auto Memory, Memory Layer, CLAUDE.md learnings) with **no single owner**, plus a **stale, prose-contaminated Graphify graph**, plus a **duplicated context-pack dir**. This is exactly the "third parallel memory" the prompt forbids.

---

## 2. Phase A — Duplication audit (per prompt §24.2)

| System | What it contained | Prior authority | Proposed destination | Proposed action | Evidence |
|---|---|---|---|---|---|
| Auto Memory `project_*` | Module status, plans, decisions | de-facto authority (loaded every session) | Obsidian/Markdown canonical owners (context-pack, per-entity docs) + ledger | Migrate unique-durable → owner; **turn OFF project-fact capture** | `~/.claude/projects/…/memory/` listing |
| Auto Memory `feedback_*` | Personal working prefs (no-commit-offers, gradle rules) | Auto Memory | **Keep** (personal prefs are the ONE legit Auto-Memory use) | Keep, scoped to prefs only | same |
| Memory Layer RAG | Searchable index over CLAUDE.md/AGENTS.md | index | **Demote to read-only retrieval** over canonical Markdown, or deprecate as writer | Stop treating as a source of truth; keep as optional index | `.claude/memory/config.json` |
| CLAUDE.md "Aprendizajes" | 200+ learning entries (a ledger) | de-facto | Keep as Claude's learning ledger, but **excluded from Graphify** | Add to `.graphifyignore`; trim into skills/rules over time | `GRAPH_REPORT.md` community hub |
| Graphify graph | Structural + **contaminated with docs/CLAUDE/AGENTS prose** | structural index | Structural index **only** (code/tests/build/schemas) | `.graphifyignore` excludes all Markdown authorities; refresh from HEAD | `.graphifyignore`, `GRAPH_REPORT.md` |
| `docs/context_pack/` (underscore) | Possible stale duplicate of `docs/context-pack/` | — | Resolve to ONE canonical dir | Diff the two; archive the stale one with provenance | `ls` both exist |
| Audit Ledger | Findings/checkpoints/SHA | canonical (keep) | **Unchanged** — this IS the transaction register | Keep as-is | ledger validator green |
| Context Pack | PRD/charters/Decision Log | canonical normative (keep) | **Unchanged** — the normative authority | Keep; Obsidian points *at it* (no copy) | context-pack listing |

---

## 3. Phase B — ADR-MEM-001 (DRAFT, pending CEO ratification)

### Decision (proposed)
```
CODE + TESTS + RUNTIME   = verifiable technical reality
GRAPHIFY                 = derived STRUCTURAL index of the code (nothing else)
OBSIDIAN over the repo's real Markdown = human view of canonical memory (no copies)
GIT                      = history / authorship / recovery
AUDIT LEDGER             = the per-task transaction register (already exists — keep)
CLAUDE.md + rules + skills = Claude's operating contract (kept lean)
```
- **One fact, one owner.** No fact about decisions/status/progress/risks lives in two writers.
- **Graphify owns structure only** and never owns decisions/status/rationale. Its corpus excludes every Markdown authority.
- **Obsidian is a viewer over the existing Git Markdown** — direct file access, no second copy, no bidirectional sync. If Obsidian/MCP is down, direct file access still works.
- **No third writer memory:** Auto Memory stops holding project facts (personal prefs only); the Memory Layer RAG is demoted to a read-only index (or deprecated), never a competing source of truth.

### Rejected alternatives (per prompt §21 Fase B)
| Alternative | Why rejected |
|---|---|
| Graphify for everything | Can't hold rationale/status/decisions; a call-graph is not a Decision Log. Wrong tool. |
| Obsidian for everything | Throws away the structural blast-radius Graphify computes for free; humans would hand-maintain call graphs. |
| Graphify → Obsidian export / wiki | Pollutes the human vault with generated maps; creates a second copy of structure that drifts. |
| Auto Memory + Obsidian | Two writers on the same facts → drift; Auto Memory is opaque and per-machine. |
| Memory Layer + Obsidian | Two writers → drift; violates single-owner. |
| Separate vault with copies | Bidirectional sync of two copies = the exact duplication we're eliminating. |
| Additional vector DB | Unneeded; the Memory Layer already does local embeddings at $0. |

### Ownership map (one-fact-one-owner, proposed)
| Fact category | Canonical owner | May only reference (not restate) |
|---|---|---|
| Product requirements / pricing | `docs/context-pack/01_…PRD`, `09_DECISION_LOG` | everything else |
| Scientific measurement / claims | `03_SCIENTIFIC_MEASUREMENT_CHARTER` | PRD, exercises |
| Decisions / rationale | `09_DECISION_LOG` + `docs/.../adr/` | ledger, work items |
| Technical current state | `05_TECHNICAL_CURRENT_STATE` | Graphify (for "where") |
| Findings / risks / audit continuity | `MINDBOOST_AUDIT_LEDGER.md` | reports link in |
| Code structure (who calls what) | **code + Graphify** (never a hand note) | — |
| Per-exercise status (of 121) | per-exercise dossier / canonical record | global generated view |
| Claude operating rules | `CLAUDE.md` + `.claude/rules` + `.claude/skills` | — |
| Personal working prefs | Auto Memory `feedback_*` (only) | — |

### Retrieval routing (proposed, per prompt §10)
| Task class | First source | Verify with |
|---|---|---|
| STRUCTURAL_CODE / EXACT_IMPL | Graphify → targeted code | tests/run |
| PRODUCT / SCIENCE / DECISION | Context Pack / Decision Log / ADR | code only for gaps |
| PROGRESS / STATUS | canonical entity record → ledger | commits/tests |
| HISTORICAL_AUDIT | ledger → linked report | hashes/commit |
| MIXED | Obsidian for *what/why*, Graphify for *where/how* | code/tests |

### Lifecycle & fallbacks
- Graphify refreshes **only when its structural corpus changes** (`graphify update .`); a docs-only change never forces a rebuild. Freshness checked by comparing `GRAPH_REPORT.md` "Built from commit" to `git rev-parse HEAD`.
- Every task ends with a **ledger transaction** (finding delta or `NO_DURABLE_MEMORY_DELTA`) — the mechanism already exists.
- Obsidian down / MCP down → direct Markdown file access is the guaranteed fallback.

---

## 4. Immediate, verified corrective actions (safe, additive — proposed for this pass)
These are low-risk and do **not** touch the irreversible surface; they can be done here or in a worktree:
1. **De-contaminate Graphify:** add `docs/`, `CLAUDE.md`, `AGENTS.md`, `*.md` authorities, `.claude/`, `graphify-out/` to `.graphifyignore`; then `graphify update .` from HEAD to drop prose nodes and refresh staleness. *(Refresh deferred: tree is dirty with foreign WIP — run when clean.)*
2. **Resolve the duplicate context-pack dir** (`context_pack` vs `context-pack`): diff, keep one, archive the other with provenance.
3. Land the ownership manifest + a local `memoryctl`-style validator (prompt §16) as new files under `docs/memory/` and `scripts/`.

## 5. Irreversible actions — BLOCKED pending CEO go (prompt §1.2)
- Rewriting/trimming the 294 KB `CLAUDE.md`.
- Turning **off** Auto Memory project-fact capture (`update-config`/settings).
- Deprecating the Memory Layer RAG as a writer.
- Installing an Obsidian MCP / configuring the vault.
- Restructuring the docs tree (`docs/memory/00_SYSTEM/…`) and migrating Auto-Memory `project_*` into canonical owners.
- Any hook change (`SessionStart`/`PreCompact`/`TaskCompleted`).

## 6. The 121 exercises (prompt §14) — honest note
No auditable per-exercise progress matrix exists yet. Per the prompt, migration must default every unproven exercise to `UNKNOWN` rather than inferring progress from tone or file counts. **Not built in this pass** — gated on the ADR (owner + schema decision).

## 7. Decisions needed from you (CEO gate)
1. **Scope of this pass** — baseline+ADR only (this doc) vs. also do the safe/additive parts (§4) vs. full autonomous rebuild.
2. **Obsidian** — adopt as read-only viewer over the repo Markdown now, defer it (fix duplication first), or wire an Obsidian MCP.
3. **Where to execute** the sweeping changes — a **dedicated worktree/branch** (recommended; project rule forbids governance edits on this dirty detached HEAD) vs. here.

*(Answers recorded, then Phases C–H proceed under the approved scope, each with its own ledger transaction.)*
