package com.mindos.app.core.config

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class FeatureFlagsTest {

    // The hub is a process-global; reset it so cross-test ordering can't leak state.
    @AfterTest
    fun resetHub() {
        FeatureFlagsHub.current = DefaultFeatureFlags
    }

    @Test
    fun defaults_keepUnreleasedWalletOff() {
        // Stable features default ON; wallet remains OFF until a real payment
        // provider is production-ready.
        assertTrue(DefaultFeatureFlags.privacyOverlayEnabled)
        assertTrue(DefaultFeatureFlags.wavesEnabled)
        assertEquals(false, DefaultFeatureFlags.walletEnabled)
        assertEquals(false, DefaultFeatureFlags.voiceCommandsEnabled)
        assertEquals(false, DefaultFeatureFlags.voiceCloudSpeechEnabled)
        // MindBoost Firestore sync is a controlled pilot: OFF by default, including production.
        assertEquals(false, DefaultFeatureFlags.mindboostSyncFirestoreEnabled)
        assertEquals(false, StaticFeatureFlags().mindboostSyncFirestoreEnabled)
        // MindBoost brain-profile global/composite index is OFF by default, including production
        // (SYSTEM-1 Slice 6-R3, C2.5): the aggregator must never surface a composite unless explicitly on.
        assertEquals(false, DefaultFeatureFlags.mindboostGlobalScoreEnabled)
        assertEquals(false, StaticFeatureFlags().mindboostGlobalScoreEnabled)
    }

    @Test
    fun mindboostGlobalScore_canBeEnabledExplicitly() {
        val flags = StaticFeatureFlags(mindboostGlobalScoreEnabled = true)
        assertTrue(flags.mindboostGlobalScoreEnabled)
        assertEquals(false, StaticFeatureFlags().mindboostGlobalScoreEnabled)
    }

    @Test
    fun mindboostSync_canBeEnabledExplicitlyForPilot() {
        // Ops (or a test) can flip the pilot on without touching any other flag.
        val flags = StaticFeatureFlags(mindboostSyncFirestoreEnabled = true)
        assertTrue(flags.mindboostSyncFirestoreEnabled)
        assertEquals(false, StaticFeatureFlags().mindboostSyncFirestoreEnabled)
    }

    @Test
    fun hub_defaultsToDefaultFeatureFlags() {
        assertSame(DefaultFeatureFlags, FeatureFlagsHub.current)
    }

    @Test
    fun hub_canSwapProvider_andDisableSingleFlag() {
        FeatureFlagsHub.current = StaticFeatureFlags(privacyOverlayEnabled = false)

        assertEquals(false, FeatureFlagsHub.current.privacyOverlayEnabled, "kill switch must take effect")
        assertEquals(true, FeatureFlagsHub.current.wavesEnabled, "other flags stay at their defaults")
        assertEquals(false, FeatureFlagsHub.current.walletEnabled)
        assertEquals(false, FeatureFlagsHub.current.voiceCommandsEnabled)
        assertEquals(false, FeatureFlagsHub.current.voiceCloudSpeechEnabled)
    }

    @Test
    fun staticFlags_overrideIndividually() {
        val flags = StaticFeatureFlags(
            walletEnabled = false,
            voiceCommandsEnabled = true,
            voiceCloudSpeechEnabled = false,
        )
        assertTrue(flags.privacyOverlayEnabled)
        assertTrue(flags.wavesEnabled)
        assertEquals(false, flags.walletEnabled)
        assertTrue(flags.voiceCommandsEnabled)
        assertEquals(false, flags.voiceCloudSpeechEnabled)
    }
}
