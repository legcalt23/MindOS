# 🧠 MindOS Memory — Map of Content

> **Obsidian entry point.** Open the **repo root** as an Obsidian vault; this file navigates the
> canonical Markdown. Nothing here is a copy — every link points at the one real file (ADR-MEM-001).
> If Obsidian is closed, all of these are plain Markdown you can open directly.

## Normative authority — *what to build & why*
- [[../context_pack/00_CONTEXT_INDEX|Context Pack index]]
- [[../context_pack/01_MINDBOOST_PRODUCT_PRD|Product PRD]] · [[../context_pack/02_CEO_COPILOT_CHARTER|CEO Charter]]
- [[../context_pack/03_SCIENTIFIC_MEASUREMENT_CHARTER|Science / Measurement Charter]]
- [[../context_pack/04_EXERCISE_QUALITY_STANDARD|Exercise Quality Standard]]
- [[../context_pack/09_DECISION_LOG|Decision Log]]  ✅ *canon: the git-tracked `docs/context_pack/` (underscore, packRelease 2.0.2); the hyphen `docs/context-pack/` is SUPERSEDED and kept only in quarantine — `MB-ENG-0006` resolved*

## Empirical / operational — *what exists & where we are*
- [[../context_pack/05_TECHNICAL_CURRENT_STATE|Technical Current State]]
- [[../../MINDBOOST_AUDIT_LEDGER|Audit Ledger]] — findings, checkpoints, single-writer
- [[../../MINDBOOST_121_EXERCISE_PORTFOLIO|121-Exercise Portfolio]] — catalog burn-up (0/120 `TRAINING_READY`)

## The memory system itself
- [[MEMORY_ARCHITECTURE_BASELINE_AND_ADR_2026-07-14|ADR-MEM-001 — Graphify × Obsidian, no third memory]]
- [[MEMORY_OWNERSHIP|Ownership manifest — one fact, one owner]]

## Structure (NOT here — use the tool)
Code "who calls what / blast radius" is owned by **code + Graphify**, never a hand note:
`graphify query "<question>"` · `graphify explain "X"` · `graphify affected "X"`.
Freshness + scope: `node scripts/memoryctl.mjs`.
