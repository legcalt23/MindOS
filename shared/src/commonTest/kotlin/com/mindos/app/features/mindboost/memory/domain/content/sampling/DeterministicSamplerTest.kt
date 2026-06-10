package com.mindos.app.features.mindboost.memory.domain.content.sampling

import com.mindos.app.features.mindboost.memory.domain.content.ContentFixtures
import com.mindos.app.features.mindboost.memory.domain.content.model.AssociationStrength
import com.mindos.app.features.mindboost.memory.domain.content.model.WordCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DeterministicSamplerTest {

    private val sampler = deterministicSampler(ContentFixtures.wordPool(), ContentFixtures.pairPool())

    @Test
    fun sampleWords_isReproducibleForSameSeed() {
        val first = sampler.sampleWords(count = 10, seed = 42)
        repeat(9) {
            assertEquals(first, sampler.sampleWords(count = 10, seed = 42))
        }
    }

    @Test
    fun sampleWords_differsForDifferentSeed() {
        val a = sampler.sampleWords(count = 10, seed = 1)
        val b = sampler.sampleWords(count = 10, seed = 2)
        assertTrue(a != b, "Different seeds should (with high probability) differ.")
    }

    @Test
    fun sampleWords_hasNoDuplicates() {
        val sample = sampler.sampleWords(count = 20, seed = 7)
        assertEquals(sample.size, sample.map { it.id }.toSet().size)
    }

    @Test
    fun sampleWords_respectsCategoryFilter() {
        val sample = sampler.sampleWords(
            count = 5,
            seed = 3,
            filters = SamplingFilters(category = WordCategory.ANIMALS),
        )
        assertTrue(sample.all { it.category == WordCategory.ANIMALS })
    }

    @Test
    fun sampleWords_neverReturnsExcludedIds() {
        val excluded = sampler.sampleWords(count = 10, seed = 5).map { it.id }.toSet()
        val next = sampler.sampleWords(count = 10, seed = 5, excludeRecentIds = excluded)
        assertTrue(next.none { it.id in excluded }, "Excluded ids must never reappear.")
    }

    @Test
    fun sampleWords_failsLoudlyWhenInsufficient() {
        val all = sampler.sampleWords(count = 130, seed = 0)
        // Exclude everything, then ask for more — must throw, not truncate or duplicate.
        assertFailsWith<IllegalStateException> {
            sampler.sampleWords(count = 5, seed = 0, excludeRecentIds = all.map { it.id }.toSet())
        }
    }

    @Test
    fun samplePairs_isReproducibleAndRespectsExclusion() {
        val first = sampler.samplePairs(count = 8, seed = 11)
        assertEquals(first, sampler.samplePairs(count = 8, seed = 11))
        val next = sampler.samplePairs(count = 8, seed = 11, excludeRecentIds = first.map { it.id }.toSet())
        assertTrue(next.none { it.id in first.map { p -> p.id }.toSet() })
    }

    @Test
    fun samplePairs_respectsStrengthFilter() {
        val sample = sampler.samplePairs(
            count = 5,
            seed = 9,
            filters = SamplingFilters(associationStrength = AssociationStrength.WEAK),
        )
        assertTrue(sample.all { it.associationStrength == AssociationStrength.WEAK })
    }

    @Test
    fun consecutiveSamples_doNotOverlapWhenFirstExcluded() {
        val firstBatch = sampler.sampleWords(count = 10, seed = 100)
        val secondBatch = sampler.sampleWords(
            count = 10,
            seed = 101,
            excludeRecentIds = firstBatch.map { it.id }.toSet(),
        )
        assertTrue(firstBatch.map { it.id }.toSet().intersect(secondBatch.map { it.id }.toSet()).isEmpty())
    }
}
