package com.mindos.app.features.mindboost.core.timing

/** Signed temporal error: `sign` is -1 (early/anticipation), 0 (exact), or +1 (late). */
data class SignedTiming(val deltaNanos: Long, val sign: Int)

/**
 * INFRA-02 · Shared timing arithmetic. [signedRt] preserves the SIGN of the response-vs-stimulus delta
 * WITHOUT throwing — the primitive that anticipatory-error metrics (Velocidad E5/E12) need.
 *
 * Reconciliation note: the plan said `TimebasePolicy.computeRtNanos` throws on rt<0; in the real repo
 * `computeRtNanos` returns a signed delta and does NOT throw — the negative-RT guard is
 * `TrialEngine.require(TimebasePolicy.isValidRt(...))`. This helper is purely additive and changes NO
 * live trial-engine behavior; relaxing that guard for anticipatory trials is deferred to E5/E12.
 */
object TimingPolicy {
    fun signedRt(stimulusNanos: Long, responseNanos: Long): SignedTiming {
        val delta = responseNanos - stimulusNanos
        return SignedTiming(deltaNanos = delta, sign = delta.compareTo(0L))
    }
}
