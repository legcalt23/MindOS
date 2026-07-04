package com.mindos.app.features.mindboost.speed.domain.quality

import com.mindos.app.features.mindboost.core.antigaming.GamingAction
import com.mindos.app.features.mindboost.speed.domain.InputTimestampPrecision
import com.mindos.app.features.mindboost.speed.domain.Timebase
import com.mindos.app.features.mindboost.speed.domain.TimestampSource
import com.mindos.app.features.mindboost.speed.domain.model.TrialRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Red-team suite for [SpeedAntiGaming]: each synthetic adversary must trip its detector, and a realistic
 * honest user must trip NONE (the INFRA-03 invariant: a detector only counts if both halves hold).
 */
class SpeedAntiGamingTest {

    private val NANOS = 1_000_000L

    @Suppress("LongParameterList") // test builder mirrors the independent TrialRecord dimensions
    private fun trial(
        index: Int,
        rtMs: Long,
        responseAtMs: Long,
        difficulty: Double,
        anticipation: Boolean,
        correct: Boolean = true,
    ) =
        TrialRecord(
            id = "t$index",
            blockId = "b0",
            trialIndex = index,
            exerciseId = "E6",
            stimulusUptimeNanos = (responseAtMs - rtMs) * NANOS,
            responseUptimeNanos = responseAtMs * NANOS,
            rtNanos = rtMs * NANOS,
            isCorrect = correct,
            isAnticipation = anticipation,
            isLapse = false,
            lapseMultiplier = 1.0,
            deadlineNanos = null,
            difficultyEstimate = difficulty,
            timestampSource = TimestampSource.CHOREOGRAPHER_FRAME,
            inputTimestampPrecision = InputTimestampPrecision.NANOS_NATIVE,
            timebase = Timebase.ANDROID_UI_UPTIME_NANOS,
        )

    /** Builds a session; [gapMs] is the inter-response gap before each trial (drives the IRI cadence). */
    @Suppress("LongParameterList") // test builder mirrors the independent session dimensions
    private fun session(
        count: Int,
        rtMs: (Int) -> Long,
        gapMs: (Int) -> Long,
        difficulty: (Int) -> Double,
        anticipation: (Int) -> Boolean = { false },
        correct: (Int) -> Boolean = { true },
    ): List<TrialRecord> {
        var responseAt = 0L
        return (0 until count).map { i ->
            responseAt += gapMs(i)
            trial(i, rtMs(i), responseAt, difficulty(i), anticipation(i), correct(i))
        }
    }

    @Test
    fun rhythmicTapper_isCaughtByD1_withholdsProfile() {
        // Mechanical 1 s cadence (constant IRI) + flat RT that ignores difficulty → tapping on a clock.
        val trials = session(
            count = 14,
            rtMs = { 300 },
            gapMs = { 1000 },
            difficulty = { 0.5 }, // fixed within a block, as in a real MVP-0 run
        )
        val verdict = SpeedAntiGaming.evaluate(trials, accuracy = 0.9, chanceAccuracy = 0.5)
        assertEquals(GamingAction.WITHHOLD_PROFILE, verdict.action)
        assertFalse(verdict.isClean)
        assertTrue("D1_RHYTHM_TAPPING" in verdict.firedDetectors)
    }

    @Test
    fun speedAccuracyHack_isCaughtByD3_invalidates() {
        // Ultra-fast 150 ms median RT at chance accuracy (0.5) with varied cadence → guessing fast.
        val trials = session(
            count = 14,
            rtMs = { 150 },
            gapMs = { i -> if (i % 2 == 0) 800L else 1600L },
            difficulty = { 0.5 }, // fixed within a block, as in a real MVP-0 run
        )
        val verdict = SpeedAntiGaming.evaluate(trials, accuracy = 0.5, chanceAccuracy = 0.5)
        assertEquals(GamingAction.INVALIDATE, verdict.action)
        assertTrue("D3_SPEED_ACCURACY_HACK" in verdict.firedDetectors)
    }

    @Test
    fun tooFast_isCaughtByD4_withholdsProfile() {
        // Half the block is anticipations (> 0.30 floor) with otherwise normal RT/accuracy/cadence.
        val trials = session(
            count = 14,
            rtMs = { 300 },
            gapMs = { i -> if (i % 2 == 0) 800L else 1600L },
            difficulty = { 0.5 }, // fixed within a block, as in a real MVP-0 run
            anticipation = { i -> i % 2 == 0 },
        )
        val verdict = SpeedAntiGaming.evaluate(trials, accuracy = 0.9, chanceAccuracy = 0.5)
        assertEquals(GamingAction.WITHHOLD_PROFILE, verdict.action)
        assertTrue("D4_TOO_FAST" in verdict.firedDetectors)
    }

