package com.mindos.app.features.arena

import com.mindos.app.features.arena.comparability.engine.ComparabilityClassifier
import com.mindos.app.features.arena.comparability.model.AccessibilityComparability
import com.mindos.app.features.arena.comparability.model.DeviceClass
import com.mindos.app.features.arena.comparability.model.InputClass
import com.mindos.app.features.arena.config.ComparabilityStateEnum
import com.mindos.app.features.arena.rating.model.JitterMs
import com.mindos.app.features.arena.rating.model.LatencyMs
import com.mindos.app.features.arena.session.model.SessionEnvironmentSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Phase 04 / P4.1: Comparability classification. */
class ComparabilityClassifierTest {

    private val classifier = ComparabilityClassifier()

    private fun env(
        latencyMs: Long = 30,
        jitterMs: Long = 5,
        networkInstability: Double = 0.1,
        accessibilityClass: String = "COMPARABLE",
        refreshRateBand: Int = 120,
        inputClass: String = "TOUCH",
    ) = SessionEnvironmentSnapshot(
        deviceClass = "HIGH_END_120HZ",
        inputClass = inputClass,
        fpsBand = 120,
        refreshRateBand = refreshRateBand,
        screenSizeBand = "MEDIUM",
        latencyMs = LatencyMs(latencyMs),
        jitterMs = JitterMs(jitterMs),
        networkInstability = networkInstability,
        languageGroup = "EN",
        accessibilityClass = accessibilityClass,
        clientVersion = "1.0",
        osVersion = "TEST",
        appBuild = "TEST",
        timestampMs = 0L,
    )

    @Test
    fun latencyTiersClassifyByThreshold() {
        assertEquals(ComparabilityStateEnum.COMPARABLE, classifier.classifyLatency(LatencyMs(49)))
        assertEquals(ComparabilityStateEnum.PARTIALLY_COMPARABLE, classifier.classifyLatency(LatencyMs(75)))
        assertEquals(ComparabilityStateEnum.NOT_COMPARABLE, classifier.classifyLatency(LatencyMs(150)))
    }

    @Test
    fun jitterAndNetworkTiersClassifyByThreshold() {
        assertEquals(ComparabilityStateEnum.COMPARABLE, classifier.classifyJitter(JitterMs(10)))
        assertEquals(ComparabilityStateEnum.NOT_COMPARABLE, classifier.classifyJitter(JitterMs(80)))
        assertEquals(ComparabilityStateEnum.COMPARABLE, classifier.classifyNetwork(0.1))
        assertEquals(ComparabilityStateEnum.PARTIALLY_COMPARABLE, classifier.classifyNetwork(0.45))
        assertEquals(ComparabilityStateEnum.NOT_COMPARABLE, classifier.classifyNetwork(0.9))
    }

    @Test
    fun accessibilityTiersMapCorrectly() {
        assertEquals(
            ComparabilityStateEnum.COMPARABLE,
            classifier.accessibilityTier(AccessibilityComparability.COMPARABLE),
        )
        assertEquals(
            ComparabilityStateEnum.PARTIALLY_COMPARABLE,
            classifier.accessibilityTier(AccessibilityComparability.COMPARABLE_OWN_NORM),
        )
        assertEquals(
            ComparabilityStateEnum.NOT_COMPARABLE,
            classifier.accessibilityTier(AccessibilityComparability.TRAINING_ONLY),
        )
        assertEquals(
            ComparabilityStateEnum.NOT_COMPARABLE,
            classifier.accessibilityTier(AccessibilityComparability.NOT_COMPARABLE),
        )
    }

    @Test
    fun cleanEnvironmentIsComparableWithFullMultiplier() {
        val decision = classifier.classify(env())
        assertEquals(ComparabilityStateEnum.COMPARABLE, decision.state)
        assertEquals(1.0, decision.updateMultiplier)
    }

    @Test
    fun worstDimensionDrivesOverallDecision() {
        val partial = classifier.classify(env(networkInstability = 0.45))
        assertEquals(ComparabilityStateEnum.PARTIALLY_COMPARABLE, partial.state)
        assertEquals(0.5, partial.updateMultiplier)

        val notComparable = classifier.classify(env(latencyMs = 200, networkInstability = 0.45))
        assertEquals(ComparabilityStateEnum.NOT_COMPARABLE, notComparable.state)
        assertEquals(0.0, notComparable.updateMultiplier)
        assertTrue(notComparable.reasonCode.contains("LATENCY"), "reason=${notComparable.reasonCode}")
    }

    @Test
    fun auditLogCapturesEveryDimensionTier() {
        val environment = env(latencyMs = 200)
        val decision = classifier.classify(environment)
        val log = classifier.auditLog(environment, decision, "AUDIT_1", "SESSION_1", 123L)
        assertEquals("NOT_COMPARABLE", log.decisionState)
        assertEquals("NOT_COMPARABLE", log.latencyTier)
        assertEquals("COMPARABLE", log.jitterTier)
        assertEquals("COMPARABLE", log.accessibilityTier)
        assertEquals("ComparabilityClassifier", log.sourceEngine)
    }

    @Test
    fun enumNormalizationFallsBackToSafeDefaults() {
        assertEquals(DeviceClass.HIGH_END_120HZ, DeviceClass.fromRefreshRateBand(120))
        assertEquals(DeviceClass.UNKNOWN, DeviceClass.fromRefreshRateBand(45))
        assertEquals(InputClass.TOUCH, InputClass.fromRaw("touch"))
        assertEquals(InputClass.UNKNOWN, InputClass.fromRaw("brain-implant"))
        assertEquals(AccessibilityComparability.COMPARABLE, AccessibilityComparability.fromRaw("nonsense"))
    }
}
