package com.mindos.app.features.mindboost.focus.domain.metrics

import kotlin.math.roundToInt

/**
 * Stop-Signal Reaction Time via the integration method (Verbruggen et al. 2019
 * consensus). Used by E4 Freno. Pure and standalone so it can be unit-tested in
 * isolation against the canonical worked example.
 *
 * Also flags "gaming": when p(respond|signal) drifts far from 0.5, the staircase
 * has broken down (typically the user is strategically slowing down), and the
 * SSRT estimate is not trustworthy.
 */
object SsrtCalculator {

    data class SsrtInput(
        /** RTs of all go trials, including go-RTs from failed stops. */
        val goReactionTimes: List<Long>,
        /** Number of stop trials presented. */
        val stopTrials: Int,
        /** Number of stop trials where the user responded anyway (failed to stop). */
        val stopFailures: Int,
        /** Mean stop-signal delay (ms). */
        val meanStopSignalDelayMs: Double,
    )

    sealed interface SsrtResult {
        data object NotComputable : SsrtResult
        data class Value(
            val ssrtMs: Double,
            val probabilityRespondGivenSignal: Double,
            val trustworthy: Boolean,
        ) : SsrtResult
    }

    private const val P_RESPOND_LOW = 0.25
    private const val P_RESPOND_HIGH = 0.75

    fun compute(input: SsrtInput): SsrtResult {
        if (input.goReactionTimes.isEmpty() || input.stopTrials <= 0) return SsrtResult.NotComputable
        val pRespond = input.stopFailures.toDouble() / input.stopTrials
        val sorted = input.goReactionTimes.sorted()
        // The nth go-RT, where n = p(respond|signal) * number of go-RTs.
        val rank = (pRespond * sorted.size).roundToInt().coerceIn(1, sorted.size)
        val nthRt = sorted[rank - 1].toDouble()
        val ssrt = nthRt - input.meanStopSignalDelayMs
        val trustworthy = pRespond in P_RESPOND_LOW..P_RESPOND_HIGH && ssrt > 0.0
        return SsrtResult.Value(
            ssrtMs = ssrt,
            probabilityRespondGivenSignal = pRespond,
            trustworthy = trustworthy,
        )
    }
}
