package com.mindos.app.core.config

/**
 * Remotely-controllable feature flags / kill switches.
 *
 * Mirrors the [com.mindos.app.core.logging.ObservabilityHub] pattern: the
 * shared module declares the abstraction and reads it through [FeatureFlagsHub];
 * the platform layer (Android: Firebase Remote Config) registers the concrete
 * provider at startup. Tests/desktop/iOS fall back to [DefaultFeatureFlags].
 *
 * Semantics are "kill switch": every flag defaults to the feature being ENABLED
 * (current behaviour), and ops can flip it to `false` in Remote Config to disable
 * the feature remotely — without shipping an app update. Useful for the
 * camera-based privacy overlay (battery/privacy sensitive) and the wallet/money
 * path, both flagged as operationally risky in the audit.
 *
 * Update timing: the Android provider reads live from the activated Remote Config
 * cache, so a fetched change takes effect on the next read (typically next launch
 * or next navigation that re-reads the flag) — not mid-frame. Adequate for kill
 * switches; not intended for per-recomposition reactive flags.
 */
interface FeatureFlags {
    /** Camera-based privacy overlay (continuous front-camera face detection). */
    val privacyOverlayEnabled: Boolean

    /** Wave (DM request) feature. */
    val wavesEnabled: Boolean

    /** Virtual-currency wallet / purchase flow. Kill switch for the money path. */
    val walletEnabled: Boolean
}

/**
 * Safe baked-in defaults: every feature ENABLED. Used on desktop, iOS, tests, and
 * on Android until/if Remote Config activates an override. These string keys are
 * the canonical Remote Config parameter names.
 */
object DefaultFeatureFlags : FeatureFlags {
    const val KEY_PRIVACY_OVERLAY = "privacy_overlay_enabled"
    const val KEY_WAVES = "waves_enabled"
    const val KEY_WALLET = "wallet_enabled"

    override val privacyOverlayEnabled: Boolean = true
    override val wavesEnabled: Boolean = true
    override val walletEnabled: Boolean = false
}

/**
 * Immutable, explicitly-valued flags. Handy for tests and for platform providers
 * that snapshot remote values.
 */
data class StaticFeatureFlags(
    override val privacyOverlayEnabled: Boolean = DefaultFeatureFlags.privacyOverlayEnabled,
    override val wavesEnabled: Boolean = DefaultFeatureFlags.wavesEnabled,
    override val walletEnabled: Boolean = DefaultFeatureFlags.walletEnabled,
) : FeatureFlags

/**
 * Global registry. The Android layer swaps [current] at app startup with a
 * Remote-Config-backed provider; everywhere else it stays [DefaultFeatureFlags].
 * Reads are lock-free; the write happens once during init.
 */
object FeatureFlagsHub {
    @Volatile
    var current: FeatureFlags = DefaultFeatureFlags
}
