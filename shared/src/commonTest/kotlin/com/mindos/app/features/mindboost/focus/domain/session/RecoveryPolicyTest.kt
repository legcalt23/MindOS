package com.mindos.app.features.mindboost.focus.domain.session

import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricUnit
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricValue
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.IndividualThresholds
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.SessionId
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile
import kotlin.test.Test
import kotlin.test.assertIs

class RecoveryPolicyTest {

    private val policy = DefaultRecoveryPolicy()

    private fun profile(baselineRtCv: Double? = null) = UserCognitiveProfile(
        userId = "user",
        thresholds = IndividualThresholds(baselineRtCv = baselineRtCv),
    )

    private fun session(endedAtMs: Long, fatigue: Double? = null, rtCv: Double? = null): SessionResult {
        val metrics = buildMap {
            fatigue?.let {
                put(MetricKey.FATIGUE_INDEX, MetricValue(MetricKey.FATIGUE_INDEX, it, 6, true, MetricUnit.INDEX))
            }
            rtCv?.let {
                put(MetricKey.RT_CV, MetricValue(MetricKey.RT_CV, it, 40, true, MetricUnit.RATIO))
            }
        }
        return SessionResult(
            sessionId = SessionId("user:$endedAtMs"),
            mode = Mode.TRAINING,
            exerciseFocus = ExerciseId.E1_VIGIA,
            blocks = emptyList(),
            startedAtMs = 0,
            endedAtMs = endedAtMs,
            sessionMetrics = metrics,
        )
    }

    @Test
    fun sustainedHighFatigue_recommendsRest() {
        val recent = listOf(session(200, fatigue = 0.9), session(100, fatigue = 0.85))
        assertIs<RecoveryGate.RestRecommended>(policy.gate(profile(), recent))
    }

    @Test
    fun recentModerateFatigue_reducesVolume() {
        val recent = listOf(session(100, fatigue = 0.6))
        assertIs<RecoveryGate.Reduced>(policy.gate(profile(), recent))
    }

    @Test
    fun rtCvAboveBaseline_reducesVolume() {
        // baseline RT-CV 0.20; recent 0.40 > 0.20 * 1.5 → degraded.
        val recent = listOf(session(100, rtCv = 0.40))
        assertIs<RecoveryGate.Reduced>(policy.gate(profile(baselineRtCv = 0.20), recent))
    }

    @Test
    fun cleanRecentHistory_isFullGo() {
        val recent = listOf(session(100, fatigue = 0.2, rtCv = 0.20))
        assertIs<RecoveryGate.FullGo>(policy.gate(profile(baselineRtCv = 0.20), recent))
    }

    @Test
    fun noHistory_isFullGo() {
        assertIs<RecoveryGate.FullGo>(policy.gate(profile(), emptyList()))
    }
}
