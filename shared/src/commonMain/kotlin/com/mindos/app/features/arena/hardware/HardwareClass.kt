package com.mindos.app.features.arena.hardware

import com.mindos.app.features.arena.config.OsFamily
import com.mindos.app.features.arena.config.RefreshRateTier
import kotlinx.serialization.Serializable

/**
 * Phase 05 / Task 5: Hardware class used to segment Mental Speed (Choice Reaction) measurement.
 *
 * Device touch pipelines and refresh rates vary by 30-80ms across OS families and refresh tiers
 * (15_jitter_device_bias_plan.md), which exceeds true cognitive variance. Mental Speed results are
 * therefore only comparable WITHIN the same [HardwareClass]; cross-class ranking is forbidden until
 * a future calibration protocol proves comparability.
 */
@Serializable
data class HardwareClass(
    val osFamily: OsFamily,
    val refreshRateTier: RefreshRateTier,
) {
    /** Stable, human-readable identifier such as "ANDROID-HZ_120". Safe (no PII). */
    val id: String get() = "${osFamily.name}-${refreshRateTier.name}"
}
