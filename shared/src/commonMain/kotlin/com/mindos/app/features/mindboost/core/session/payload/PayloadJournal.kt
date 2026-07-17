package com.mindos.app.features.mindboost.core.session.payload

/**
 * S3 write-ahead payload journal — the transient append-only log that closes the "process died between a session
 * being COMPLETED and its payload being persisted to its destination store" data-loss window (audit B1/B3/B6/B7).
 *
 * WHY IT EXISTS: several MindBoost seals persisted their payload via a fire-and-forget
 * `scope.launch { store.record(...) }` (Decisiones reversibility) or a `NonCancellable` coroutine that survives
 * screen dispose but NOT process death (E8). A journal write that COMPLETES before navigation, drained + confirmed
 * by an idempotent replay on the next app start, upgrades those paths from "lost on kill" to "recovered on replay".
 *
 * MINIMIZATION (charter): an entry holds ONLY the minimized summary its destination store already persists (the
 * `ReversibilityTrialDto` log / the `CognitiveSessionRun`), serialized opaquely into [JournaledPayload.payload] —
 * never raw trials, per-item behavior, or a richer snapshot than the sink itself keeps.
 *
 * D-19 (S3 vs S10) — SEPARATE PRIMITIVES, by design: this is a LOCAL, encrypted, no-network write-ahead whose
 * replay re-applies to LOCAL idempotent sinks (no retry/backoff/signature). The S10 remote intent outbox is a
 * different failure model (network retries, exponential backoff, server-side idempotency) and its metadata already
 * lives in the `SyncQueue` (`core.sync`); sharing one abstraction across both would be a premature over-generalization
 * over two unrelated concerns. Keep them separate.
 */
enum class PayloadKind {
    /** A finished Decisiones session's reversibility log (destination: `LocalReversibilityResultStore`). */
    REVERSIBILITY_LOG,

    /** A sealed E8 Go/No-Go `CognitiveSessionRun` (destination: `CognitiveSessionRunStore`, idempotent by runId). */
    E8_COGNITIVE_RUN,
}

/**
 * One journaled payload: the destination-minimized [payload] plus the [entryId] (the session id) that makes replay
 * idempotent — a session already drained + confirmed is no longer in [PayloadJournalStore.pending], and re-applying
 * the same [entryId] is a no-op at its idempotent sink. [enqueuedAtMs] is `null` when unknown (never a `0L`
 * sentinel — see the charter's "unknown is null" rule).
 */
data class JournaledPayload(
    val entryId: String,
    val kind: PayloadKind,
    val payload: String,
    val enqueuedAtMs: Long?,
)

/**
 * Subject-scoped, append-only write-ahead journal. Bound to ONE subject (the authenticated user) at construction,
 * exactly like `SecureSessionRecordStore` / `SecureCognitiveSessionRunStore`, so [pending] and [confirm] can only
 * ever see or remove THIS subject's own entries even though the physical namespace is one device-wide file shared
 * by every locally-authenticated account.
 *
 * IDEMPOTENT ENQUEUE: keyed by [entryId], so re-journaling the same session UPSERTS (never duplicates) the entry.
 */
interface PayloadJournalStore {
    /** Writes (or replaces) the entry for [entryId]. Suspends until the write is durable (never fire-and-forget). */
    suspend fun enqueue(entryId: String, kind: PayloadKind, payload: String)

    /** This subject's not-yet-confirmed entries (order unspecified; the replay drains all of them). */
    suspend fun pending(): List<JournaledPayload>

    /** Removes the entry for [entryId] AFTER its payload has been durably applied to its destination store. */
    suspend fun confirm(entryId: String)
}

/** In-memory journal for tests and non-durable platforms; durable only within a single process run. */
class InMemoryPayloadJournalStore : PayloadJournalStore {
    private val entries = linkedMapOf<String, JournaledPayload>()

    override suspend fun enqueue(entryId: String, kind: PayloadKind, payload: String) {
        require(entryId.isNotBlank()) { "entryId must not be blank." }
        entries[entryId] = JournaledPayload(entryId, kind, payload, enqueuedAtMs = null)
    }

    override suspend fun pending(): List<JournaledPayload> = entries.values.toList()

    override suspend fun confirm(entryId: String) {
        entries.remove(entryId)
    }
}

/**
 * A journal that persists nothing — the default for holders whose journal is optional (e.g. the E8 state holder in
 * tests that do not exercise the write-ahead). Every operation is a no-op, so behavior is identical to the pre-S3
 * fire-and-forget path when no real journal is wired.
 */
object NoOpPayloadJournalStore : PayloadJournalStore {
    override suspend fun enqueue(entryId: String, kind: PayloadKind, payload: String) = Unit
    override suspend fun pending(): List<JournaledPayload> = emptyList()
    override suspend fun confirm(entryId: String) = Unit
}
