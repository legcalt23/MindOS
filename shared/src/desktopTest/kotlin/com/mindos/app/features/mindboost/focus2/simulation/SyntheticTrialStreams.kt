package com.mindos.app.features.mindboost.focus2.simulation

import com.mindos.app.features.mindboost.core.runtime.DeterministicRng
import com.mindos.app.features.mindboost.core.runtime.RngVersion
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.focus2.domain.scoring.ConditionId
import com.mindos.app.features.mindboost.focus2.domain.scoring.Response
import com.mindos.app.features.mindboost.focus2.domain.scoring.SecondaryTiming
import com.mindos.app.features.mindboost.focus2.domain.scoring.TaskSpec
import com.mindos.app.features.mindboost.focus2.domain.scoring.TrialRecord

/**
 * Focus V5 · S2 · tarea (5) — the adversarial profiles, as BEHAVIOUR.
 *
 * This file is the actual repair of `MB-FOCUS-0007`. The v1 oracle
 * (`FocusV1ScoreAdversarialTest`) built each adversarial profile as a hand-written `MetricSet` with
 * `confident = true` on every value and the decisive `lapses = 20.0` asserted by the test author rather than
 * produced by any engine. It imported no trial type at all. That makes it a FORMULA oracle wearing the label
 * of a BEHAVIOUR oracle: it can prove "given these numbers, the formula returns those numbers", which is not
 * the question. The question is whether someone who mashes one key outscores someone who genuinely filters.
 *
 * Here nothing hand-writes an aggregate. A profile is a description of how a person BEHAVES, it is turned
 * into a stream of individual trials, and the scorer derives every count from that stream itself. If the
 * counts are wrong, the scorer is wrong — there is no author-supplied number for it to hide behind.
 *
 * The v1 file is deliberately left untouched. Its inverted assertions are accurate documentation of the v1
 * defect, which is what it says it is; editing it would destroy the evidence that the defect existed. Nothing
 * is reused from it — least of all its pinned literals, which are v1-formula-specific.
 */
object SyntheticTrialStreams {

    /**
     * Trial counts, raised from the ratified n=55/profile.
     *
     * **This is a correction, not a deviation.** The oracle header specifies 55 trials per profile
     * (`agent_outputs/03_psychometrician.md:112-113`), which was written for a session-level statistic. Task
     * (5) mandates going PER CONDITION, and `MinSampleGate.specFor(DPRIME).nMinPerCell` is 20. Splitting 55
     * across 2 conditions x 2 (signal/noise) gives ~14 per cell — below the floor — so every cell would
     * return `Absent(INSUFFICIENT_N)` and the ordering assertions would compare nothing against nothing and
     * pass. The simulation would report green having never produced a single number.
     *
     * So the counts are raised until every cell clears the floor with margin. The consequence follows from
     * task (5)'s own mandate; it is flagged to the CEO rather than absorbed silently.
     */
    const val E07_TRIALS: Int = 112

    /** 24 signal (20% target rate) + 96 noise. 24 >= 20, floor cleared. */
    const val E01_TRIALS: Int = 120

    val E07_SPEC = TaskSpec(
        taskId = "FOC2-E07",
        conditions = listOf(ConditionId.CONGRUENT, ConditionId.INCONGRUENT),
        // LEFT is the "yes" response by convention. See the note on the 2AFC shortcut below.
        yesResponse = Response.LEFT,
        noResponse = Response.RIGHT,
        chanceLevel = 0.5,
        primaryCondition = ConditionId.INCONGRUENT,
    )

    val E01_SPEC = TaskSpec(
        taskId = "FOC2-E01",
        conditions = listOf(ConditionId.VIGILANCE),
        yesResponse = Response.TAP,
        // In a go-only task the absence of a response IS the "no". Silence on a non-target is a correct
        // rejection, not an omission — not responding is the norm in this paradigm.
        noResponse = Response.NONE,
        // A go-only vigilance task has NO well-defined guessing probability: the chance of a correct "no"
        // depends on how willing the person is to respond at all, which is exactly what criterion measures.
        // Fabricating a value here would invent a denominator, so the branch is structurally disabled.
        chanceLevel = null,
        primaryCondition = ConditionId.VIGILANCE,
    )

    enum class Profile { HONEST, EXPERT, MASHER, WAITER, SIDE_BIAS }

    private const val E07_WINDOW_MS = 1200L
    private const val E01_WINDOW_MS = 1500L

    /**
     * Builds the flanker stream for [profile].
     *
     * **Why the yes/no framing and not the 2AFC shortcut.** A choice task invites `d' = sqrt(2) * z(pc)`.
     * That formula assumes an UNBIASED observer — precisely the assumption SIDE_BIAS violates — and it folds
     * bias into sensitivity, so a person who always answers LEFT would appear to have poor discrimination
     * rather than an extreme bias. Under the LEFT-as-yes convention the two are separated: d' carries
     * discrimination and `criterion` carries the side bias, which is the quantity assertion A3 needs.
     */
    fun e07(profile: Profile, seed: Long = DEFAULT_SEED): List<TrialRecord> {
        val rng = DeterministicRng.forSeed(SessionSeed(seed, RngVersion.V1))
        val perCondition = E07_TRIALS / 2
        return E07_SPEC.conditions.flatMapIndexed { conditionIndex, condition ->
            (0 until perCondition).map { i ->
                // Half the trials in each condition carry a left target, half a right target.
                val isSignal = i < perCondition / 2
                val index = conditionIndex * perCondition + i
                e07Trial(profile, condition, isSignal, index, rng)
            }
        }
    }

