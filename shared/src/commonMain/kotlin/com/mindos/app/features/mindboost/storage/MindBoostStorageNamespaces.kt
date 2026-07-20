package com.mindos.app.features.mindboost.storage

/** Canonical encrypted namespaces containing account-bound cognitive data. */
object MindBoostStorageNamespaces {
    const val Focus = "mindboost_focus"
    const val Memory = "mindboost_memory"
    const val Speed = "mindboost_speed"
    const val Logic = "mindboost_logic"
    const val Creativity = "mindboost_creativity"
    const val Emotions = "mindboost_emotions"

    /** Decisiones module: encrypted per-user decision-making data (e.g. the E2 reversibility-card log). */
    const val Decisiones = "mindboost_decisiones"

    /** Lenguaje module: encrypted per-user verbal evidence (evidence packets + aggregate session summaries). */
    const val Language = "mindboost_language"

    /** The global cognitive "brain profile" snapshot (Fase 2A). Derived, coarse, local-only. */
    const val Profile = "mindboost_profile"

    /**
     * Normalized session lifecycle log (Fase 2, DC-16). One row per training session: which block, when it
     * started/updated, how far it got, and whether it FINALIZED or was ABANDONED. Per-user and encrypted;
     * holds ONLY lifecycle + coarse engagement counters, never raw trials, per-item responses or free text.
     *
     * "Per-user" is now ENFORCED, not aspirational (SYSTEM-1 slice 6-R3, §14 fold-in): the physical namespace
     * below is ONE device-wide file shared by every locally-authenticated account, but every physical key
     * [com.mindos.app.features.mindboost.core.session.SecureSessionRecordStore] writes is scoped by a
     * mandatory, first-class `subject` constructor parameter, so two accounts sharing this namespace can never
     * read, list, or delete one another's rows (previously they could — see the historical isolation gap fixed
     * in that slice).
     */
    const val Sessions = "mindboost_sessions"

    /**
     * SYSTEM-1 unified cognitive-history store (`CognitiveSessionRun`). One sealed, immutable row per completed
     * session holding ONLY the honest cognitive OUTCOME (aggregate outcome counts, coarse metrics, honest score,
     * eligibility) — never raw trials, per-item content, userId or sync state. Per-user and encrypted;
     * account-bound so it is fully erased on account deletion. The namespace is empty until an exercise is wired
     * to seal runs into it (E8 adapter is ready but productive wiring is gated on this coverage).
     */
    const val SessionRuns = "mindboost_session_runs"

    /**
     * Durable Firestore-sync queue (Phase 1.1). Holds per-user sync metadata (userId, sessionId, fingerprint
     * hash — never raw trials). Account-bound so it is erased on account deletion, even though the sync
     * coordinator is not yet mounted in production (the namespace is empty until the pilot is activated).
     */
    const val SyncQueue = "mindboost_sync_queue"

    /**
     * AI Coach durable state (MC8u-G): the Elite user's IA consent record, the daily coach message quota, and the
     * minimized coach memory (coarse Markdown views only, never raw trials / self-report / free text / PII). Local
     * encrypted (SecureKeyValueStore); account-bound so it is fully erased on account deletion.
     */
    const val Coach = "mindboost_coach"

    /**
     * S3 write-ahead payload journal: a transient append-only log of session payloads that MUST survive a process
     * death between "session completed" and "session persisted to its destination store" (reversibility log, E8
     * cognitive run). Each entry holds ONLY the minimized destination summary the sink already persists (never raw
     * trials), keyed by the session id so replay is idempotent. Local-only (never synced to cloud) and
     * account-bound so it is erased on account deletion; in steady state it is empty (entries are drained + removed
     * on confirmation). See
     * [com.mindos.app.features.mindboost.core.session.payload.PayloadJournalStore].
     */
    const val PayloadJournal = "mindboost_payload_journal"

    /**
     * SYSTEM-1 slice 6-R3 (P2-01) · PERMANENT per-subject revocation tombstones
     * ([com.mindos.app.features.mindboost.core.session.SubjectRevocationAuthority]). One tiny presence marker per
     * erased subject (key `revocation:<subject>`, value `"1"`); holds NO cognitive data.
     *
     * DELIBERATELY NOT in [AccountBound] — and MUST NEVER be added to it. This is an ANTI-RESURRECTION marker
     * that MUST SURVIVE account deletion: blanket-clearing it (the `AccountBound` erasure path) would let a stale
     * in-flight writer resurrect a deleted subject's data after erasure reported complete. Enforced by the
     * negative guard test `AccountBoundErasureCoverageTest` (revocation NOT in AccountBound).
     */
    const val Revocation = "mindboost_revocation"

    /**
     * Focus V5 · S1 · ADR-FOCUS-MIGRATION · the Focus v2 engine's own namespace.
     *
     * Deliberately SEPARATE from [Focus]: v2 does not replace v1's data and never migrates it in place. The v1
     * history stays under its own namespace, RETIRED, and is retired only once a complete functional replacement
     * plus a user-facing export exist — deleting a user's real history to make room for a rewrite is data loss,
     * not a migration.
     *
     * Registered in [AccountBound] from its FIRST commit, before anything writes to it: a namespace that starts
     * holding cognitive data and is wired into the eraser afterwards has, in between, been data that account
     * deletion silently missed.
     */
    const val Focus2 = "mindboost_focus2"

    val AccountBound: List<String> = listOf(
        Focus, Focus2, Memory, Speed, Logic, Creativity, Emotions, Decisiones, Language, Profile, Sessions,
        SessionRuns, SyncQueue, Coach, PayloadJournal,
    )
}
