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

    val AccountBound: List<String> =
        listOf(Focus, Memory, Speed, Logic, Creativity, Emotions, Decisiones, Language, Profile)
}
