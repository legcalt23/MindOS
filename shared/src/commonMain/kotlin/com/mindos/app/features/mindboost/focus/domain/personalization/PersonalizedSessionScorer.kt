package com.mindos.app.features.mindboost.focus.domain.personalization

import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricSet

/**
 * An honest, archetype-personalized performance score for a single session (0..100). It is NOT the
 * composite Focus Score (which is archetype-neutral); it is the per-session reward signal, bent by the
 * user's [ScoringModifiers] so the system never incentivizes the wrong behaviour:
 *
 * - [ScoringModifiers.speedIncentive] < 1.0 → speed is rewarded LESS (impulsive / fast-but-inaccurate),
 *   so a fast-but-careless session is not praised for haste ("no premia RT corto", 08_ §6).
 * - [ScoringModifiers.errorPenalty] > 1.0 → accuracy weighs more, so errors cost more.
 * - [ScoringModifiers.lapsePenalty] > 1.0 → lapses subtract more (low-concentration).
 *
 * The score is gated by confidence like every other number here: too few trials → no value, shown as
 * provisional. The caption is non-diagnostic (08_ §5): it never names or labels the archetype.
 */
data class PersonalizedScore(
    val value: Double?,
    val confident: Boolean,
    val caption: String,
    /** True when speed was down-weighted, so the score prizes control over haste. */
    val deemphasizesSpeed: Boolean = false,
)

/** Computes a [PersonalizedScore] from a session's metrics and the archetype's [ScoringModifiers]. */
interface PersonalizedSessionScorer {
    fun score(metrics: MetricSet, modifiers: ScoringModifiers): PersonalizedScore
}

/**
 * Default scorer. Combines the available honest components — accuracy, speed (from RT median) and
 * stability (from RT-CV) — as a weighted average whose weights are bent by the modifiers, then subtracts
 * a lapse penalty. Accuracy is required; speed/stability contribute only when confidently available.
 */
class DefaultPersonalizedSessionScorer : PersonalizedSessionScorer {

    override fun score(metrics: MetricSet, modifiers: ScoringModifiers): PersonalizedScore {
        val accuracy = confidentValue(metrics, MetricKey.ACCURACY)
            ?: return notEnoughData(modifiers)
        val n = metrics[MetricKey.ACCURACY]?.n ?: 0

        val components = buildComponents(metrics, accuracy)
        val weights = weightsFor(modifiers)
        val weighted = components.sumOf { weights.getValue(it.kind) * it.value }
        val totalWeight = components.sumOf { weights.getValue(it.kind) }
        val base = if (totalWeight > 0.0) weighted / totalWeight else accuracy

        val lapses = metrics[MetricKey.LAPSES]?.value ?: 0.0
        val lapsePenalty = (lapses * PER_LAPSE_COST * modifiers.lapsePenalty).coerceIn(0.0, MAX_LAPSE_PENALTY)
        val score01 = (base - lapsePenalty).coerceIn(0.0, 1.0)

        val confident = n >= MIN_N
        val deemphasizesSpeed = modifiers.speedIncentive < 1.0
        return PersonalizedScore(
            value = score01 * PERCENT,
            confident = confident,
            caption = caption(confident, deemphasizesSpeed),
            deemphasizesSpeed = deemphasizesSpeed,
        )
    }

    private enum class ComponentKind { ACCURACY, SPEED, STABILITY }
    private data class Component(val kind: ComponentKind, val value: Double)

    private fun buildComponents(metrics: MetricSet, accuracy: Double): List<Component> = buildList {
        add(Component(ComponentKind.ACCURACY, accuracy))
        confidentValue(metrics, MetricKey.RT_MEDIAN)?.let { rt ->
            val speed = ((RT_SLOW_MS - rt) / (RT_SLOW_MS - RT_FAST_MS)).coerceIn(0.0, 1.0)
            add(Component(ComponentKind.SPEED, speed))
        }
        confidentValue(metrics, MetricKey.RT_CV)?.let { cv ->
            val stability = 1.0 - (cv / RT_CV_CAP).coerceIn(0.0, 1.0)
            add(Component(ComponentKind.STABILITY, stability))
        }
    }

    private fun weightsFor(modifiers: ScoringModifiers): Map<ComponentKind, Double> = mapOf(
        // Errors hurt more → accuracy matters more; speed rewarded by the incentive (≤1 dampens haste).
        ComponentKind.ACCURACY to W_ACCURACY * modifiers.errorPenalty,
        ComponentKind.SPEED to W_SPEED * modifiers.speedIncentive,
        ComponentKind.STABILITY to W_STABILITY,
    )

    private fun confidentValue(metrics: MetricSet, key: MetricKey): Double? =
        metrics[key]?.takeIf { it.confident }?.value

    private fun notEnoughData(modifiers: ScoringModifiers) = PersonalizedScore(
        value = null,
        confident = false,
        caption = "Estimación preliminar — pocos datos todavía.",
        deemphasizesSpeed = modifiers.speedIncentive < 1.0,
    )

    private fun caption(confident: Boolean, deemphasizesSpeed: Boolean): String = when {
        !confident -> "Estimación preliminar — pocos datos todavía."
        deemphasizesSpeed -> "Rendimiento de hoy — prioriza el control sobre la prisa."
        else -> "Rendimiento de hoy en este ejercicio."
    }

    private companion object {
        const val PERCENT = 100.0
        const val MIN_N = 20
        // Speed normalization: 250 ms (very fast) → 1.0, 800 ms (slow) → 0.0.
        const val RT_FAST_MS = 250.0
        const val RT_SLOW_MS = 800.0
        const val RT_CV_CAP = 0.5
        const val W_ACCURACY = 0.5
        const val W_SPEED = 0.2
        const val W_STABILITY = 0.3
        const val PER_LAPSE_COST = 0.03
        const val MAX_LAPSE_PENALTY = 0.3
    }
}
