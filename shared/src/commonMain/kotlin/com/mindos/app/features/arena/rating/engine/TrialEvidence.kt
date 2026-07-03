@file:Suppress("MagicNumber") // 0.0/1.0 bounds are explicit, named domain limits, not incidental magic.

package com.mindos.app.features.arena.rating.engine

import com.mindos.app.features.arena.config.ComparabilityStateEnum
import com.mindos.app.features.arena.config.ConfidenceStateEnum
import com.mindos.app.features.arena.config.IntegrityStateEnum
import com.mindos.app.features.arena.config.SessionValidityStatusEnum
import com.mindos.app.features.arena.config.SmurfSuspicionLevel
import com.mindos.app.features.arena.config.TaskMotorType
import com.mindos.app.features.arena.hardware.HardwareClass
import com.mindos.app.features.arena.rating.model.Confidence
import com.mindos.app.features.arena.rating.model.LatencyMs
import com.mindos.app.features.arena.rating.model.MatchQuality
import com.mindos.app.features.arena.rating.model.MeasurementQuality
import com.mindos.app.features.arena.rating.model.Rating

/**
 * Phase 03/04/05: Immutable, fully-validated evidence from one completed trial that feeds the
 * Bayesian rating engine.
 *
 * Every field is either a validated value object or a typed enum, and the constructor rejects
 * impossible combinations (out-of-range performance, non-positive mode scaling, Mental Speed
 * without a hardware class). There is deliberately NO premium / subscription / self-report /
 * fatigue field: only objective task signal and integrity/comparability gating may influence a
 * rating (observed_task_performance.md, no_premium_rating_guard.md).
 */
@Suppress("LongParameterList") // Auditable evidence is an irreducible, gate-bearing record.
data class TrialEvidence(
    val motorType: TaskMotorType,
    /** Task/context difficulty in rating units (opponent-independent). */
    val taskDifficulty: Rating,
    /** Objective normalized motor outcome in [0,1]. No fatigue/readiness/self-report. */
    val observedPerformance: Double,
    val accuracy: Confidence,
    val latency: LatencyMs,
    val integrityState: IntegrityStateEnum,
    val ledgerApprovedCurrent: Boolean,
    val comparability: ComparabilityStateEnum,
    val measurementConfidence: ConfidenceStateEnum,
    val sessionValidity: SessionValidityStatusEnum,
    val measurementMultiplier: MeasurementQuality,
    val comparabilityMultiplier: MatchQuality,
    /** Mode scaling factor (> 0); part of the final blended multiplier. */
    val modeScalingFactor: Double,
    val smurfSuspicion: SmurfSuspicionLevel,
    /** Required for [TaskMotorType.MENTAL_SPEED_CHOICE_REACTION]; optional for other motors. */
    val hardwareClass: HardwareClass? = null,
) {
    init {
        require(observedPerformance in 0.0..1.0 && !observedPerformance.isNaN()) {
            "observedPerformance must be in [0,1]"
        }
        require(modeScalingFactor > 0.0 && !modeScalingFactor.isNaN() && !modeScalingFactor.isInfinite()) {
            "modeScalingFactor must be a finite value > 0"
        }
        if (motorType == TaskMotorType.MENTAL_SPEED_CHOICE_REACTION) {
            requireNotNull(hardwareClass) {
                "Mental Speed evidence requires a HardwareClass for valid measurement"
            }
        }
    }
}
