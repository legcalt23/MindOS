@file:Suppress("MagicNumber") // Refresh-rate values (60/90/120) ARE the normalization data, not magic.

package com.mindos.app.features.arena.comparability.model

/**
 * Phase 04: Device class by refresh-rate tier. Used for DIF analysis and split leaderboards in
 * latency-sensitive motors (device_class_normalization.md).
 */
enum class DeviceClass {
    LOW_END_60HZ,
    MID_RANGE_90HZ,
    HIGH_END_120HZ,
    UNKNOWN;

    companion object {
        fun fromRefreshRateBand(band: Int): DeviceClass = when (band) {
            60 -> LOW_END_60HZ
            90 -> MID_RANGE_90HZ
            120 -> HIGH_END_120HZ
            else -> UNKNOWN
        }
    }
}

/**
 * Phase 04: Input method classification, used for DIF analysis (input_class_normalization.md).
 */
enum class InputClass {
    TOUCH,
    MOUSE_KEYBOARD,
    GAMEPAD,
    ACCESSIBILITY_SWITCH,
    UNKNOWN;

    companion object {
        fun fromRaw(raw: String): InputClass =
            entries.firstOrNull { it.name == raw.trim().uppercase() } ?: UNKNOWN
    }
}

/**
 * Phase 04: Accessibility comparability tier (accessibility_comparability_classification.md):
 * - COMPARABLE: cosmetic adaptations (e.g. colorblind palettes).
 * - COMPARABLE_OWN_NORM: motor adaptations comparable only within their own norm.
 * - TRAINING_ONLY: extended-time / non-ranked-comparable accommodations.
 * - NOT_COMPARABLE: accommodations that invalidate comparison (e.g. motor disabilities in speed motors).
 *
 * An unrecognized / absent value defaults to COMPARABLE (standard play, no penalty).
 */
enum class AccessibilityComparability {
    COMPARABLE,
    COMPARABLE_OWN_NORM,
    TRAINING_ONLY,
    NOT_COMPARABLE;

    companion object {
        fun fromRaw(raw: String): AccessibilityComparability =
            entries.firstOrNull { it.name == raw.trim().uppercase() } ?: COMPARABLE
    }
}
