package com.mindos.app.features.mindboost.memory.domain.exercise

import com.mindos.app.features.mindboost.memory.domain.content.MemoryContent
import com.mindos.app.features.mindboost.memory.domain.content.model.PairId
import com.mindos.app.features.mindboost.memory.domain.content.model.WordId
import com.mindos.app.features.mindboost.memory.domain.model.AxisLevel
import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyVector
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import kotlin.math.roundToInt

/**
 * Produces a deterministic [MemoryTrialPlan] for one trial. Determinism (same
 * inputs → same plan) makes generators unit-testable and replayable, mirroring
 * focus's `TrialGenerator`. Generators draw their stimuli from [MemoryContent]
 * (training pools only — the transfer-probe reserve is never reachable here) and
 * decode the relevant axes of [MemoryDifficultyVector]; other axes are inert.
 *
 * [excludeRecentWordIds]/[excludeRecentPairIds] enforce cross-session no-repetition
 * (anti-engaño); the caller supplies them from a recent-item registry (Phase 7/10).
 */
interface MemoryTrialGenerator {
    val exerciseId: MemoryExerciseId

    fun generate(
        difficulty: MemoryDifficultyVector,
        content: MemoryContent,
        seed: Long,
        excludeRecentWordIds: Set<WordId> = emptySet(),
        excludeRecentPairIds: Set<PairId> = emptySet(),
    ): MemoryTrialPlan
}

/** Shared difficulty-decoding helpers. Generators map normalized axes → concrete params. */
internal object DifficultyDecode {

    /** Linear interpolation, clamped to [0,1] on t. */
    fun lerp(a: Double, b: Double, t: Double): Double = a + (b - a) * t.coerceIn(0.0, 1.0)

    /** Decodes a normalized [level] into an integer count in [min]..[max] (rounded). */
    fun decodeCount(level: AxisLevel, min: Int, max: Int): Int {
        require(min <= max) { "decodeCount requires min <= max (was $min..$max)." }
        return lerp(min.toDouble(), max.toDouble(), level.value).roundToInt().coerceIn(min, max)
    }

    /** Decodes a normalized [level] into a millisecond duration in [minMs]..[maxMs] (rounded). */
    fun decodeMillis(level: AxisLevel, minMs: Long, maxMs: Long): Long {
        require(minMs <= maxMs) { "decodeMillis requires minMs <= maxMs (was $minMs..$maxMs)." }
        return lerp(minMs.toDouble(), maxMs.toDouble(), level.value).roundToInt().toLong().coerceIn(minMs, maxMs)
    }
}
