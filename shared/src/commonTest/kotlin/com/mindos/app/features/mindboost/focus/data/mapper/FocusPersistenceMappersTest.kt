package com.mindos.app.features.mindboost.focus.data.mapper

import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.data.dto.BaselineSnapshotDto
import com.mindos.app.features.mindboost.focus.data.dto.SessionSummaryDto
import com.mindos.app.features.mindboost.focus.data.dto.UserCognitiveProfileDto
import com.mindos.app.features.mindboost.focus.data.local.FocusJson
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricUnit
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricValue
import com.mindos.app.features.mindboost.focus.domain.model.AxisLevel
import com.mindos.app.features.mindboost.focus.domain.model.BaselineSnapshot
import com.mindos.app.features.mindboost.focus.domain.model.BlockId
import com.mindos.app.features.mindboost.focus.domain.model.BlockResult
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyAxis
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.IndividualThresholds
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.SessionId
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import com.mindos.app.features.mindboost.focus.domain.model.UserArchetype
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FocusPersistenceMappersTest {

    private val json = FocusJson.instance

    @Test
    fun userProfile_roundTripsThroughJson() {
        val profile = UserCognitiveProfile(
            userId = "user-1",
            archetype = UserArchetype.IMPULSIVE,
            archetypeConfidence = 0.66,
            thresholds = IndividualThresholds(lapseRtMs = 540, anticipationFloorMs = 150, baselineRtCv = 0.21),
            baseline = BaselineSnapshot(
                userId = "user-1",
                perMetric = mapOf(MetricKey.ACCURACY to 0.82, MetricKey.RT_MEDIAN to 305.0),
                capturedAtMs = 1_000,
                deviceFingerprint = "dev-x",
            ),
            startingDifficulty = mapOf(
                ExerciseId.E1_VIGIA to DifficultyVector.EASIEST.adjust(DifficultyAxis.PERCEPTUAL_LOAD, AxisLevel(0.4)),
            ),
            cycleWeek = 3,
            totalSessions = 7,
            updatedAtMs = 2_000,
        )

        val encoded = json.encodeToString(profile.toDto())
        val decoded = json.decodeFromString<UserCognitiveProfileDto>(encoded).toDomain()

        assertEquals(profile, decoded)
        assertTrue(encoded.contains("\"schemaVersion\":1"))
    }

    @Test
    fun baseline_roundTripsThroughJson() {
        val baseline = BaselineSnapshot(
            userId = "user-2",
            perMetric = mapOf(MetricKey.RT_CV to 0.18, MetricKey.LAPSES to 2.0),
            capturedAtMs = 5_000,
            deviceFingerprint = null,
        )
        val decoded = json.decodeFromString<BaselineSnapshotDto>(json.encodeToString(baseline.toDto())).toDomain()
        assertEquals(baseline, decoded)
    }

    @Test
    fun session_persistsSummary_andDropsRawTrials() {
        val metric = MetricValue(MetricKey.ACCURACY, 0.9, n = 40, confident = true, unit = MetricUnit.PROPORTION)
        val block = BlockResult(
            blockId = BlockId("b0"),
            exerciseId = ExerciseId.E1_VIGIA,
            mode = Mode.TRAINING,
            trials = FocusTestFixtures.hits(listOf(300, 310, 305)), // 3 raw trials present
            difficultyStart = DifficultyVector.EASIEST,
            difficultyEnd = DifficultyVector.EASIEST,
            metrics = mapOf(MetricKey.ACCURACY to metric),
        )
        val session = SessionResult(
            sessionId = SessionId("user:9000"),
            mode = Mode.TRAINING,
            exerciseFocus = ExerciseId.E1_VIGIA,
            blocks = listOf(block),
            startedAtMs = 0,
            endedAtMs = 9_000,
            sessionMetrics = mapOf(MetricKey.ACCURACY to metric),
        )

        val decoded = json.decodeFromString<SessionSummaryDto>(json.encodeToString(session.toSummaryDto())).toDomain()

        // Persisted summary fields survive…
        assertEquals(session.sessionId, decoded.sessionId)
        assertEquals(session.endedAtMs, decoded.endedAtMs)
        assertEquals(session.sessionMetrics, decoded.sessionMetrics)
        assertEquals(block.metrics, decoded.blocks.single().metrics)
        assertEquals(block.difficultyEnd, decoded.blocks.single().difficultyEnd)
        // …but raw trials are intentionally not persisted.
        assertTrue(decoded.blocks.single().trials.isEmpty())
    }
}
