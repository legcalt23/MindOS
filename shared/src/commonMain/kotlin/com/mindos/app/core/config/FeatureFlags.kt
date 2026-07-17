package com.mindos.app.core.config

/**
 * Remotely-controllable feature flags / kill switches.
 *
 * Mirrors the [com.mindos.app.core.logging.ObservabilityHub] pattern: the
 * shared module declares the abstraction and reads it through [FeatureFlagsHub];
 * the platform layer (Android: Firebase Remote Config) registers the concrete
 * provider at startup. Tests/desktop/iOS fall back to [DefaultFeatureFlags].
 *
 * Semantics are "kill switch": established features default to their current behaviour, while
 * unreleased/high-risk features default OFF. Ops can disable enabled features or explicitly roll
 * out disabled features through Remote Config without shipping an app update.
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

    /** Command router and local voice orchestration surface. */
    val voiceCommandsEnabled: Boolean

    /** Remote cloud speech processing through the authenticated MindOS broker. */
    val voiceCloudSpeechEnabled: Boolean

    // Arena Simulation (all default OFF — offline simulation only, nothing live)
    val arenaRatingEngineEnabled: Boolean
    val arenaShadowRankedEnabled: Boolean
    val arenaMentalSpeedRankedEnabled: Boolean
    val arenaMatchmakingEnabled: Boolean
    val arenaTrialsEnabled: Boolean

    /**
     * MindBoost · Enfoque cloud-sync pilot (Firestore). Default OFF everywhere, including production: when
     * off, the app behaves exactly as before (local-first only, nothing uploaded). Only when on does the
     * Enfoque motor attempt a best-effort cloud mirror of session summaries.
     */
    val mindboostSyncFirestoreEnabled: Boolean

    /**
     * MindBoost brain-profile GLOBAL/composite index kill switch. Default OFF everywhere, including
     * production: when off, `MindBoostProfileAggregator` NEVER surfaces a cross-dimension composite
     * (official or provisional), no matter how many dimensions individually reach `ESTABLISHED` — closing
     * the "an accidental 3rd established dimension quietly turns the global on" gap (SYSTEM-1 Slice 6-R3,
     * C2.5 / SCO-014). Turning this ON only unlocks a clearly-labelled PROVISIONAL composite (SCO-014); the
     * OFFICIAL composite additionally requires `ADR_SCI_006_APPROVED` (a code constant, never a flag) and
     * stays unreachable until that ADR is approved.
     *
     * PRECONDITION before ever flipping this ON (W4 efficacy-guardian, SYSTEM-1 6R3 §25 reserva #1): the
     * provisional index must render its "(preliminar)" marker FUSED to the number itself (not only in the
     * caption), and its unweighted-mean composition must be documented as a non-validated proxy
     * (ADR-SCI-006). Today the number cannot render at all (this flag OFF + fewer than 3 dimensions reach
     * `ESTABLISHED`, so `summarize` returns null), making this a forward guard, not a live gap.
     */
    val mindboostGlobalScoreEnabled: Boolean
}

/**
 * Safe baked-in defaults. Used on desktop, iOS, tests, and on Android until/if Remote Config
 * activates an override. These string keys are the canonical Remote Config parameter names.
 */
object DefaultFeatureFlags : FeatureFlags {
    const val KEY_PRIVACY_OVERLAY = "privacy_overlay_enabled"
    const val KEY_WAVES = "waves_enabled"
    const val KEY_WALLET = "wallet_enabled"
    const val KEY_VOICE_COMMANDS = "voice_commands_enabled"
    const val KEY_VOICE_CLOUD_SPEECH = "voice_cloud_speech_enabled"
    const val KEY_ARENA_RATING_ENGINE = "arena_rating_engine_enabled"
    const val KEY_ARENA_SHADOW_RANKED = "arena_shadow_ranked_enabled"
    const val KEY_ARENA_MENTAL_SPEED_RANKED = "arena_mental_speed_ranked_enabled"
    const val KEY_ARENA_MATCHMAKING = "arena_matchmaking_enabled"
    const val KEY_ARENA_TRIALS = "arena_trials_enabled"
    const val KEY_MINDBOOST_SYNC_FIRESTORE = "mindboost_sync_firestore_enabled"
    const val KEY_MINDBOOST_GLOBAL_SCORE = "mindboost_global_score_enabled"

    override val privacyOverlayEnabled: Boolean = true
    override val wavesEnabled: Boolean = true
    override val walletEnabled: Boolean = false
    override val voiceCommandsEnabled: Boolean = false
    override val voiceCloudSpeechEnabled: Boolean = false
    override val arenaRatingEngineEnabled: Boolean = false
    override val arenaShadowRankedEnabled: Boolean = false
    override val arenaMentalSpeedRankedEnabled: Boolean = false
    override val arenaMatchmakingEnabled: Boolean = false
    override val arenaTrialsEnabled: Boolean = false
    override val mindboostSyncFirestoreEnabled: Boolean = false
    override val mindboostGlobalScoreEnabled: Boolean = false
}

/**
 * Immutable, explicitly-valued flags. Handy for tests and for platform providers
 * that snapshot remote values.
 */
data class StaticFeatureFlags(
    override val privacyOverlayEnabled: Boolean = DefaultFeatureFlags.privacyOverlayEnabled,
    override val wavesEnabled: Boolean = DefaultFeatureFlags.wavesEnabled,
    override val walletEnabled: Boolean = DefaultFeatureFlags.walletEnabled,
    override val voiceCommandsEnabled: Boolean = DefaultFeatureFlags.voiceCommandsEnabled,
    override val voiceCloudSpeechEnabled: Boolean = DefaultFeatureFlags.voiceCloudSpeechEnabled,
    override val arenaRatingEngineEnabled: Boolean = DefaultFeatureFlags.arenaRatingEngineEnabled,
    override val arenaShadowRankedEnabled: Boolean = DefaultFeatureFlags.arenaShadowRankedEnabled,
    override val arenaMentalSpeedRankedEnabled: Boolean = DefaultFeatureFlags.arenaMentalSpeedRankedEnabled,
    override val arenaMatchmakingEnabled: Boolean = DefaultFeatureFlags.arenaMatchmakingEnabled,
    override val arenaTrialsEnabled: Boolean = DefaultFeatureFlags.arenaTrialsEnabled,
    override val mindboostSyncFirestoreEnabled: Boolean = DefaultFeatureFlags.mindboostSyncFirestoreEnabled,
    override val mindboostGlobalScoreEnabled: Boolean = DefaultFeatureFlags.mindboostGlobalScoreEnabled,
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
