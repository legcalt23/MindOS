# Memory Layer — Architecture & Decisions

Status: **v1 implemented (2026-06-30)**. This document is the durable design record. It was produced
from a multi-agent investigation (repo audit · live-web-verified tech matrix · security review) plus an
adversarial pre-mortem, then narrowed to the choices below.

---

## 1. Goal & constraints

Give Claude Code a persistent, private, versioned, auditable "project brain" that returns only the
*relevant* context per task — without losing context between sessions or across git worktrees, and
without ever sending secrets/PII to the model.

Hard constraints discovered in the repo:

- **No Docker, no local Postgres** on the machine.
- **Solo developer, Windows**, Node v24, using **Vibe Kanban → one git worktree per task**.
- Repo ethos: **local-first, privacy-first**, strict secret scanning, fail-closed security.
- Five overlapping memory surfaces already exist (CLAUDE.md, AGENTS.md, harness file-memory, docs/) —
  the layer must **unify/index** them, not duplicate.

---

## 2. The decision: hybrid local-first

**Markdown-with-frontmatter files in git are the source of truth.** An embedded **`sqlite-vec` + FTS5**
index (a disposable, gitignored, per-worktree cache) provides hybrid keyword+vector retrieval.
Embeddings sit behind a **provider-decoupled interface** with a local/offline default. Integration is a
**CLI + slash commands**; an MCP server is an optional later phase.

Why this spine:

| Force | How the design answers it |
|---|---|
| Worktree survival | Source of truth is files in git → every worktree has them. The index is a local cache rebuilt per worktree. |
| No Docker/Postgres | `node:sqlite` is built into Node 24 (no native compile); `sqlite-vec` is a single optional extension. |
| Versioning / audit | Git history *is* the version chain and audit trail; entries are immutable-ish (version bump on edit). |
| No vendor lock-in | `Embedder` interface; default is on-device Transformers.js. |
| Privacy-first | Fail-closed redaction guard on write and on retrieval; default embeddings never leave the machine. |
| Robustness | The `.db` is never load-bearing — `reindex` rebuilds it bit-for-bit from the Markdown. |

---

## 3. Options compared

**Vector store** — chosen: `sqlite-vec` via `node:sqlite`.

| Option | Verdict |
|---|---|
| **sqlite-vec + node:sqlite** | ✅ Zero-server, single rebuildable file, FTS5 in the box, no native compile on Win+Node24. |
| pgvector | ❌ Needs Docker or cloud egress — breaks local-first + privacy-first. |
| better-sqlite3 (+sqlite-vec) | ❌ Node-24 Windows native-module compile pain. `node:sqlite` avoids it entirely. |
| LanceDB / DuckDB+vss | ❌ Heavier native engines; multi-file datasets; over-built for a small git-backed corpus. |
| Chroma / hnswlib | ❌ Server-ish, or just an ANN index with no metadata/filtering. |

**Embeddings** — chosen: provider-decoupled interface, local/offline default (Transformers.js,
bge-small/nomic), content-hash cached. API providers (Voyage best-for-code, OpenAI, Gemini) are opt-in
and gated by the redaction guard.

**Integration** — chosen: CLI + slash commands first (works with zero startup surface); MCP server is a
documented phase-2 optimization, never a dependency.

---

## 4. Retrieval

Hybrid **keyword (FTS5/BM25) + vector (sqlite-vec)** fused with **Reciprocal Rank Fusion**, with a
deliberate **keyword bias** and an **exact-token boost** — because this corpus is full of exact tokens
(file paths, task ids, Detekt rule names, error strings) that pure embeddings blur. At most **one chunk
per entry** survives into the result, so a flood of near-duplicate learnings can't crowd out the one
useful hit. The splice is budget-capped (~1500 tokens), every hit carries a verifiable `source:` anchor,
and sensitive bodies are shown as title-only.

---

## 5. Security model

- **Fail-closed redaction guard** (`lib/redaction.mjs`) — a *separate* gate from the warn-only
  `mindos-guard.cjs` hook (which always exits 0 and therefore can't block). Reuses the repo's
  `scan-secrets` patterns + high-entropy heuristic, adds PII (email/phone/IPv4/credit-card-Luhn/JWT). A
  finding **rejects the write**; nothing is persisted.
- **Retrieval-time scrubber** — every snippet is re-scanned and any secret/PII replaced with a
  placeholder before it reaches the model. Defense-in-depth for older/edited entries.
- **v1 stores no sensitive content** — `access_level: sensitive` writes are refused (no encryption
  subsystem; keep real secrets in the OS store / password manager). This deliberately avoids the
  key-management footgun where a machine-bound key can't travel with git-committed ciphertext.
- **PII-free, allowlisted audit log** — append-only JSONL under the gitignored index dir; only
  allowlisted fields, query hashes instead of query text.

---

## 6. Risks (from the pre-mortem) & how the design answers them

| Risk | Mitigation built in |
|---|---|
| **Silent index↔file drift** (the #1 failure) | Per-worktree disposable index + a content-hash manifest; search auto-reindexes when behind and shows a loud `index-stale` banner otherwise. Never answers from a known-stale index. |
| Retrieval not useful → abandoned | Keyword-led RRF + exact-token boost + tight splice + best-per-entry dedup. (Recommended: instrument for 2 weeks; kill if it doesn't beat reading CLAUDE.md.) |
| Secret/PII leak via auto-summary | `/session-end` **drafts only**; embeddings built from redacted text; canary regression test. |
| MCP startup tax | The whole system works with the MCP server off; degrades to keyword-only/grep and says so. |
| Over-engineering → abandoned | Encryption/graph/rotation deliberately deferred or cut; ~5 types carry the value, 15 available. |

---

## 7. Postgres upgrade threshold

Postgres/pgvector becomes worth it only when: corpus exceeds ~100k–500k chunks and sqlite-vec slows
past ~150ms; **or** 3+ independent writers need one shared transactional index; **or** a second human
needs the memory with row-level access control; **or** memory moves server-side for the app itself
(then Firestore — already in the stack — or managed pgvector). Because Markdown stays the source of
truth, that migration is a one-adapter change, not a rewrite.

---

## 8. Scope of v1 vs deferred

**In v1:** 15 types, hybrid retrieval, fail-closed redaction, audit log, CLI + 5 slash commands,
per-worktree auto-reindex + staleness banner, ingest of CLAUDE.md/AGENTS.md read-only, templates,
tests, seed examples. **Deferred:** local vectors are wired but off until `npm install` (keyword-only by
default); MCP server (stubbed); travel-safe field encryption; knowledge graph (cut).
