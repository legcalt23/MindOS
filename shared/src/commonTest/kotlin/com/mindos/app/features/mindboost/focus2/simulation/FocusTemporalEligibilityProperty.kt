// Cohesive property-core module: one value type (NonExclusionState) plus its free operators. The file is
// named for its PURPOSE (the FocusTemporalEligibility monotonicity property), not for the incidental type,
// so MatchingDeclarationName is suppressed at file scope rather than degrading the name or wrapping ~12
// isEligible() call-sites in an object.
@file:Suppress("MatchingDeclarationName")

package com.mindos.app.features.mindboost.focus2.simulation

import com.mindos.app.features.mindboost.core.timing.TemporalClass
import com.mindos.app.features.mindboost.core.timing.TemporalQualityThresholds
import com.mindos.app.features.mindboost.focus2.domain.evidence.FocusTemporalEligibility.TemporalEvaluation
import com.mindos.app.features.mindboost.focus2.domain.evidence.FocusTemporalEligibility.Verdict

/**
 * S5 · Paso 4 — the PURE, platform-agnostic CORE of the shadow-eligibility monotonicity property.
 *
 * This file is `commonTest`, so it compiles for JVM **and** Kotlin/Native; the property it defines is
 * therefore cross-platform by construction. The reducer-driven ORACLE that feeds real `evaluate()`
 * outputs through this core lives in `desktopTest` (which additionally exercises a JVM class-file
 * dependency barrier and is therefore JVM-bound). No JVM API appears here — only kotlin stdlib and
 * focus2 `commonMain` value types — which is what makes the extraction K/N-safe.
 *
 * `CROSS_PLATFORM_PROPERTY_PARITY = NOT_RUN`: this core is verified under `desktopTest` on this host;
 * it is executed under `iosSimulatorArm64Test` only on macOS/CI, which has not run.
 */

/**
 * Fase B — the projection of an evaluation that is INDEPENDENT of how much was excluded.
 *
 * Two evaluations are [comparableByExclusion] only when these fields are identical, so a property that
 * varies `excludedFraction` alone cannot secretly also move temporal authority, coverage completeness,
 * the observed-window denominator, or the threshold configuration. Isolating exactly one axis is what
 * the R2 falsification demanded: the monotonicity claim is only meaningful between states that differ
 * in nothing but their exclusion.
 */
data class NonExclusionState(
    val sessionClass: TemporalClass,
    val coverageComplete: Boolean,
    val windowsClassified: Int,
    /**
     * Held constant too, and that is the point: how much a session could NOT observe is a second axis,
     * and a monotonicity claim that let it drift would be comparing two different instruments. It was
     * invisible while the oracle's domain contained no unobserved window at all.
     */
    val windowsUnobserved: Int,
    val sessionExcludedMax: Double,
    val thresholdsVersion: String,
)

/** Everything about an evaluation except how much it excluded. */
fun TemporalEvaluation.nonExclusionState(thresholds: TemporalQualityThresholds): NonExclusionState =
    NonExclusionState(
        sessionClass = sessionClass,
        coverageComplete = coverageComplete,
        windowsClassified = windowsClassified,
        windowsUnobserved = windowsUnobserved,
        sessionExcludedMax = thresholds.sessionExcludedMax,
        thresholdsVersion = thresholds.version,
    )

fun Verdict.isEligible(): Boolean = this is Verdict.Eligible

/**
 * Fase B — the comparability relation the monotonicity property is defined over: `a` and `b` share the
 * same non-exclusion state, and `a` excluded no MORE than `b`.
 *
 * A `null` excludedFraction is the fail-closed maximum — an underivable fraction is treated as fully
 * excluded — so it orders ABOVE every derivable fraction and never below one. That keeps "absence" on
 * the conservative side of the order, matching the SUT's own `?: FULLY_EXCLUDED` guard.
 */
fun comparableByExclusion(
    a: TemporalEvaluation,
    b: TemporalEvaluation,
    thresholds: TemporalQualityThresholds,
): Boolean =
    a.nonExclusionState(thresholds) == b.nonExclusionState(thresholds) &&
        exclusionRank(a) <= exclusionRank(b)

private fun exclusionRank(evaluation: TemporalEvaluation): Double =
    evaluation.excludedFraction ?: FAIL_CLOSED_MAX

/**
 * Fase B — the minimal monotonicity checker over a sequence ordered by ASCENDING exclusion: once
 * eligibility drops to false it must never return to true. Returns the count of re-openings (0 =
 * monotone).
 *
 * Factored out so BOTH real `evaluate()` outputs (the oracle) and a deliberately broken model (the
 * checker-sensitivity sentinel) run through the identical logic — the sentinel proves this checker is
 * not blind, and the oracle then trusts it on real outputs.
 */
fun reopeningsOverAscendingExclusion(eligibilityByExclusionRank: List<Boolean>): Int {
    var sawExcluded = false
    var reopenings = 0
    for (eligible in eligibilityByExclusionRank) {
        if (!eligible) sawExcluded = true else if (sawExcluded) reopenings++
    }
    return reopenings
}

/** An underivable fraction can never pass a "below ceiling" check — mirrors the SUT's fail-closed guard. */
private const val FAIL_CLOSED_MAX: Double = 1.0
