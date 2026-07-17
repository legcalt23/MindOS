# MEMORY_CONTRACT — epistemic labels + write/transaction rules

> How facts enter memory and how tasks close. Companion to [[MEMORY_ROUTER]] and [[../MEMORY_OWNERSHIP]].
> The mechanism already exists: `MINDBOOST_AUDIT_LEDGER.md` (append-only, single-writer, SHA-256, validator).

## Epistemic labels (never elevate without evidence)
`CEO_APPROVED` · `REPO_VERIFIED` · `TEST_VERIFIED` · `RUNTIME_VERIFIED` · `CLAIMED_BY_CLAUDE` ·
`INFERRED` · `PROVISIONAL` · `TARGET` · `UNKNOWN` · `BLOCKED` · `CONTRADICTED` · `SUPERSEDED`.
- A green *targeted* test is not `REPO_VERIFIED` of the whole system.
- A repeated Claude claim never becomes `REPO_VERIFIED`.
- A recent file mtime is not proof of conceptual currency.

## What may enter canonical memory (at least one must hold)
Changes/creates/resolves a decision, state, progress, risk, durable constraint, reproducible evidence,
contradiction, architecture, contract, Definition of Done, reusable lesson, or the next action.

## What must NOT enter it
Step narration · fruitless search · full tool outputs · transient opinions · anything reconstructable
from the code (use Graphify) · any fact that already has a canonical owner (link it instead).

## One fact, one owner (see [[../MEMORY_OWNERSHIP]])
Other docs reference the owner (link / ID); they never restate the fact in independent prose.

## Single writer + transaction per task
- Only the primary session writes the ledger. Subagents return proposed deltas; the owner reconciles.
- Every task (incl. subtasks, blocked/interrupted, no-code) closes with a ledger transaction:
  a finding delta **or** an explicit `NO_DURABLE_MEMORY_DELTA` + reason.
- Atomic close: owner updated → ledger event + checkpoint → links valid → generated views refreshed →
  Graphify refreshed *if its corpus changed* → validators green (`memoryctl`, `validate-audit-ledger`).
- **Concurrency (observed, `MB-MEMORY-0003`):** single-writer is doc-only, not enforced. Reconcile the
  §0 count+hash **last**, after all append-only rows, and recompute the SHA over the on-disk file.

## Memory receipt (end of task)
```yaml
memory_receipt: { task_id:, durable_delta:, canonical_files_updated: [], ledger_entry_added:,
  generated_views_refreshed:, graph_refresh: {required:, performed:, reason:},
  stale_or_conflicting_memory_found: [], unresolved_unknowns: [] }
```
Do not claim DONE if the receipt is incomplete.
