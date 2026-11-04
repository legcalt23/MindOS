package com.mindos.app.features.mindboost.focus.domain.evaluation

import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.model.BlockId
import com.mindos.app.features.mindboost.focus.domain.model.BlockResult
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyAxis
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The pure baseline fold (06_ §2–§3) must personalize thresholds from the user's own RT distribution,
 * seed a per-exercise starting difficulty in the productive zone, and store the reserved-probe metrics
 * as the pre-snapshot — without any Compose/timing.
 */
class BaselineAssemblerTest {

    private val assembler = BaselineAssembler(DefaultCalibrationEngine())

    @Test
    fun assemble_personalizesThresholds_seedsDifficulty_andStoresProbeSnapshot() {
        val profile = UserCognitiveProfile(userId = "u1")
        // 10 fast, consistent HITs → personalized lapse cutoff + a baseline RT-CV; accuracy 1.0 → hard start.
        val calibrationBlock = BlockResult(
            blockId = BlockId("u1:b0"),
            exerciseId = ExerciseId.E1_VIGIA,
            mode = Mode.CALIBRATION,
            trials = FocusTestFixtures.hits(listOf(300, 310, 320, 330, 340, 350, 360, 370, 380, 390)),
            difficultyStart = DifficultyVector.EASIEST,
            difficultyEnd = DifficultyVector.EASIEST,
            // A CONFIDENT calibration metric, deliberately absent from the probe block. The snapshot is
            // the `pre` side of a capacity claim and must come from the reserved probe alone; a training
            // block's metric leaking in would carry a sample nobody sized for a claim (the baseline E2
            // block's congruency cell is 12 against a floor of 20 — see MB-CATALOG-0015). With
            // `metrics = emptyMap()` this test could not tell "probe only" from "probe merged with
            // calibration"; it can now.
            metrics = FocusTestFixtures.metricSet(MetricKey.CONGRUENCY_EFFECT to 42.0),
        )
        val probeBlock = BlockResult(
            blockId = BlockId("u1:probe"),
            exerciseId = ExerciseId.E1_VIGIA,
            mode = Mode.EVALUATION,
            trials = emptyList(),
            difficultyStart = DifficultyVector.EASIEST,
            difficultyEnd = DifficultyVector.EASIEST,
            metrics = FocusTestFixtures.metricSet(
                MetricKey.RT_MEDIAN to 350.0,
                MetricKey.RT_CV to 0.2,
            ),
        )

        val outcome = assembler.assemble(
            profile = profile,
            calibrationBlocks = listOf(calibrationBlock),
            probeBlock = probeBlock,
            capturedAtMs = 5_000L,
        )

        // Personalized thresholds derived from the calibration RTs.
        assertNotNull(outcome.profile.thresholds.baselineRtCv, "RT-CV baseline must be personalized.")
        assertTrue(outcome.profile.thresholds.lapseRtMs >= 500L, "Lapse cutoff never below the PVT floor.")
        // Accuracy 1.0 → hardest calibration level on the governing perceptual-load axis.
        val started = outcome.profile.startingDifficulty[ExerciseId.E1_VIGIA]
        assertNotNull(started)
        assertTrue(
            started.level(DifficultyAxis.PERCEPTUAL_LOAD).value > 0.0,
            "High accuracy seeds a non-trivial start.",
        )
        // The reserved-probe metrics become the pre-snapshot.
        assertEquals(350.0, outcome.snapshot.perMetric[MetricKey.RT_MEDIAN])
        // …and ONLY the probe's. The calibration block's confident metric must not reach the snapshot:
        // the `pre` side comes from the reserved probe, never from a trained block. (The probe is a
        // parallel form of a trained paradigm, so it grounds no capacity claim either — `MB-FOCUS-0089`.)
        assertNull(
            outcome.snapshot.perMetric[MetricKey.CONGRUENCY_EFFECT],
            "A calibration-block metric leaked into the pre-snapshot.",
        )
        assertEquals(outcome.snapshot, outcome.profile.baseline)
        assertEquals(5_000L, outcome.profile.updatedAtMs)
    }

    @Test
    fun assemble_requiresAtLeastOneCalibrationBlock() {
        val profile = UserCognitiveProfile(userId = "u1")
        val probeBlock = BlockResult(
            blockId = BlockId("u1:probe"),
            exerciseId = ExerciseId.E1_VIGIA,
            mode = Mode.EVALUATION,
            trials = emptyList(),
            difficultyStart = DifficultyVector.EASIEST,
            difficultyEnd = DifficultyVector.EASIEST,
            metrics = emptyMap(),
        )
        assertFailsWith<IllegalArgumentException>("A baseline must contain at least one calibration block.") {
            assembler.assemble(profile, emptyList(), probeBlock, capturedAtMs = 1L)
        }
    }
}
