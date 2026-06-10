package com.mindos.app.features.mindboost.memory.domain.personalization

import kotlin.math.abs

/**
 * Normalized behavioural signals an archetype is inferred from. Decoupling the
 * heuristic from raw metrics keeps it pure and exhaustively testable.
 *
 * @param consolidationRatio delayed/immediate recall (doc §12 c: < 0.50 forgets fast).
 * @param discriminationDPrime signal-detection d-prime (doc §12 d: low confuses similar).
 * @param confidenceBias over (+) / under (-) confidence (doc §12 e/f: |bias| > 0.20).
 * @param workingMemorySpanNorm 0..1 normalized complex span (doc §12 b: low ≤ p25).
 */
data class MemoryArchetypeSignals(
    val accuracy: Double,
    val rtMedianMs: Double,
    val consolidationRatio: Double,
    val discriminationDPrime: Double,
    val intrusionRate: Double,
    val confidenceBias: Double,
    val workingMemorySpanNorm: Double,
    val totalSessions: Int,
)

/**
 * Result of inference: the archetype, a confidence in 0..1 (low early on), and a
 * runner-up — the archetype is probabilistic and revisable, never a fixed label.
 */
data class MemoryArchetypeInferenceResult(
    val archetype: MemoryArchetype,
    val confidence: Double,
    val secondGuess: MemoryArchetype?,
)

/** Infers a (revisable) archetype from behavioural signals (doc §12). */
interface ArchetypeInference {
    fun infer(signals: MemoryArchetypeSignals): MemoryArchetypeInferenceResult
}

/**
 * Default rule-based inference. Each metric-detectable archetype gets a 0..1 score;
 * the highest wins, confidence = margin × data maturity. Before enough sessions the
 * user is a low-confidence BEGINNER. DEMANDING_STUDENT and onboarding BEGINNER are
 * user-declared and never inferred here.
 */
class DefaultArchetypeInference : ArchetypeInference {

    override fun infer(signals: MemoryArchetypeSignals): MemoryArchetypeInferenceResult {
        if (signals.totalSessions < BEGINNER_SESSIONS) {
            return MemoryArchetypeInferenceResult(MemoryArchetype.BEGINNER, EARLY_CONFIDENCE, null)
        }

        val scores = scoreArchetypes(signals)
        val ranked = scores.entries.sortedByDescending { it.value }
        val top = ranked[0]
        val second = ranked[1]
        val maturity = clamp(signals.totalSessions.toDouble() / MATURE_SESSIONS)
        val confidence = ((top.value - second.value) * maturity).coerceIn(MIN_CONFIDENCE, 1.0)
        return MemoryArchetypeInferenceResult(top.key, confidence, second.key)
    }

    private fun scoreArchetypes(s: MemoryArchetypeSignals): Map<MemoryArchetype, Double> {
        val acc = s.accuracy.coerceIn(0.0, 1.0)
        val rtSlow = clamp((s.rtMedianMs - RT_FAST_MS) / (RT_SLOW_MS - RT_FAST_MS))
        val rtFast = 1.0 - rtSlow
        val forgetsFast = clamp((CONSOLIDATION_FLOOR - s.consolidationRatio) / CONSOLIDATION_FLOOR)
        val poorDiscrimination = clamp(1.0 - s.discriminationDPrime / DPRIME_GOOD)
        val intrusionHigh = clamp(s.intrusionRate / INTRUSION_HIGH)
        val biasNorm = clamp(abs(s.confidenceBias) / BIAS_SCALE)
        val lowSpan = clamp(1.0 - s.workingMemorySpanNorm)
        val consol = s.consolidationRatio.coerceIn(0.0, 1.0)
        val span = s.workingMemorySpanNorm.coerceIn(0.0, 1.0)

        return mapOf(
            MemoryArchetype.FAST_FORGETTER to forgetsFast,
            MemoryArchetype.CONFUSES_SIMILAR to mean(poorDiscrimination, intrusionHigh),
            MemoryArchetype.UNDERCONFIDENT to if (s.confidenceBias < 0) biasNorm else 0.0,
            MemoryArchetype.OVERCONFIDENT to if (s.confidenceBias > 0) biasNorm else 0.0,
            MemoryArchetype.LOW_WORKING_MEMORY to lowSpan,
            MemoryArchetype.SLOW_BUT_ACCURATE to mean(acc, rtSlow),
            MemoryArchetype.FAST_BUT_INACCURATE to mean(1.0 - acc, rtFast),
            MemoryArchetype.ADVANCED to mean(acc, consol, 1.0 - intrusionHigh),
            MemoryArchetype.ELITE to mean(acc, consol, 1.0 - biasNorm, span),
        )
    }

    private fun clamp(v: Double): Double = v.coerceIn(0.0, 1.0)
    private fun mean(vararg values: Double): Double = values.average()

    private companion object {
        const val BEGINNER_SESSIONS = 3
        const val EARLY_CONFIDENCE = 0.2
        const val MATURE_SESSIONS = 8.0
        const val MIN_CONFIDENCE = 0.05
        const val RT_FAST_MS = 800.0
        const val RT_SLOW_MS = 3_000.0
        const val CONSOLIDATION_FLOOR = 0.50
        const val DPRIME_GOOD = 2.0
        const val INTRUSION_HIGH = 0.30
        const val BIAS_SCALE = 0.40
    }
}