    @Test
    fun uniformFastGuessing_atChanceButMedianAbove250_escapesD3_isCaughtByChanceGate() {
        // Uniform ~260 ms responses at chance accuracy: the median is ABOVE the 250 ms floor, so D3 stays
        // idle (its exploit band). The threshold-free chance gate catches it — a block at chance carries no
        // valid speed signal, however fast the (≥250 ms) responses are.
        val trials = session(
            count = 14,
            rtMs = { 260 },
            gapMs = { i -> if (i % 2 == 0) 800L else 1600L }, // varied cadence → D1 stays idle
            difficulty = { 0.5 },
        )
        val verdict = SpeedAntiGaming.evaluate(trials, accuracy = 0.5, chanceAccuracy = 0.5)
        assertEquals(GamingAction.INVALIDATE, verdict.action)
        assertTrue("SPEED_CHANCE_GUESSING" in verdict.firedDetectors)
        assertFalse("D3_SPEED_ACCURACY_HACK" in verdict.firedDetectors, "median 260 ms is above D3's 250 ms floor")
    }

    @Test
    fun alternatingFastSlow_holdingMedianAbove250_atChance_isCaughtByChanceGate() {
        // Alternating 150/450 ms holds the median at ~300 ms (≥ 250 → D3 idle), still at chance accuracy.
        val trials = session(
            count = 14,
            rtMs = { i -> if (i % 2 == 0) 150L else 450L },
            gapMs = { i -> if (i % 2 == 0) 800L else 1600L },
            difficulty = { 0.5 },
        )
        val verdict = SpeedAntiGaming.evaluate(trials, accuracy = 0.5, chanceAccuracy = 0.5)
        assertEquals(GamingAction.INVALIDATE, verdict.action)
        assertTrue("SPEED_CHANCE_GUESSING" in verdict.firedDetectors)
        assertFalse("D3_SPEED_ACCURACY_HACK" in verdict.firedDetectors, "the median is held above D3's floor")
    }

    @Test
    fun fastButAccurateReactor_isNotFlagged_noFalsePositive() {
        // The critical FP guard (mirrors the §15 P1-humano run: ~177 ms at high accuracy). Fast AND accurate
        // beats chance comfortably → NO detector fires, so a genuine fast reactor is never penalized.
        val trials = session(
            count = 14,
            rtMs = { 177 },
            gapMs = { i -> if (i % 2 == 0) 800L else 1600L },
            difficulty = { 0.5 },
        )
        val verdict = SpeedAntiGaming.evaluate(trials, accuracy = 0.92, chanceAccuracy = 0.5)
        assertEquals(GamingAction.NONE, verdict.action)
        assertTrue(verdict.isClean)
        assertTrue(verdict.firedDetectors.isEmpty(), "a fast accurate reactor must trip nothing: ${verdict.firedDetectors}")
    }

    @Test
    fun honestUser_tripsNoDetector_isClean() {
        // Varied cadence (random foreperiods keep IRI variance well above the rhythm floor), normal RT well
        // above the 250 ms floor, no anticipations, good accuracy — a fixed-difficulty block (production).
        val trials = session(
            count = 14,
            rtMs = { i -> (320 + (i % 4) * 40).toLong() },
            gapMs = { i -> if (i % 2 == 0) 800L else 1600L },
            difficulty = { 0.5 }, // fixed within a block, as in a real MVP-0 run
        )
        val verdict = SpeedAntiGaming.evaluate(trials, accuracy = 0.9, chanceAccuracy = 0.5)
        assertEquals(GamingAction.NONE, verdict.action)
        assertTrue(verdict.isClean)
        assertTrue(verdict.firedDetectors.isEmpty(), "honest play must trip no detector: ${verdict.firedDetectors}")
    }

    @Test
    fun partialFastGuesser_aboveChanceOverall_isCaught_withholdsProfile() {
        // The residual band the whole-block gates leave open: half the trials are fast ~150 ms guesses at
        // coin-flip accuracy, the other half are genuine 420 ms answers (all correct). Overall accuracy 0.75
        // stays ABOVE chance (chance gate idle) and the median is dragged to 285 ms (≥ 250 → D3 idle) —
        // exactly the partial fast-guessing signature the new detector must catch.
        val trials = session(
            count = 24,
            rtMs = { i -> if (i % 2 == 0) 150L else 420L },
            gapMs = { i -> if (i % 2 == 0) 800L else 1600L },
            difficulty = { 0.5 },
            correct = { i -> i % 2 != 0 || (i / 2) % 2 == 0 }, // fast half: 6/12 correct; slow half: 12/12
        )
        val verdict = SpeedAntiGaming.evaluate(trials, accuracy = 0.75, chanceAccuracy = 0.5)
        assertEquals(GamingAction.WITHHOLD_PROFILE, verdict.action)
        assertTrue("SPEED_PARTIAL_FAST_GUESSING" in verdict.firedDetectors)
        assertFalse("SPEED_CHANCE_GUESSING" in verdict.firedDetectors, "overall 0.75 is above chance")
        assertFalse("D3_SPEED_ACCURACY_HACK" in verdict.firedDetectors, "median 285 ms is above D3's floor")
    }

