package com.mindos.app.features.mindboost.core.antigaming

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** INFRA-03 · the aggregator picks the most severe triggered action; untriggered signals are ignored. */
class AntiGamingFrameworkTest {

    private fun signal(id: String, triggered: Boolean, action: GamingAction) =
        DetectorSignal(id, triggered, strength = if (triggered) 1.0 else 0.0, action = action, evidence = id)

    @Test
    fun noSignals_isCleanNone() {
        val v = AntiGamingFramework.aggregate(emptyList())
        assertEquals(GamingAction.NONE, v.action)
        assertTrue(v.isClean)
        assertTrue(v.firedDetectors.isEmpty())
    }

    @Test
    fun untriggeredSignals_areIgnored() {
        val v = AntiGamingFramework.aggregate(
            listOf(signal("a", false, GamingAction.INVALIDATE), signal("b", false, GamingAction.WITHHOLD_PROFILE)),
        )
        assertEquals(GamingAction.NONE, v.action)
        assertTrue(v.isClean)
    }

    @Test
    fun mostSevereTriggeredAction_wins() {
        val v = AntiGamingFramework.aggregate(
            listOf(
                signal("flag", true, GamingAction.FLAG_ONLY),
                signal("withhold", true, GamingAction.WITHHOLD_PROFILE),
                signal("invalidate", true, GamingAction.INVALIDATE),
            ),
        )
        assertEquals(GamingAction.INVALIDATE, v.action)
        assertFalse(v.isClean)
        assertEquals(setOf("flag", "withhold", "invalidate"), v.firedDetectors.toSet())
    }

    @Test
    fun flagOnly_staysClean_butWithholdDoesNot() {
        assertTrue(AntiGamingFramework.aggregate(listOf(signal("f", true, GamingAction.FLAG_ONLY))).isClean)
        assertFalse(AntiGamingFramework.aggregate(listOf(signal("w", true, GamingAction.WITHHOLD_PROFILE))).isClean)
    }
}
