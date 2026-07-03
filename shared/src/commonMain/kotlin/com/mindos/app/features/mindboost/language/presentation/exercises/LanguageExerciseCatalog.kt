package com.mindos.app.features.mindboost.language.presentation.exercises

import com.mindos.app.features.mindboost.language.domain.model.TraitCode

/**
 * The menu of eligible Lenguaje exercises shown when the user enters the module. Each entry is a
 * distinct cognitive mechanic (not the same task repainted) with its own calibrated bank, plus a
 * short interleaved "Sesión mixta". This is the single source the selection screen renders and the
 * state holder loads from.
 */
object LanguageExerciseCatalog {

    /** All standalone exercises, in suggested order (anchored inference first, critical reading last). */
    val exercises: List<LanguageExercise> = listOf(
        EvidenceLockBank.exercise,
        SemanticSurgeryBank.exercise,
        CompressionLadderBank.exercise,
        ArgumentXRayBank.exercise,
        ScopeNegationBank.exercise,
        SourceTriageBank.exercise,
    )

    /** Stable id of the interleaved session (kept distinct from any single-exercise id). */
    const val MIXED_ID = "mixed_session"

    /**
     * A short interleaved round: one calibrated item from each exercise. Interleaving traits within a
     * single session is a deliberate desirable-difficulty design (Bjork), not just variety.
     */
    val mixedSession: LanguageExercise = LanguageExercise(
        id = MIXED_ID,
        title = "Sesión mixta",
        subtitle = "Un ítem de cada ejercicio: alterna habilidades en una ronda corta.",
        primaryTrait = TraitCode.TRANS,
        family = "mixed",
        items = exercises.map { it.items.first() },
    )

    /** Everything offered in the menu: the mixed session first, then each standalone exercise. */
    val menu: List<LanguageExercise> = listOf(mixedSession) + exercises

    /** Look up an exercise (including the mixed session) by id, or null if unknown. */
    fun byId(id: String): LanguageExercise? =
        if (id == MIXED_ID) mixedSession else exercises.firstOrNull { it.id == id }
}
