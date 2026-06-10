package com.mindos.app.features.mindboost.memory.domain.content

import com.mindos.app.features.mindboost.memory.domain.content.model.AssociationStrength
import com.mindos.app.features.mindboost.memory.domain.content.model.Concreteness
import com.mindos.app.features.mindboost.memory.domain.content.model.Language
import com.mindos.app.features.mindboost.memory.domain.content.model.PairId
import com.mindos.app.features.mindboost.memory.domain.content.model.PairItem
import com.mindos.app.features.mindboost.memory.domain.content.model.WordCategory
import com.mindos.app.features.mindboost.memory.domain.content.model.WordFrequency
import com.mindos.app.features.mindboost.memory.domain.content.model.WordId
import com.mindos.app.features.mindboost.memory.domain.content.model.WordItem
import com.mindos.app.features.mindboost.memory.domain.content.pool.PairPool
import com.mindos.app.features.mindboost.memory.domain.content.pool.WordPool

/**
 * Synthetic content fixtures for pool/sampler/partition tests. These are NOT the
 * Spanish content pool (validated separately in SpanishMemoryContentDataTest) —
 * just enough valid, deterministic items to exercise pool/sampler logic without
 * depending on the authored data, keeping these unit tests isolated.
 */
object ContentFixtures {

    fun word(index: Int): WordItem {
        val text = "palabra$index"
        return WordItem(
            id = WordId("w$index"),
            text = text,
            category = WordCategory.entries[index % WordCategory.entries.size],
            concreteness = Concreteness.entries[index % Concreteness.entries.size],
            frequency = WordFrequency.entries[index % WordFrequency.entries.size],
            lengthChars = text.length,
        )
    }

    /** A valid pool of [n] words (defaults above the pool minimum). */
    fun wordPool(n: Int = WordPool.MIN_POOL_SIZE + 30): WordPool =
        WordPool(version = 1, language = Language.SPANISH_ES, items = (1..n).map { word(it) })

    /** A valid pool of [n] pairs built from 2·[n] distinct words. */
    fun pairPool(n: Int = PairPool.MIN_POOL_SIZE + 20): PairPool {
        val pairs = (1..n).map { k ->
            PairItem(
                id = PairId("p$k"),
                cue = word(2 * k),
                target = word(2 * k + 1),
                associationStrength = AssociationStrength.entries[k % AssociationStrength.entries.size],
            )
        }
        return PairPool(version = 1, language = Language.SPANISH_ES, items = pairs)
    }
}