    @Test
    fun honestFastAndAccurate_largeBlock_isClean_speedAloneNeverAccuses() {
        // Fast (180–225 ms) AND accurate (23/24): the fast half beats the chance bound comfortably, so the
        // partial gate stays idle — being fast alone must never accuse (the CEO anti-FP rule).
        val trials = session(
            count = 24,
            rtMs = { i -> (180 + (i % 4) * 15).toLong() },
            gapMs = { i -> if (i % 2 == 0) 800L else 1600L },
            difficulty = { 0.5 },
            correct = { i -> i != 7 }, // a single miss
        )
        val verdict = SpeedAntiGaming.evaluate(trials, accuracy = 23 / 24.0, chanceAccuracy = 0.5)
        assertTrue(verdict.isClean, "a fast accurate player must trip nothing: ${verdict.firedDetectors}")
    }

    @Test
    fun honestSpeedAccuracyTradeoff_isClean() {
        // A genuine tradeoff: the fast half is somewhat less accurate (10/12 ≈ 0.83) but far above coin-flip.
        // The partial gate demands CHANCE-consistency in the fast half, so a normal tradeoff never fires it.
        val trials = session(
            count = 24,
            rtMs = { i -> if (i % 2 == 0) 200L else 420L },
            gapMs = { i -> if (i % 2 == 0) 800L else 1600L },
            difficulty = { 0.5 },
            correct = { i -> i != 0 && i != 2 }, // 2 misses among the 12 fast trials
        )
        val verdict = SpeedAntiGaming.evaluate(trials, accuracy = 22 / 24.0, chanceAccuracy = 0.5)
        assertTrue(verdict.isClean, "an honest tradeoff must trip nothing: ${verdict.firedDetectors}")
    }

    @Test
    fun partialPattern_belowFastSampleFloor_isInconclusive_neverAccuses() {
        // The same partial signature but only 14 trials (fast half = 7 < 10): a tiny fast subset is not
        // evidence, so the detector idles and the block stays clean — inconclusive, never contaminating.
        val trials = session(
            count = 14,
            rtMs = { i -> if (i % 2 == 0) 150L else 420L },
            gapMs = { i -> if (i % 2 == 0) 800L else 1600L },
            difficulty = { 0.5 },
            correct = { i -> i % 2 != 0 || (i / 2) % 2 == 0 }, // fast: 4/7 correct; slow: 7/7
        )
        val verdict = SpeedAntiGaming.evaluate(trials, accuracy = 11 / 14.0, chanceAccuracy = 0.5)
        assertTrue(verdict.isClean, "insufficient fast sample must stay clean: ${verdict.firedDetectors}")
    }

    @Test
    fun simpleRtConvention_noChanceConcept_chanceGatesStayIdle() {
        // E1 single-tap semantics: accuracy 1.0 + chanceAccuracy 0.0 ("no chance concept") structurally idle
        // every chance-based gate even with uniformly fast RTs; cadence/anticipation protection stays live.
        val trials = session(
            count = 24,
            rtMs = { 150 },
            gapMs = { i -> if (i % 2 == 0) 800L else 1600L },
            difficulty = { 0.5 },
        )
        val verdict = SpeedAntiGaming.evaluate(trials, accuracy = 1.0, chanceAccuracy = 0.0)
        assertTrue(verdict.isClean, "a simple-RT task must never be accused by chance logic: ${verdict.firedDetectors}")
    }

    @Test
    fun belowMinTrials_isClean_noRateAssessmentOnTinySamples() {
        // A short/partial block (< MIN_TRIALS_FOR_DETECTION): even 50% anticipations + fast chance responses
        // must NOT flag — a rate/median over a tiny sample is not evidence of gaming (FP guard).
        val trials = session(
            count = 6,
            rtMs = { 150 },
            gapMs = { 800 },
            difficulty = { 0.5 },
            anticipation = { i -> i % 2 == 0 },
        )
        val verdict = SpeedAntiGaming.evaluate(trials, accuracy = 0.5, chanceAccuracy = 0.5)
        assertEquals(GamingAction.NONE, verdict.action)
        assertTrue(verdict.isClean)
    }
}
