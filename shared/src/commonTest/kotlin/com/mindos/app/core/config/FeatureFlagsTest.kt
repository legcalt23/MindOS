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
    }

    @Test
    fun staticFlags_overrideIndividually() {
        val flags = StaticFeatureFlags(walletEnabled = false)
        assertTrue(flags.privacyOverlayEnabled)
        assertTrue(flags.wavesEnabled)
        assertEquals(false, flags.walletEnabled)
    }
}
