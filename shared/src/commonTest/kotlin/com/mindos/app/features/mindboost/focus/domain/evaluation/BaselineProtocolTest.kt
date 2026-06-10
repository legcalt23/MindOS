package com.mindos.app.features.mindboost.focus.domain.evaluation

import com.mindos.app.features.mindboost.focus.domain.model.Mode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BaselineProtocolTest {

    private val protocol = BaselineProtocols.INITIAL

    @Test
    fun initialBaseline_lastsBetween15And20Minutes() {
        val minutes = protocol.estimatedDurationMs / 60_000.0
        assertTrue(minutes in 15.0..20.0, "Baseline should last 15–20 min, was $minutes")
    }

    @Test
    fun initialBaseline_hasCalibrationBlocksAndExactlyOneReservedProbe() {
        val probeBlocks = protocol.blocks.filter { it.isProbe }
        assertEquals(1, probeBlocks.size)
        assertTrue(probeBlocks.single().probe?.isReserved == true)
        // At least vigilance + selective + inhibition calibration blocks.
        assertTrue(protocol.blocks.count { !it.isProbe } >= 3)
    }

    @Test
    fun initialBaseline_neverAdapts() {
        assertTrue(protocol.blocks.none { it.mode == Mode.TRAINING })
        // Calibration blocks use CALIBRATION; the probe uses EVALUATION.
        assertTrue(protocol.blocks.any { it.mode == Mode.CALIBRATION })
        assertTrue(protocol.blocks.any { it.mode == Mode.EVALUATION })
    }

    @Test
    fun calibrationExercises_existInTrainingCatalogue() {
        assertEquals(3, BaselineProtocols.calibrationExercises.size)
    }

    @Test
    fun baselineBlock_rejectsBothExerciseAndProbe() {
        assertFailsWith<IllegalArgumentException> {
            BaselineBlock(
                capacity = protocol.blocks.first().capacity,
                exerciseId = protocol.blocks.first().exerciseId,
                probe = ProbeBank.VIGILANCE_PROBE,
                fixedDifficulty = protocol.blocks.first().fixedDifficulty,
                targetTrialCount = 10,
                mode = Mode.CALIBRATION,
            )
        }
    }

    @Test
    fun baselineBlock_rejectsTrainingMode() {
        assertFailsWith<IllegalArgumentException> {
            BaselineBlock(
                capacity = protocol.blocks.first().capacity,
                exerciseId = protocol.blocks.first().exerciseId,
                probe = null,
                fixedDifficulty = protocol.blocks.first().fixedDifficulty,
                targetTrialCount = 10,
                mode = Mode.TRAINING,
            )
        }
    }
}
