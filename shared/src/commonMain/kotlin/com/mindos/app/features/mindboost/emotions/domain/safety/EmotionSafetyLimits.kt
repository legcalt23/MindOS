@file:Suppress("MagicNumber") // Safety/anti-gaming constants ARE the policy; each is named.

package com.mindos.app.features.mindboost.emotions.domain.safety

import com.mindos.app.features.mindboost.emotions.domain.adaptive.InductionIntensity

/**
 * The safety action the engine should take, evaluated strictly before any performance/difficulty
 * decision. CHECK_IN is armed by overload proxies; REDUCE/STOP by the intensity ceiling.
 */
enum class SafetyDirective { NONE, CHECK_IN, REDUCE, STOP }

/**
 * Hard safety policy for the emotions module (decision B/Q2). Intensity is a discrete 1..5 scale with a
 * HARD operational ceiling of 3: the engine never auto-induces above 3. Levels 4/5 only arrive from a
 * self-report or guardrail and map to REDUCE / STOP.
 */
object EmotionSafetyLimits {

    // Intensity → directive (1-2 normal · 3 operating cap, hold · 4 reduce · 5 stop).
    fun directiveForIntensity(intensity: Int): SafetyDirective = when {
        intensity >= InductionIntensity.STOP_AT -> SafetyDirective.STOP
        intensity == InductionIntensity.REDUCE_AT -> SafetyDirective.REDUCE
        else -> SafetyDirective.NONE
    }

    /** Check-in intensity → directive (1-2 NONE · 3 hold/NONE · 4 REDUCE · 5 STOP). */
    fun directiveForCheckIn(reportedIntensity: Int): SafetyDirective = directiveForIntensity(reportedIntensity)

    // Anti-gaming pause policy (decision G/Q-adaptive). Two counters: a limit counter (every pause)
    // and a 45s cooldown that ONLY suppresses duplicate check-in prompts, never the limit/proxy.
    const val PAUSE_COOLDOWN_MS = 45_000L
    const val MAX_PAUSES_PER_BLOCK = 3
    const val RAPID_PAUSES_FOR_REDUCE = 4

    /** A pause is "neutral" (no concern) if post-pause performance is within this of pre-pause. */
    const val POST_PAUSE_TOLERANCE = 0.05

    // Micro-pause overlay window (neutral, low-motion). Exit visible the whole time; continue only
    // enabled after the minimum.
    const val MICRO_PAUSE_MIN_MS = 20_000L
    const val MICRO_PAUSE_MAX_MS = 30_000L
}
