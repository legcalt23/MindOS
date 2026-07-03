package com.mindos.app.features.mindboost.profile.domain.source

import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.ModuleContribution

/**
 * One-way contract: a MindBoost module exposes its coarse profile contribution; the global
 * MindBoostProfile consumes it. The profile NEVER reaches into a module's internals — adapters live
 * inside each module and depend on the module, not the other way around (same direction as
 * `EmotionProfileReadOnly`).
 *
 * Returns `null` when the module has no usable signal yet for [userId] (the aggregator treats a missing
 * contribution as "calibrating", never as a zero score).
 */
interface CognitiveModuleProfileSource {
    val moduleId: CognitiveModuleId
    suspend fun contribution(userId: String): ModuleContribution?
}
