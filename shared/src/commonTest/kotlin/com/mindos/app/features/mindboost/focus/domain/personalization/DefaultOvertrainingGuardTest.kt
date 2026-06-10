package com.mindos.app.features.mindboost.focus.domain.personalization

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

class DefaultOvertrainingGuardTest {

    private val guard = DefaultOvertrainingGuard()

    private fun profile(baselineRtCv: Double? = null) = UserCognitiveProfile(
        userId = "user",
        thresholds = IndividualThresholds(baselineRtCv = baselineRtCv),
    )

    private fun session(endedAtMs: Long, fatigue: Double? = null, accuracy: Double? = null, rtCv: Double? = null) =
        SessionResult(
            sessionId = SessionId("user:$endedAtMs"),
            mode = Mode.TRAINING,
            exerciseFocus = ExerciseId.E1_VIGIA,
            blocks = emptyList(),
            startedAtMs = 0,
            endedAtMs = endedAtMs,
            sessionMetrics = buildMap {
                fatigue?.let {
                    put(MetricKey.FATIGUE_INDEX, MetricValue(MetricKey.FATIGUE_INDEX, it, 6, true, MetricUnit.INDEX))
                }
                accuracy?.let {
                    put(MetricKey.ACCURACY, MetricValue(MetricKey.ACCURACY, it, 50, true, MetricUnit.PROPORTION))
                }
                rtCv?.let {
                    put(MetricKey.RT_CV, MetricValue(MetricKey.RT_CV, it, 40, true, MetricUnit.RATIO))
                }
            },
        )

    @Test
    fun chronicHighFatigue_forcesRest() {
        val recent = listOf(session(300, fatigue = 0.85), session(200, fatigue = 0.80), session(100, fatigue = 0.78))
        assertIs<OvertrainingVerdict.ForceRest>(guard.check(profile(), recent))
    }

    @Test
    fun decliningAccuracy_withoutFatigue_isCaution() {
        // Newest-first: accuracy falling from 0.88 (oldest) to 0.65 (newest).
        val recent = listOf(
            session(400, accuracy = 0.65),
            session(300, accuracy = 0.70),
            session(200, accuracy = 0.82),
            session(100, accuracy = 0.88),
        )
        assertIs<OvertrainingVerdict.Caution>(guard.check(profile(), recent))
    }

    @Test
    fun risingVariabilityAndDecliningAccuracy_forcesRest() {
        val recent = listOf(
            session(400, accuracy = 0.66, rtCv = 0.40),
            session(300, accuracy = 0.72),
            session(200, accuracy = 0.83),
            session(100, accuracy = 0.90),
        )
        // baseline RT-CV 0.20; recent 0.40 > 0.20 * 1.5 → degraded + declining → ForceRest.
        assertIs<OvertrainingVerdict.ForceRest>(guard.check(profile(baselineRtCv = 0.20), recent))
    }

    @Test
    fun stableHealthyHistory_isOk() {
        val recent = listOf(
            session(400, fatigue = 0.2, accuracy = 0.85),
            session(300, fatigue = 0.2, accuracy = 0.86),
            session(200, fatigue = 0.2, accuracy = 0.85),
            session(100, fatigue = 0.2, accuracy = 0.84),
        )
        assertIs<OvertrainingVerdict.Ok>(guard.check(profile(), recent))
    }
}