    private fun e07Trial(
        profile: Profile,
        condition: ConditionId,
        isSignal: Boolean,
        index: Int,
        rng: DeterministicRng,
    ): TrialRecord {
        val correctSide = if (isSignal) Response.LEFT else Response.RIGHT
        val wrongSide = if (isSignal) Response.RIGHT else Response.LEFT
        val incongruent = condition == ConditionId.INCONGRUENT
        val (response, rtMs) = when (profile) {
            // Genuinely filters. Slightly worse and slower under interference, which is the signature of a
            // real flanker effect — and it stays DESCRIPTIVE: the difference is never the governing metric.
            Profile.HONEST -> {
                val pCorrect = if (incongruent) 0.90 else 0.94
                val side = if (rng.nextDouble() < pCorrect) correctSide else wrongSide
                side to (if (incongruent) 560L else 505L) + rng.nextInt(-40, 40)
            }
            // Near ceiling and fast, but NOT metronomic: latency still tracks the condition. Must not be
            // penalised for being good (red-team row 4 / row 36).
            Profile.EXPERT -> {
                val pCorrect = if (incongruent) 0.97 else 0.98
                val side = if (rng.nextDouble() < pCorrect) correctSide else wrongSide
                side to (if (incongruent) 420L else 385L) + rng.nextInt(-30, 30)
            }
            // Ignores the stimulus entirely: one key, always, fast and near-constant.
            Profile.MASHER -> Response.LEFT to 250L + rng.nextInt(-5, 5)
            // Rides the deadline. Correct side is a coin flip and a slice of responses land past the window,
            // which in a mandatory-response task is an OMISSION, not a wrong answer.
            Profile.WAITER -> {
                val side = if (rng.nextBoolean()) correctSide else wrongSide
                val late = rng.nextDouble() < 0.15
                if (late) Response.NONE to E07_WINDOW_MS + 50L else side to E07_WINDOW_MS - rng.nextInt(1, 60)
            }
            // READS the stimulus but always answers LEFT. Indistinguishable from MASHER in the 2x2 — and
            // that is correct, see the note in the test: only latency separates them, and the primary
            // statistic must not consume latency.
            Profile.SIDE_BIAS -> Response.LEFT to (if (incongruent) 495L else 470L) + rng.nextInt(-25, 25)
        }
        return TrialRecord(
            index = index,
            conditionId = condition,
            isSignal = isSignal,
            response = response,
            timing = SecondaryTiming(rtMs, E07_WINDOW_MS),
        )
    }

    /** Builds the vigilance stream for [profile]. */
    fun e01(profile: Profile, seed: Long = DEFAULT_SEED): List<TrialRecord> {
        require(profile != Profile.SIDE_BIAS) {
            "SIDE_BIAS does not exist for FOC2-E01: a go-only single-tap task has ONE response, so there is " +
                "no side to be biased toward. Inventing a variant would fabricate an adversary the paradigm " +
                "cannot have. Four of the five profiles run on E01, and the report says so."
        }
        val rng = DeterministicRng.forSeed(SessionSeed(seed, RngVersion.V1))
        val signalCount = E01_TRIALS / 5 // 20% target rate: sparse targets ARE the paradigm.
        return (0 until E01_TRIALS).map { index ->
            val isSignal = index % 5 == 0
            val (hitRate, faRate) = when (profile) {
                Profile.HONEST -> 0.92 to 0.06
                Profile.EXPERT -> 0.97 to 0.02
                Profile.MASHER -> 1.0 to 1.0 // taps at everything
                Profile.WAITER -> 0.45 to 0.45
                Profile.SIDE_BIAS -> error("unreachable: guarded above")
            }
            val responds = rng.nextDouble() < (if (isSignal) hitRate else faRate)
            TrialRecord(
                index = index,
                conditionId = ConditionId.VIGILANCE,
                isSignal = isSignal,
                response = if (responds) Response.TAP else Response.NONE,
                timing = if (responds) SecondaryTiming(e01Rt(profile, rng), E01_WINDOW_MS) else null,
            ).also { check(signalCount > 0) }
        }
    }

    private fun e01Rt(profile: Profile, rng: DeterministicRng): Long = when (profile) {
        Profile.HONEST -> 480L + rng.nextInt(-80, 170)
        Profile.EXPERT -> 400L + rng.nextInt(-50, 90)
        Profile.MASHER -> 250L + rng.nextInt(-5, 5)
        Profile.WAITER -> E01_WINDOW_MS - rng.nextInt(1, 60)
        Profile.SIDE_BIAS -> error("unreachable: guarded above")
    }

    /** Fixed so every run of the suite sees the identical stream. Pinned algorithm, pinned seed. */
    const val DEFAULT_SEED: Long = 0x5EEDF0C25L
}
