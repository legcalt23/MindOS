package com.mindos.app.features.mindboost.profile.domain.display

import com.mindos.app.features.mindboost.core.scoring.ScoringVersionRegistry
import com.mindos.app.features.mindboost.profile.domain.model.DimensionState
import com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus
import kotlin.math.roundToInt

/**
 * A cognitive dimension's DISPLAYABLE signal — the sealed boundary between the internal cognitive profile
 * and any user-facing number. Construction is PRIVATE: the ONLY way to obtain one is [fromDimensionState],
 * which yields `null` when the dimension has no computable value ([DimensionState.value] == null — i.e.
 * CALIBRATING, procedural feedback, or an invalid internal score). Raw metrics and procedural feedback
 * therefore have no accidental path to a visible score: it is impossible by construction.
 *
 * It carries ONLY [status] + [confidence] and has NO notion of session count. The full display gate
 * (scoring doc §13) also requires ≥2 valid sessions cross-session for an EMERGING dimension; that clause
 * belongs to the upstream calibration policy (DC-3, which decides EMERGING/ESTABLISHED) and is NOT yet
 * enforced by the current aggregator. Until the DC-3 slice lands, this mapper deliberately gates on
 * status + confidence ONLY — so an EMERGING dimension that reaches confidence ≥ 0.55 early can surface a
 * *provisional* number. It never re-derives session-count maturity here.
 */
class ProfileDimensionSignal private constructor(
    val normalized: Double,
    val status: DimensionStatus,
    val confidence: Double,
) {
    companion object {
        /** The ONLY factory. Returns `null` when [state] has no displayable value (`value == null`). */
        fun fromDimensionState(state: DimensionState): ProfileDimensionSignal? {
            val value = state.value ?: return null
            return ProfileDimensionSignal(normalized = value, status = state.status, confidence = state.confidence)
        }
    }
}

/**
 * The user-facing verdict of the display gate (DC-2). [Withheld] renders as "—", NEVER as 0.
 *
 * Both number-bearing tiers are named unambiguously PROVISIONAL (SYSTEM-1 Slice 6-R3, C2.4 item 5): no
 * ADR-approved equating/scoring-version compatibility exists yet for the sources that reach ESTABLISHED
 * today (Focus/Logic self-declare their scorers `preliminary`), so a plain "Official" label would over-claim
 * validation the measurement doesn't have. This renames the type only — the DC-2 gating logic is unchanged.
 */
sealed interface DisplayScore {
    /** ESTABLISHED confidence tier — a 0-100 number; still provisional pending equating (never "official"). */
    data class ProvisionalStable(val value0to100: Int) : DisplayScore

    /** EMERGING tier that passes the confidence gate — a 0-100 number the UI labels "en progreso". */
    data class ProvisionalEmerging(val value0to100: Int) : DisplayScore

    /** Below the display gate, or no displayable signal — the UI shows "—", never a number. */
    data object Withheld : DisplayScore
}

/**
 * Maps a [ProfileDimensionSignal] to a user-facing [DisplayScore] under the DC-2 display gate: an integer
 * is shown ONLY for ESTABLISHED, or EMERGING with `confidence ≥ `[DISPLAY_CONFIDENCE_GATE]; everything
 * else is [DisplayScore.Withheld] ("—", never 0). The `0..1 → 0-100` map is linear and PRELIMINARY (no
 * per-block baseline curve yet, scoring doc §7); its version is pinned in [ScoringVersionRegistry].
 *
 * The gate depends ONLY on [DimensionStatus] + confidence — never on session count (that is upstream
 * calibration, DC-3).
 */
object UserDisplayScoreMapper {

    /** DC-2 confidence threshold to show a provisional integer for an EMERGING dimension. */
    const val DISPLAY_CONFIDENCE_GATE: Double = 0.55

    /** This mapper's pinned version (single source of truth; no scattered version strings). */
    val version: String get() = ScoringVersionRegistry.USER_DISPLAY_SCORE_MAPPER

    /** The DC-2 gate. A `null` signal (no displayable value) is always [DisplayScore.Withheld]. */
    fun displayScore(signal: ProfileDimensionSignal?): DisplayScore = when {
        signal == null -> DisplayScore.Withheld
        signal.status == DimensionStatus.ESTABLISHED ->
            DisplayScore.ProvisionalStable(toPercent(signal.normalized))
        signal.status == DimensionStatus.EMERGING && signal.confidence >= DISPLAY_CONFIDENCE_GATE ->
            DisplayScore.ProvisionalEmerging(toPercent(signal.normalized))
        else -> DisplayScore.Withheld
    }

    /** Convenience: gate a [DimensionState] directly (builds the sealed signal, then applies the gate). */
    fun displayScore(state: DimensionState): DisplayScore =
        displayScore(ProfileDimensionSignal.fromDimensionState(state))

    private const val PERCENT_SCALE = 100.0
    private const val MAX_PERCENT = 100

    private fun toPercent(normalized: Double): Int =
        (normalized * PERCENT_SCALE).roundToInt().coerceIn(0, MAX_PERCENT)
}
