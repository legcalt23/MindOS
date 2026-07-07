@file:Suppress("MaxLineLength") // Event construction reads best whole.

package com.mindos.app.core.analytics

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * MC8u-E §2 — the app-wide product-analytics hub: no-op by default, forwards to a registered sink, is fail-safe, and
 * a RELEASE build must register a real sink (not the no-op). Mirrors the ObservabilityHub contract.
 */
class MindBoostAnalyticsTest {

    private class Recording : MindBoostAnalyticsSink {
        val events = mutableListOf<MindBoostAnalyticsEvent>()
        override fun log(event: MindBoostAnalyticsEvent) { events += event }
    }

    @AfterTest
    fun resetHub() { MindBoostAnalyticsHub.sink = NoOpMindBoostAnalyticsSink }

    @Test
    fun default_sink_is_noop() {
        assertFalse(MindBoostAnalyticsHub.hasConfiguredSink, "before registration the hub is the no-op")
    }

    @Test
    fun hub_forwards_to_the_registered_sink() {
        val rec = Recording()
        MindBoostAnalyticsHub.sink = rec
        assertTrue(MindBoostAnalyticsHub.hasConfiguredSink)
        MindBoostAnalyticsHub.log(MindBoostAnalyticsEvent("emotion_exercise_started", mapOf("exercise_id" to "EM1_PAUSA")))
        assertEquals(1, rec.events.size)
        assertEquals("emotion_exercise_started", rec.events.first().name)
    }

    @Test
    fun a_throwing_sink_never_breaks_the_caller() {
        MindBoostAnalyticsHub.sink = object : MindBoostAnalyticsSink { override fun log(event: MindBoostAnalyticsEvent) = error("analytics down") }
        MindBoostAnalyticsHub.log(MindBoostAnalyticsEvent("x")) // must not throw
    }

    @Test
    fun release_build_must_register_a_real_sink() {
        assertFailsWith<IllegalArgumentException> {
            MindBoostAnalyticsPolicy.requireRealSinkInRelease(NoOpMindBoostAnalyticsSink, isReleaseBuild = true)
        }
        // Debug may keep the no-op; a real sink always passes.
        MindBoostAnalyticsPolicy.requireRealSinkInRelease(NoOpMindBoostAnalyticsSink, isReleaseBuild = false)
        MindBoostAnalyticsPolicy.requireRealSinkInRelease(Recording(), isReleaseBuild = true)
    }
}
