package com.mindos.app.features.mindboost.memory.domain.session

/**
 * Phases of a memory session, in order (doc §7 reference model):
 * - WARMUP: light activation.
 * - DEEP_ENCODING: main encoding block (strategy focus).
 * - IMMEDIATE_RECALL: test what was just encoded.
 * - INTERFERENCE: a different exercise that also seeds interference for delayed recall.
 * - DELAYED_RECALL: re-test the encoding material, no re-study.
 * - TRANSFER: functional micro-protocol (bridge to real use) — Phase 8/11.
 * - COOLDOWN_REFLECTION: low load + metacognitive reflection.
 */
enum class MemorySessionPhase {
    WARMUP,
    DEEP_ENCODING,
    IMMEDIATE_RECALL,
    INTERFERENCE,
    DELAYED_RECALL,
    TRANSFER,
    COOLDOWN_REFLECTION,
}

/** A phase and its duration within a session. */
data class MemoryPhaseSlot(val phase: MemorySessionPhase, val durationMs: Long) {
    init {
        require(durationMs >= 0) { "MemoryPhaseSlot duration cannot be negative." }
    }
}

/**
 * Time structure of a session for a given [template]. Invariant: phase durations
 * sum EXACTLY to the template's nominal length — enforced here and tested.
 */
data class MemorySessionBlueprint(
    val template: MemorySessionTemplate,
    val phases: List<MemoryPhaseSlot>,
) {
    val totalMs: Long get() = phases.sumOf { it.durationMs }

    init {
        require(phases.isNotEmpty()) { "A blueprint needs at least one phase." }
        require(totalMs == template.totalMinutes * MILLIS_PER_MINUTE) {
            "Blueprint phases must sum to the template length (${template.totalMinutes} min); " +
                "got ${totalMs / MILLIS_PER_MINUTE.toDouble()} min for ${template.name}."
        }
        require(phases.map { it.phase }.toSet().size == phases.size) {
            "A blueprint must not repeat a phase (${template.name})."
        }
    }

    fun hasPhase(phase: MemorySessionPhase): Boolean = phases.any { it.phase == phase }

    fun durationOf(phase: MemorySessionPhase): Long =
        phases.firstOrNull { it.phase == phase }?.durationMs ?: 0L

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}

/**
 * Canonical phase breakdowns per template (doc §7). The numbers ARE the design data
 * (each row sums to the template length), hence MagicNumber is suppressed.
 */
@Suppress("MagicNumber")
object MemorySessionBlueprints {

    private fun blueprint(
        template: MemorySessionTemplate,
        vararg slots: Pair<MemorySessionPhase, Long>,
    ): MemorySessionBlueprint =
        MemorySessionBlueprint(template, slots.map { MemoryPhaseSlot(it.first, it.second) })

    /** 5 min: warmup + single encoding+recall + reflection. No interference/delayed (doc §7). */
    val MICRO_5 = blueprint(
        MemorySessionTemplate.MICRO_5,
        MemorySessionPhase.WARMUP to 30_000,
        MemorySessionPhase.DEEP_ENCODING to 180_000,
        MemorySessionPhase.COOLDOWN_REFLECTION to 90_000,
    )

    /** 10 min: warmup, encoding, immediate, interference, delayed, reflection. */
    val STANDARD_10 = blueprint(
        MemorySessionTemplate.STANDARD_10,
        MemorySessionPhase.WARMUP to 60_000,
        MemorySessionPhase.DEEP_ENCODING to 120_000,
        MemorySessionPhase.IMMEDIATE_RECALL to 120_000,
        MemorySessionPhase.INTERFERENCE to 120_000,
        MemorySessionPhase.DELAYED_RECALL to 120_000,
        MemorySessionPhase.COOLDOWN_REFLECTION to 60_000,
    )

    /** 20 min: full reference model with transfer. */
    val FULL_20 = blueprint(
        MemorySessionTemplate.FULL_20,
        MemorySessionPhase.WARMUP to 120_000,
        MemorySessionPhase.DEEP_ENCODING to 300_000,
        MemorySessionPhase.IMMEDIATE_RECALL to 180_000,
        MemorySessionPhase.INTERFERENCE to 180_000,
        MemorySessionPhase.DELAYED_RECALL to 180_000,
        MemorySessionPhase.TRANSFER to 120_000,
        MemorySessionPhase.COOLDOWN_REFLECTION to 120_000,
    )

    /** 30 min: intensive — bigger encoding + cross-interference; rest folded into cooldown. */
    val INTENSIVE_30 = blueprint(
        MemorySessionTemplate.INTENSIVE_30,
        MemorySessionPhase.WARMUP to 120_000,
        MemorySessionPhase.DEEP_ENCODING to 540_000,
        MemorySessionPhase.IMMEDIATE_RECALL to 240_000,
        MemorySessionPhase.INTERFERENCE to 180_000,
        MemorySessionPhase.DELAYED_RECALL to 360_000,
        MemorySessionPhase.TRANSFER to 240_000,
        MemorySessionPhase.COOLDOWN_REFLECTION to 120_000,
    )

    fun forTemplate(template: MemorySessionTemplate): MemorySessionBlueprint = when (template) {
        MemorySessionTemplate.MICRO_5 -> MICRO_5
        MemorySessionTemplate.STANDARD_10 -> STANDARD_10
        MemorySessionTemplate.FULL_20 -> FULL_20
        MemorySessionTemplate.INTENSIVE_30 -> INTENSIVE_30
    }

    val all: List<MemorySessionBlueprint> = listOf(MICRO_5, STANDARD_10, FULL_20, INTENSIVE_30)
}
