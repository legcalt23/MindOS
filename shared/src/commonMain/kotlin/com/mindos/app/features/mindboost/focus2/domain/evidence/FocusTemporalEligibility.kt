package com.mindos.app.features.mindboost.focus2.domain.evidence

import com.mindos.app.features.mindboost.core.timing.ClockDomain
import com.mindos.app.features.mindboost.core.timing.ProbeCapability
import com.mindos.app.features.mindboost.core.timing.TemporalClass
import com.mindos.app.features.mindboost.core.timing.TemporalClassPolicy
import com.mindos.app.features.mindboost.core.timing.TemporalQualityThresholds
import com.mindos.app.features.mindboost.core.timing.TemporalWindowBucket
import com.mindos.app.features.mindboost.core.timing.WindowExclusionReason
import com.mindos.app.features.mindboost.focus2.domain.runtime.FocusRuntimeSnapshot

/**
 * S5 · ADR-FOCUS-TEMPORAL-QUALITY R3-11 (`D-R3-02`, CLOSED) — SHADOW derivation of a session's
 * temporal eligibility from the stratified cells. Pure, versioned by
 * [TemporalQualityThresholds.version], and UNMOUNTED: no production call-site alters any official
 * verdict with this in S5 (Paso 4 wires the candidate). Cite the `R3-*` block, never the plainly
 * numbered sections of that ADR: everything below its `# HISTORIA — R2 (SUPERSEDED…)` marker is
 * preserved evidence, NOT live authority (`MB-FOCUS-0079`).
 *
 * The load-bearing third verdict is [Verdict.NotCaptured]: a v1 record (or a probe-less session)
 * has EMPTY cells, and reading that as "0 bad windows" would fabricate a maximum while reading it
 * as "0 good ones" would fabricate a minimum. Absence is its own typed state, mirroring
 * `Descriptive.Absent`/`Verdict.InsufficientData` on the accuracy axis.
 */
object FocusTemporalEligibility {

    /**
     * Session-level verdict for the SHADOW RT lane.
     *
     * **No softening tier exists, and none is declared.** A `Degraded` variant used to sit here with NO
     * producer, reserved for a phase that then closed without building it. It is DELETED
     * (`MB-FOCUS-0080`), which restores the symmetry Paso 5 established when it deleted the producerless
     * entries of `WindowExclusionReason` rather than reserving them.
     *
     * What licensed the deletion is that the blocker dissolved: `D-R3-02` (CLOSED, R3-11) keeps every
     * temporal diagnostic in SHADOW and states that none of them produces measurement evidence in S5, so
     * there is no measurement claim for a degraded verdict to soften and no arithmetic to freeze. A tier
     * that softens a verdict must arrive WITH the arithmetic that justifies it, its producer and its
     * test; if S6 needs one it reintroduces it then, and until it is handled every exhaustive `when` over
     * this hierarchy stops compiling. Absence enforced by the compiler outranks a pin checked at runtime.
     */
    sealed interface Verdict {
        data object Eligible : Verdict
        data class Excluded(val causes: Set<ExclusionCause>) : Verdict
        data object NotCaptured : Verdict
    }

    enum class ExclusionCause { NON_AUTHORITATIVE_CLASS, COVERAGE_INCOMPLETE, SESSION_BACKSTOP_EXCEEDED }

    /** Everything the shadow candidate needs, derived — never persisted (single authority = cells). */
    data class TemporalEvaluation(
        val sessionClass: TemporalClass,
        /** Trials whose window produced a verdict we could actually read. `NO_OBSERVATION` is NOT one. */
        val windowsClassified: Int,
        /**
         * Trials whose window resolved but could not be classified at all (`NO_OBSERVATION`).
         *
         * Carried as its own count rather than folded into either side: an unobservable window is neither
         * a good one nor a bad one, and the two facts it sits between — "we classified everything" and
         * "of what we classified, this much was bad" — are orthogonal axes that must not be collapsed.
         */
        val windowsUnobserved: Int,
        val coverageComplete: Boolean,
        /** (resolved-excluded + attributed discards) / (classified + discards); null = nothing classified. */
        val excludedFraction: Double?,
        val rtShadowVerdict: Verdict,
    )

