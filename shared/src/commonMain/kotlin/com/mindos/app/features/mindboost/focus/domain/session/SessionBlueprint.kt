package com.mindos.app.features.mindboost.focus.domain.session

/**
 * Phases of a session, in order (07_session_periodization_plan.md §0).
 * - WARMUP: re-engages below working level.
 * - CORE: the main adaptive training.
 * - PEAK: brief push to high difficulty.
 * - TRANSFER: weak/subjective functional micro-protocols.
 * - COOLDOWN: low load, no error spiral.
 * - REFLECTION: brief questionnaire + honest feedback.
 */
enum class SessionPhase { WARMUP, CORE, PEAK, TRANSFER, COOLDOWN, REFLECTION }

/** A phase and its duration within a session. */
data class PhaseSlot(val phase: SessionPhase, val durationMs: Long) {
    init {
        require(durationMs >= 0) { "PhaseSlot duration cannot be negative." }
    }
}

/**
 * The time structure of a session for a given [template]. Invariant: the phase
 * durations sum EXACTLY to the template's nominal length — enforced here and tested.
 */
data class SessionBlueprint(
    val template: SessionTemplate,
    val phases: List<PhaseSlot>,
) {
    val totalMs: Long get() = phases.sumOf { it.durationMs }

    init {
        require(phases.isNotEmpty()) { "A blueprint needs at least one phase." }
        require(totalMs == template.totalMinutes * MILLIS_PER_MINUTE) {
            "Blueprint phases must sum to the template length (${template.totalMinutes} min); " +
                "got ${totalMs / MILLIS_PER_MINUTE.toDouble()} min for ${template.name}."
        }
    }

    fun durationOf(phase: SessionPhase): Long = phases.filter { it.phase == phase }.sumOf { it.durationMs }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}

/**
 * Canonical phase breakdowns per template (07_ §0 table). The numbers ARE the
 * design data (each row sums to the template length), hence MagicNumber is suppressed.
 */
@Suppress("MagicNumber")
object SessionBlueprints {

    private fun blueprint(template: SessionTemplate, vararg slots: Pair<SessionPhase, Long>): SessionBlueprint =
        SessionBlueprint(template, slots.map { PhaseSlot(it.first, it.second) })

    val MAINTENANCE_5 = blueprint(
        SessionTemplate.MAINTENANCE_5,
        SessionPhase.WARMUP to 30_000,
        SessionPhase.CORE to 210_000,
        SessionPhase.COOLDOWN to 30_000,
        SessionPhase.REFLECTION to 30_000,
    )

    val STANDARD_10 = blueprint(
        SessionTemplate.STANDARD_10,
        SessionPhase.WARMUP to 60_000,
        SessionPhase.CORE to 360_000,
        SessionPhase.PEAK to 90_000,
        SessionPhase.COOLDOWN to 60_000,
        SessionPhase.REFLECTION to 30_000,
    )

    val DEVELOPMENT_20 = blueprint(
        SessionTemplate.DEVELOPMENT_20,
        SessionPhase.WARMUP to 120_000,
        SessionPhase.CORE to 600_000,
        SessionPhase.PEAK to 180_000,
        SessionPhase.TRANSFER to 180_000,
        SessionPhase.COOLDOWN to 60_000,
        SessionPhase.REFLECTION to 60_000,
    )

    val ELITE_30 = blueprint(
        SessionTemplate.ELITE_30,
        SessionPhase.WARMUP to 180_000,
        SessionPhase.CORE to 840_000,
        SessionPhase.PEAK to 240_000,
        SessionPhase.TRANSFER to 300_000,
        SessionPhase.COOLDOWN to 120_000,
        SessionPhase.REFLECTION to 120_000,
    )

    fun forTemplate(template: SessionTemplate): SessionBlueprint = when (template) {
        SessionTemplate.MAINTENANCE_5 -> MAINTENANCE_5
        SessionTemplate.STANDARD_10 -> STANDARD_10
        SessionTemplate.DEVELOPMENT_20 -> DEVELOPMENT_20
        SessionTemplate.ELITE_30 -> ELITE_30
    }

    val all: List<SessionBlueprint> = listOf(MAINTENANCE_5, STANDARD_10, DEVELOPMENT_20, ELITE_30)
}
