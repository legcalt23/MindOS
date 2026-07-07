@file:Suppress("MagicNumber", "MaxLineLength") // Fixture values are the data under test.

package com.mindos.app.features.mindboost.emotions.data.dto

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * MC8u-D §2/§6 — the sync/persistence contract. A fresh session is durable-local-only by charter; the sync posture is
 * an explicit, round-trip-safe field; and a build that DECLARES cloud sync required cannot ship the no-op sink.
 */
class EmotionSyncContractTest {

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    private fun summary() = EmotionSessionSummaryDto(
        exerciseId = "EM1_PAUSA",
        startedAtMillis = 1_000,
        durationMs = 130_000,
        difficultySummary = DifficultySummaryDto("INHIBITORY_DEMAND", 0.4, 2),
        metricsSummary = emptyList(),
        completed = true,
        abandoned = false,
        sessionId = "sess_sync1",
        endReason = "COMPLETED",
    )

    @Test
    fun fresh_summary_is_durable_local_only_on_a_known_platform_field() {
        val s = summary()
        assertEquals(EmotionSyncStatus.LOCAL_ONLY, s.syncStatus, "a fresh row is durable on-device only")
        assertEquals(EmotionPlatform.UNKNOWN, s.platform, "platform defaults to UNKNOWN until the app injects it")
        assertTrue(EmotionSyncStatus.isKnown(s.syncStatus))
    }

    @Test
    fun sync_posture_survives_serialization_round_trip() {
        val stamped = summary().copy(platform = EmotionPlatform.ANDROID, syncStatus = EmotionSyncStatus.PENDING_SYNC)
        val restored = json.decodeFromString(EmotionSessionSummaryDto.serializer(), json.encodeToString(EmotionSessionSummaryDto.serializer(), stamped))
        assertEquals(EmotionPlatform.ANDROID, restored.platform)
        assertEquals(EmotionSyncStatus.PENDING_SYNC, restored.syncStatus)
    }

    @Test
    fun status_set_is_closed() {
        assertTrue(EmotionSyncStatus.isKnown(EmotionSyncStatus.SYNCED))
        assertTrue(EmotionSyncStatus.isKnown(EmotionSyncStatus.SYNC_FAILED))
        assertFalse(EmotionSyncStatus.isKnown("MADE_UP"))
    }

    @Test
    fun release_requiring_cloud_sync_must_wire_a_real_sink() {
        // A build that declares cloud sync required must NOT ship the no-op — this is the enforceable release contract.
        assertFailsWith<IllegalArgumentException> {
            EmotionSyncPolicy.requireCloudSyncWiredWhenRequired(NoOpEmotionCloudSync, cloudSyncRequired = true)
        }
        // Charter local-first (no cloud required) legitimately keeps the no-op; a real sink always passes.
        EmotionSyncPolicy.requireCloudSyncWiredWhenRequired(NoOpEmotionCloudSync, cloudSyncRequired = false)
        val realSink = object : EmotionCloudSyncSink {
            override suspend fun push(userId: String, summary: EmotionSessionSummaryDto): String = EmotionSyncStatus.SYNCED
        }
        EmotionSyncPolicy.requireCloudSyncWiredWhenRequired(realSink, cloudSyncRequired = true)
    }
}
