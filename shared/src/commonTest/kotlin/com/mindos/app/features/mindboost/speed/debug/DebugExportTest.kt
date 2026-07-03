package com.mindos.app.features.mindboost.speed.debug

import com.mindos.app.features.mindboost.speed.SpeedTestFixtures
import com.mindos.app.features.mindboost.speed.data.persistence.SpeedJson
import com.mindos.app.features.mindboost.speed.domain.DeviceTimingRisk
import com.mindos.app.features.mindboost.speed.domain.InputTimestampPrecision
import com.mindos.app.features.mindboost.speed.domain.MetricThresholds
import com.mindos.app.features.mindboost.speed.domain.SessionQuality
import com.mindos.app.features.mindboost.speed.domain.SpeedVersions
import com.mindos.app.features.mindboost.speed.domain.Timebase
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugExportTest {

    private val ms = SpeedTestFixtures.MS

    private fun sampleRun() = SpeedTestFixtures.run(
        userId = "u1",
        sessionId = "u1:s1",
        blocks = listOf(SpeedTestFixtures.block("u1:s1", "u1:s1:b0")),
        trials = listOf(
            SpeedTestFixtures.trial("u1:s1:b0", 0, 300 * ms, isCorrect = true),
            SpeedTestFixtures.trial("u1:s1:b0", 1, 280 * ms, isCorrect = true),
        ),
    )

    private val metadata = DebugExportMetadata(
        appVersion = "1.0-mvp0",
        metricsVersion = SpeedVersions.METRICS_VERSION,
        debugExportVersion = SpeedVersions.DEBUG_EXPORT_VERSION,
        timebase = Timebase.ANDROID_UI_UPTIME_NANOS,
        inputTimestampPrecision = InputTimestampPrecision.NANOS_NATIVE,
        deviceTimingRisk = DeviceTimingRisk.LOW,
        sessionQuality = SessionQuality.HIGH,
        thresholds = MetricThresholds.DEFAULT,
    )

    @Test
    fun json_roundTripsEnvelopeWithMetadata() {
        val json = DebugExport.toJson(sampleRun(), metadata)
        val decoded = SpeedJson.instance.decodeFromString<DebugExportEnvelope>(json)
        assertEquals("u1:s1", decoded.run.session.id)
        assertEquals(2, decoded.run.trials.size)
        assertEquals(SpeedVersions.METRICS_VERSION, decoded.metadata.metricsVersion)
        assertEquals(Timebase.ANDROID_UI_UPTIME_NANOS, decoded.metadata.timebase)
        assertEquals(SpeedVersions.DEBUG_EXPORT_VERSION, decoded.metadata.debugExportVersion)
        // Los umbrales activos viajan en la metadata.
        assertEquals(100_000_000L, decoded.metadata.thresholds.anticipationThresholdNanos)
    }

    @Test
    fun csv_hasHeaderAndOneRowPerTrial() {
        val csv = DebugExport.trialsToCsv(sampleRun())
        val lines = csv.split("\n")
        assertEquals(3, lines.size) // header + 2 trials
        assertTrue(lines.first().contains("rtNanos"))
        assertTrue(lines.first().contains("timebase"))
        assertTrue(lines[1].contains("${300 * ms}"))
        assertTrue(lines[2].contains("${280 * ms}"))
    }

    @Test
    fun privacySafeExport_removesStableUserAndDeviceValues() {
        val run = sampleRun()

        val json = DebugExport.toPrivacySafeJson(run, metadata)
        val csv = DebugExport.trialsToPrivacySafeCsv(run)

        listOf("u1", "Pixel-Test", "Android 14 (API 34)", "device-1").forEach { sensitiveValue ->
            assertFalse(json.contains(sensitiveValue))
            assertFalse(csv.contains(sensitiveValue))
        }
        assertTrue(json.contains("debug_user"))
        assertTrue(json.contains("redacted_device_model"))
        assertTrue(csv.contains("debug_user:s1:b0"))
    }
}
