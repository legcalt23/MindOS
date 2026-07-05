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
     */
    const val Sessions = "mindboost_sessions"

    /**
     * Durable Firestore-sync queue (Phase 1.1). Holds per-user sync metadata (userId, sessionId, fingerprint
     * hash — never raw trials). Account-bound so it is erased on account deletion, even though the sync
     * coordinator is not yet mounted in production (the namespace is empty until the pilot is activated).
     */
    const val SyncQueue = "mindboost_sync_queue"

    val AccountBound: List<String> =
        listOf(Focus, Memory, Speed, Logic, Creativity, Emotions, Decisiones, Language, Profile, Sessions, SyncQueue)
}
