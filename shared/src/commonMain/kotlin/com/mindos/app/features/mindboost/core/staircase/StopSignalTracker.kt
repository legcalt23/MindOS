package com.mindos.app.features.mindboost.core.staircase

/**
 * Online 1-up/1-down staircase for the stop-signal delay (SSD). After a SUCCESSFUL
 * stop the SSD increases (harder to stop next time); after a FAILED stop it
 * decreases. This tracks the SSD towards p(respond|signal) ~ 0.5, the regime where
 * SSRT is validly estimable. The SSD is inherently ONLINE — it depends on the
 * user's responses, so it is never pre-generated.
 *
 * Promoted to `core` in Ola 3 · R1: Enfoque E4 and the fused Emociones inhibition
 * exercise run the SAME staircase, and a second copy of a convergence rule is how
 * two engines quietly stop estimating the same quantity.
 */
class StopSignalTracker(
    private val initialSsdMs: Long,
    private val stepMs: Long,
    private val minSsdMs: Long = MIN_SSD_MS,
    private val maxSsdMs: Long = MAX_SSD_MS,
) {
    var currentSsdMs: Long = initialSsdMs
        private set

    fun onStopOutcome(successfulStop: Boolean) {
        currentSsdMs = if (successfulStop) {
            (currentSsdMs + stepMs).coerceAtMost(maxSsdMs)
        } else {
            (currentSsdMs - stepMs).coerceAtLeast(minSsdMs)
        }
    }

    private companion object {
        const val MIN_SSD_MS = 0L
        const val MAX_SSD_MS = 1_000L
    }
}