    fun evaluate(
        probeCapability: ProbeCapability?,
        clockDomain: ClockDomain,
        snapshot: FocusRuntimeSnapshot,
        thresholds: TemporalQualityThresholds = TemporalQualityThresholds.DEFAULT,
    ): TemporalEvaluation {
        val cells = snapshot.temporalCells
        if (probeCapability == null || cells.isEmpty()) {
            return TemporalEvaluation(
                sessionClass = TemporalClass.T0_NON_AUTHORITATIVE,
                windowsClassified = 0,
                windowsUnobserved = 0,
                coverageComplete = false,
                excludedFraction = null,
                rtShadowVerdict = Verdict.NotCaptured,
            )
        }
        val sessionClass = TemporalClassPolicy.sessionClass(
            capability = probeCapability,
            clockDomain = clockDomain,
            sampledFramesSession = snapshot.temporalSampledFrames,
            thresholds = thresholds,
        )
        // A `NO_OBSERVATION` window resolved a trial but classified NOTHING about its integrity, so it may
        // not enter either side of the exclusion ratio NOR count towards coverage. Summing every bucket
        // here made an all-unobserved session read `coverageComplete = true` with `excludedFraction = 0.0`
        // and therefore Eligible — absence fabricating the maximum, and reachable today: the iOS probe
        // declares `expectedCadenceHz = null`, so `cadenceResolved` fails for EVERY window while
        // `temporalSampledFrames` keeps accumulating and carries the session class to T1.
        val windowsClassified = cells
            .filter { it.bucket.isClassified() }
            .sumOf { it.cell.totalTrials }
        val windowsUnobserved = cells
            .filter { it.bucket == TemporalWindowBucket.NO_OBSERVATION }
            .sumOf { it.cell.totalTrials }
        val excludedResolved = cells
            .filter { it.bucket == TemporalWindowBucket.EXCLUDED }
            .sumOf { it.cell.totalTrials }
        val discardReasons = setOf(
            WindowExclusionReason.PAUSE_DISCARD.name,
            WindowExclusionReason.CLOCK_EPOCH.name,
        )
        val discards = cells.sumOf { cell ->
            cell.excludedByReason.filterKeys { it in discardReasons }.values.sum()
        }
        val denominator = windowsClassified + discards
        val excludedFraction =
            if (denominator > 0) (excludedResolved + discards).toDouble() / denominator else null
        val coverageComplete = windowsClassified == snapshot.resolvedTrials
        val causes = buildSet {
            if (sessionClass != TemporalClass.T1_FRAME_OBSERVED) add(ExclusionCause.NON_AUTHORITATIVE_CLASS)
            if (!coverageComplete) add(ExclusionCause.COVERAGE_INCOMPLETE)
            if ((excludedFraction ?: FULLY_EXCLUDED) > thresholds.sessionExcludedMax) {
                add(ExclusionCause.SESSION_BACKSTOP_EXCEEDED)
            }
        }
        return TemporalEvaluation(
            sessionClass = sessionClass,
            windowsClassified = windowsClassified,
            windowsUnobserved = windowsUnobserved,
            coverageComplete = coverageComplete,
            excludedFraction = excludedFraction,
            rtShadowVerdict = if (causes.isEmpty()) Verdict.Eligible else Verdict.Excluded(causes),
        )
    }

    /**
     * Whether a persisted bucket names a window whose integrity this build could actually read.
     *
     * An ALLOWLIST, and exhaustive on purpose. A denylist (`!= NO_OBSERVATION`) is fail-OPEN twice over:
     * a bucket added later would default into "classified" without anyone deciding that, and `bucket` is
     * nullable because `fromWire` is a fail-closed lookup — a name written by a NEWER schema (the N-1
     * downgrade window `ADR-FOCUS-MIGRATION` allows) arrives as `null` and would have counted as a clean
     * classified window. An unreadable fact is not a good one. Written as a `when` over the enum so the
     * next bucket breaks the build here instead of silently joining the numerator.
     */
    private fun TemporalWindowBucket?.isClassified(): Boolean = when (this) {
        TemporalWindowBucket.CLEAN,
        TemporalWindowBucket.DEGRADED_USABLE,
        TemporalWindowBucket.EXCLUDED,
        -> true

        TemporalWindowBucket.NO_OBSERVATION -> false
        null -> false
    }

    /** An underivable fraction can never pass a "below ceiling" check (fail-closed, not a fake 0.0). */
    private const val FULLY_EXCLUDED: Double = 1.0
}
