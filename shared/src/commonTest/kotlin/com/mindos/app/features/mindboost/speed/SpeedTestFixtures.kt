// Las fábricas de records reflejan los DTOs del §5: muchos params por diseño.
@file:Suppress("LongParameterList")

package com.mindos.app.features.mindboost.speed

import com.mindos.app.features.mindboost.speed.data.persistence.MeasurementRun
import com.mindos.app.features.mindboost.speed.domain.DeviceTimingRisk
import com.mindos.app.features.mindboost.speed.domain.InputTimestampPrecision
import com.mindos.app.features.mindboost.speed.domain.SessionQuality
import com.mindos.app.features.mindboost.speed.domain.SpeedVersions
import com.mindos.app.features.mindboost.speed.domain.TimestampSource
import com.mindos.app.features.mindboost.speed.domain.Timebase
import com.mindos.app.features.mindboost.speed.domain.metrics.MeasurementContext
import com.mindos.app.features.mindboost.speed.domain.model.BlockMetrics
import com.mindos.app.features.mindboost.speed.domain.model.BlockRecord
import com.mindos.app.features.mindboost.speed.domain.model.DeviceProfile
import com.mindos.app.features.mindboost.speed.domain.model.QualityDecisionLog
import com.mindos.app.features.mindboost.speed.domain.model.SessionMetrics
import com.mindos.app.features.mindboost.speed.domain.model.SessionRecord
import com.mindos.app.features.mindboost.speed.domain.model.TrialRecord
import com.mindos.app.features.mindboost.speed.domain.model.VersionSnapshot

/** Fábricas compartidas para construir records de prueba sin un dispositivo. */
object SpeedTestFixtures {

    const val MS = 1_000_000L // nanos por ms

    val context = MeasurementContext(
        timestampSource = TimestampSource.CHOREOGRAPHER_FRAME,
        inputTimestampPrecision = InputTimestampPrecision.NANOS_NATIVE,
        timebase = Timebase.ANDROID_UI_UPTIME_NANOS,
    )

    fun trial(
        blockId: String,
        index: Int,
        rtNanos: Long,
        isCorrect: Boolean,
        isAnticipation: Boolean = false,
        isLapse: Boolean = false,
        exerciseId: String = "E1",
    ): TrialRecord = TrialRecord(
        id = "$blockId:t$index",
        blockId = blockId,
        trialIndex = index,
        exerciseId = exerciseId,
        stimulusUptimeNanos = 0L,
        responseUptimeNanos = rtNanos,
        rtNanos = rtNanos,
        isCorrect = isCorrect,
        isAnticipation = isAnticipation,
        isLapse = isLapse,
        lapseMultiplier = 2.5,
        deadlineNanos = null,
        difficultyEstimate = 0.5,
        timestampSource = context.timestampSource,
        inputTimestampPrecision = context.inputTimestampPrecision,
        timebase = context.timebase,
    )

    fun deviceProfile(
        id: String = "device-1",
        apiLevel: Int = 34,
        risk: DeviceTimingRisk = DeviceTimingRisk.LOW,
    ): DeviceProfile = DeviceProfile(
        id = id,
        deviceModel = "Pixel-Test",
        osVersion = "Android 14 (API $apiLevel)",
        apiLevel = apiLevel,
        simpleReactionMedianMs = 250.0,
        deviceTimingRisk = risk,
    )

    @Suppress("LongParameterList")
    fun run(
        userId: String,
        sessionId: String,
        device: DeviceProfile = deviceProfile(),
        blocks: List<BlockRecord> = emptyList(),
        trials: List<TrialRecord> = emptyList(),
        blockMetrics: List<BlockMetrics> = emptyList(),
        sessionMetrics: SessionMetrics? = null,
        qualityLogs: List<QualityDecisionLog> = emptyList(),
    ): MeasurementRun = MeasurementRun(
        session = SessionRecord(
            id = sessionId,
            userId = userId,
            timestampEpochMs = 1_700_000_000_000L,
            deviceProfileId = device.id,
            durationMs = 60_000L,
        ),
        deviceProfile = device,
        versionSnapshot = VersionSnapshot(
            sessionId = sessionId,
            metricsVersion = SpeedVersions.METRICS_VERSION,
            staircaseVersion = SpeedVersions.STAIRCASE_VERSION,
            difficultyModelVersion = SpeedVersions.DIFFICULTY_MODEL_VERSION,
            lapseMultiplier = 2.5,
        ),
        blocks = blocks,
        trials = trials,
        blockMetrics = blockMetrics,
        sessionMetrics = sessionMetrics,
        qualityLogs = qualityLogs,
    )

    fun block(sessionId: String, blockId: String, index: Int = 0, exerciseId: String = "E1"): BlockRecord =
        BlockRecord(
            id = blockId,
            sessionId = sessionId,
            blockIndex = index,
            exerciseId = exerciseId,
            startedAtEpochMs = 1_700_000_000_000L,
            durationMs = 30_000L,
            fixedDifficulty = 0.5,
            deadlineNanos = null,
            timestampSource = context.timestampSource,
            inputTimestampPrecision = context.inputTimestampPrecision,
            timebase = context.timebase,
        )

    fun qualityLog(sessionId: String): QualityDecisionLog = QualityDecisionLog(
        id = "$sessionId:q0",
        sessionId = sessionId,
        deviceTimingRisk = DeviceTimingRisk.LOW,
        sessionQuality = SessionQuality.HIGH,
        reasons = emptyList(),
        createdAtEpochMs = 1_700_000_000_000L,
    )
}
