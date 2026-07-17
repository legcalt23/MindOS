# Phase-C Execution Plan — the irreversible edits (staged, NOT applied)

> ADR-MEM-001 Phases C–H. Each item is BLOCKED for a concrete reason. Apply in a clean/committed tree or
> on explicit confirmation of method. Recorded in the ledger as `MB-MEMORY-0005` (concurrent governance
> rewrite in flight). Nothing here has been applied.

## Why blocked right now
The target files carry heavy **uncommitted foreign WIP** (another agent is mid-rewrite):
`CLAUDE.md` +857/−256 · `.claude/skills/ceo/SKILL.md` +159/−283 · `.claude/settings.json` +19/−8 ·
`.claude/hooks/mindos-guard.cjs` +48/−1. A full rewrite would clobber their work (forbidden by CLAUDE.md §4.10).
**Unblock:** the foreign WIP is committed/landed, OR the founder confirms it's abandoned → then apply below and
reconcile against whatever they shipped.

## 1. CLAUDE.md — add a Memory Router pointer (surgical, additive; do NOT rewrite the 294 KB file)
Insert near the top, after the governance intro:
```md
> **Memory routing (ADR-MEM-001):** Graphify = code structure only; Obsidian/Markdown canon = decisions/status/
> risks/history; no third memory. Start from `docs/memory/00_SYSTEM/MEMORY_ROUTER.md`. Close every task with a
> `MINDBOOST_AUDIT_LEDGER.md` transaction. Validate: `node scripts/memoryctl.mjs`.
```
The 294 KB "Aprendizajes" ledger stays (it's Claude's learning ledger) but is **already excluded from Graphify**
(`.graphifyignore`). Optional later: split learnings into `.claude/skills`/`rules` to shrink CLAUDE.md.

## 2. Auto Memory — turn OFF project-fact capture (keep personal `feedback_*`)
Prereq met: [[AUTO_MEMORY_MIGRATION]] proves no unique project fact is lost. Method to CONFIRM (do not guess a
settings key blindly): either a per-project Claude Code setting in `.claude/settings.json`, or a memory hook that
refuses project-fact writes. Preserve the 4 `feedback_*` + 2 `reference_*`. `settings.json` is dirty → apply after §"Why blocked".

## 3. Memory Layer RAG — demote from writer to read-only index (or deprecate)
- `.claude/memory/README.md` (**clean**): add a banner — "Deprecated as a source of truth (ADR-MEM-001). Canonical
  memory = repo Markdown + `MINDBOOST_AUDIT_LEDGER.md`. Retained only as an optional local retrieval index."
- `/session-start`, `/recall`, `/session-end`, `/adr`, `/memory` (in `.claude/commands/*.md`, **clean**) + the
  `/ceo` skill (**dirty**): point retrieval at the canon, not the RAG as authority. Apply together with §"Why
  blocked" so the `/ceo` change reconciles with the in-flight rewrite (which already touches memory/orchestration).

## 4. Hooks (SessionStart / PreCompact) — `.claude/settings.json` + `mindos-guard.cjs` (both dirty → staged)
- **SessionStart:** inject `docs/memory/00_SYSTEM/MEMORY_ROUTER.md` (small) + the active work item. Not the vault.
- **PreCompact:** flush active state to the work item + ledger (never to Auto Memory).
- Fallback (prompt §15.9): if a hook event is unavailable, fold the gate into the `/ceo` skill + `memoryctl`.

## 5. context_pack de-duplication (`MB-MEMORY-0004`) — needs a CEO ruling, not a guess
`docs/context-pack/` (hyphen, tracked, canonical) vs `docs/context_pack/` (underscore, untracked, divergent —
`09_DECISION_LOG` is *larger*). **Do not delete either blindly.** Ruling needed: is the underscore copy (a) a
newer draft to promote, or (b) stale/foreign WIP to archive? Then diff → merge unique content into the canonical
dir → archive the other under `99_ARCHIVE/` with provenance.

## 6. 121-exercise progress (prompt §14) — default UNKNOWN
A per-exercise matrix already exists in `MINDBOOST_121_EXERCISE_PORTFOLIO.md` (0/120 `TRAINING_READY`). Any
per-exercise status not backed by evidence stays `UNKNOWN`; the global view is generated from per-exercise owners.

## Sequence once unblocked
commit/land foreign WIP → §1 → §5 (CEO ruling) → §3 → §2 → §4 → refresh generated views → `graphify update .`
(clean tree) → `node scripts/memoryctl.mjs` green → ledger transaction per step.
