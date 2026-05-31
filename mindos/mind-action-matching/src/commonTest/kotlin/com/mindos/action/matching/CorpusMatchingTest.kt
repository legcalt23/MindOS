package com.mindos.action.matching

import com.mindos.action.understanding.ActionUnderstandingResult
import com.mindos.testkit.CorpusBuilder
import com.mindos.testkit.CorpusCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Corpus-driven suite (ZIP04 COMMAND_CORPUS_GUIDE): a 10k+ deterministic dataset of valid commands,
 * aliases, misspellings, edge cases and malformed inputs. Asserts every entry resolves
 * deterministically and never crashes, and that VALID/ALIAS entries hit their expected intent.
 */
class CorpusMatchingTest {

    private val corpus = CorpusBuilder(Reliability.grammar, Reliability.aliases).build(targetSize = 10_000, seed = 99)

    @Test
    fun corpusHasTenThousandPlusEntries() {
        assertTrue(corpus.size >= 10_000, "corpus too small: ${corpus.size}")
    }

    @Test
    fun everyEntryIsDeterministicAndNeverCrashes() {
        for (entry in corpus) {
            val a = Reliability.matcher.understand(entry.text)
            val b = Reliability.matcher.understand(entry.text)
            assertEquals(a, b, "non-deterministic for <<${entry.text}>>")
        }
    }

    @Test
    fun validAndAliasEntriesResolveToExpectedIntent() {
        val checked = corpus.filter {
            (it.category == CorpusCategory.VALID || it.category == CorpusCategory.ALIAS) && it.expectedIntentId != null
        }
        assertTrue(checked.size > 1000, "expected a large valid set, got ${checked.size}")
        for (entry in checked) {
            val r = Reliability.matcher.understand(entry.text)
            assertTrue(r is ActionUnderstandingResult.Understood, "expected match for <<${entry.text}>> (${entry.category})")
            assertEquals(entry.expectedIntentId, (r as ActionUnderstandingResult.Understood).intent.id.value, "wrong intent for <<${entry.text}>>")
        }
    }

    @Test
    fun edgeCaseEntriesAreHandledNotCrashed() {
        val edge = corpus.filter { it.category == CorpusCategory.EDGE_CASE }
        for (entry in edge) {
            // Empty/whitespace → Rejected; others → some typed result. Never throws.
            Reliability.matcher.understand(entry.text)
        }
        assertTrue(edge.isNotEmpty())
    }
}
