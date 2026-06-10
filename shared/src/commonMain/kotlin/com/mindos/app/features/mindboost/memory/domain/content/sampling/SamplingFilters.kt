package com.mindos.app.features.mindboost.memory.domain.content.sampling

import com.mindos.app.features.mindboost.memory.domain.content.model.AssociationStrength
import com.mindos.app.features.mindboost.memory.domain.content.model.Concreteness
import com.mindos.app.features.mindboost.memory.domain.content.model.WordCategory
import com.mindos.app.features.mindboost.memory.domain.content.model.WordFrequency

/**
 * Optional content filters the Phase-4 generators use to control axes S and I
 * without the sampler hardcoding any difficulty semantics. The sampler applies
 * these as plain predicates over the pool BEFORE shuffling; it does NOT interpret
 * [com.mindos.app.features.mindboost.memory.domain.model.AxisLevel] (that
 * translation is Phase 4's job).
 *
 * Word filters ([category], [concreteness], [frequency]) apply to word sampling;
 * [associationStrength] applies to pair sampling. A null field means "no constraint".
 */
data class SamplingFilters(
    val category: WordCategory? = null,
    val concreteness: Concreteness? = null,
    val frequency: WordFrequency? = null,
    val associationStrength: AssociationStrength? = null,
) {
    companion object {
        val None = SamplingFilters()
    }
}
