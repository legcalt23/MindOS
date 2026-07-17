# Auto-Memory Migration Analysis (prompt §9.1 / ADR-MEM-001 Phase F)

> Auto Memory lives at `~/.claude/projects/…MindOS/memory/` (per-machine, OUTSIDE the repo, NOT in the
> shared dirty tree). Goal: turn OFF project-fact capture safely. This classifies every file so we prove
> **nothing UNIQUE_DURABLE lives only in Auto Memory** before disabling it. Classified from `MEMORY.md`
> index descriptions (not deep-read); items marked `REVIEW` need a look before deletion.

## Decisive result
**No project fact lives ONLY in Auto Memory** — every `project_*` has a repo owner (module docs / ledger /
portfolio / context-pack) or is stale. Only **personal working `feedback_*`** must be preserved (the one
legit Auto-Memory use). ⇒ Turning off project-fact capture loses nothing unique. Auto Memory keeps 4 prefs.

## Classification
| Auto-Memory file | Class | Canonical owner (destination) | Action |
|---|---|---|---|
| `feedback_no-commit-offers` | **KEEP** (personal pref) | Auto Memory `feedback_*` | keep |
| `feedback_animation-on-demand` | **KEEP** (personal pref) | Auto Memory `feedback_*` | keep |
| `feedback_gradle-no-destructive-clean` | **KEEP** (personal pref) | Auto Memory `feedback_*` | keep |
| `feedback_no-cli-gradle-when-ide-open` | **KEEP** (personal pref) | Auto Memory `feedback_*` | keep |
| `feedback_mindboost-audit-ledger-protocol` | DEMOTE | `MINDBOOST_AUDIT_LEDGER.md` §1 + `CLAUDE.md` callout | keep as pointer; canon = ledger |
| `feedback_mindboost-rigor-charter` | DUPLICATE | `docs/context-pack/03_…CHARTER` + `CLAUDE.md §2` | pointer; canon = charter |
| `project_system1-cognitive-session-run` | DUPLICATE/STALE | slice docs + ledger (`6R3`) | stop-capture; canon = ledger |
| `project_profile-v2-master-plan` | DUPLICATE | `docs/mindboost_profile_v2/` + ledger | stop-capture |
| `project_scoring-master-plan-reconciled` | DUPLICATE | `docs/mindboost_scoring/MASTER_PLAN_RECONCILED.md` | stop-capture |
| `project_velocidad-mental-mvp-status` | DUPLICATE/STALE | `docs/context-pack/05_TECH_STATE` + portfolio | stop-capture |
| `project_mindboost-lenguaje-status` | DUPLICATE/STALE | module docs + portfolio | stop-capture |
| `project_creativity-mvp-status` | DUPLICATE/STALE | module docs + portfolio | stop-capture |
| `project_module-iii-arena-status` | DUPLICATE/STALE | module docs | stop-capture |
| `project_mindboost-decisiones-status` | DUPLICATE/STALE | module docs + portfolio | stop-capture |
| `project_mindboost-emociones-fase2b-status` | DUPLICATE/STALE | module docs + portfolio | stop-capture |
| `project_mindboostprofile-2a` | DUPLICATE/STALE | module docs | stop-capture |
| `project_ai-coach-status` | DUPLICATE/STALE | module docs | stop-capture |
| `project_app-profile-card` | DUPLICATE/STALE | module docs | stop-capture |
| `project_apptextfield-rollout` | DUPLICATE/STALE | module docs | stop-capture |
| `project_mindboost-mvp-audit-remediation` | DUPLICATE/STALE | ledger + portfolio | stop-capture |
| `project_kmp-cognitive-catalog-infra-p0` | DUPLICATE/STALE | ledger (`INFRA-*`) + docs | stop-capture |
| `project_mindboost-exercise-design-catalog` | DUPLICATE | `docs/mindboost_exercise_design/` + portfolio | stop-capture |
| `project_global-loading-system` | DUPLICATE/STALE | `05_TECH_STATE` | stop-capture |
| `project_firestore-sync-phase-status` | DUPLICATE/STALE | `05_TECH_STATE` + ledger | stop-capture |
| `project_firebase-appid-migration` | STALE | `05_TECH_STATE` | archive |
| `project_security-hardening-status` | STALE (self-labelled) | ledger | archive |
| `project_graphify-memory-graph` | DUPLICATE | `docs/memory/` (ADR-MEM-001) | pointer; canon = ADR |
| `project_cognitive-science-corpus` | REVIEW (records deliberate ingestion) | `docs/knowledge/` + ADR note | migrate note → `docs/memory/`, then stop-capture |
| `project_memory-layer` | SUPERSEDED | ADR-MEM-001 (`superseded_by`) | mark superseded |
| `project_n8n-agent-runner` | REVIEW (infra pointer) | `docs/` or reference | keep as reference |
| `reference_skills-cli-find-skills` | KEEP (reference) | Auto Memory `reference_*` | keep |

## Turn-off (Phase C — BLOCKED, needs method + clean tree)
Disabling project-fact capture is a Claude Code config action, not a content move. Exact mechanism (per-project
setting vs memory hook) is staged in [[EXECUTION_PLAN_PHASE_C]]; `.claude/settings.json` currently has foreign WIP.
Prereq satisfied by this file: no UNIQUE_DURABLE loss. Preserve the 4 `feedback_*` prefs + the 2 `reference_*`.
