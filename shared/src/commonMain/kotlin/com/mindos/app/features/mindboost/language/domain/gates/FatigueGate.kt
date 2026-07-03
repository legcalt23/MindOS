package com.mindos.app.features.mindboost.language.domain.gates

import com.mindos.app.features.mindboost.language.domain.model.FatigueState
import com.mindos.app.features.mindboost.language.domain.model.Proportion
import kotlinx.serialization.Serializable

/**
 * Fatigue assessment (v6 §4.4, §6.3). Computes the [FatigueState] that scoring/adaptation
 * consume. High fatigue forbids strong profile updates and lowers working-memory load — it is
 * weighed above engagement (§1.4 anti-caos: SafetyGate > engagement).
 */
fun interface FatigueGate {
    fun assess(signals: FatigueSignals): FatigueState
}

/**
 * Signals available at decision time. [recentLatenciesMs] is chronological; rising latency
 * across the session is a fatigue marker even when accuracy holds (§4.4).
 */
@Serializable
data class FatigueSignals(
    val recentLatenciesMs: List<Long> = emptyList(),
    val itemsCompleted: Int = 0,
    val sessionElapsedMs: Long = 0,
    val recentErrorRate: Double = 0.0,
)

/**
 * Default [FatigueGate]. Pure. Blends progress, time-on-task, rising-latency and recent error
 * rate into a 0..1 level; [FatigueState.isHigh] (>= 0.7) is what gates strong updates.
 */
class DefaultFatigueGate : FatigueGate {

    override fun assess(signals: FatigueSignals): FatigueState {
        val rising = isLatencyRising(signals.recentLatenciesMs)
        val itemsFraction = (signals.itemsCompleted / ITEM_CAP).coerceIn(0.0, 1.0)
        val timeFraction = (signals.sessionElapsedMs / TIME_CAP_MS).coerceIn(0.0, 1.0)
        val level = (
            W_ITEMS * itemsFraction +
                W_TIME * timeFraction +
                W_LATENCY * (if (rising) 1.0 else 0.0) +
                W_ERROR * signals.recentErrorRate.coerceIn(0.0, 1.0)
            ).coerceIn(0.0, 1.0)
        return FatigueState(Proportion(level), risingLatency = rising)
    }

    private fun isLatencyRising(latencies: List<Long>): Boolean {
        if (latencies.size < MIN_LATENCY_SAMPLES) return false
        val third = latencies.size / LATENCY_WINDOW_THIRDS
        val first = median(latencies.take(third))
        val last = median(latencies.takeLast(third))
        return last > first * LATENCY_RISE_FACTOR
    }

    private fun median(values: List<Long>): Double {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / EVEN_MEDIAN_DIVISOR
        } else {
            sorted[mid].toDouble()
        }
    }

    companion object {
        const val W_ITEMS = 0.35
        const val W_TIME = 0.25
        const val W_LATENCY = 0.20
        const val W_ERROR = 0.20

        const val ITEM_CAP = 40.0
        const val TIME_CAP_MS = 1_200_000.0 // 20 min
        const val MIN_LATENCY_SAMPLES = 6
        const val LATENCY_RISE_FACTOR = 1.15
        const val EVEN_MEDIAN_DIVISOR = 2.0
        const val LATENCY_WINDOW_THIRDS = 3
    }
}
