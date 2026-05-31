package com.mindos.action.matching

import com.mindos.action.understanding.ActionUnderstandingResult
import com.mindos.testkit.AliasRow
import com.mindos.testkit.CommandCategory
import com.mindos.testkit.DatasetGenerator
import com.mindos.testkit.VariantKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Large-scale dataset validation (ZIP11 COMMAND_CORPUS_GUIDE): 10k+ commands and 50k+ variations,
 * generated deterministically and run through the engine. Asserts no crashes, full determinism, and
 * that valid/alias rows resolve to their expected intent.
 */
class DatasetScaleTest {

    private val intentToCategory = mapOf(
        "open_profile" to CommandCategory.PROFILES,
        "open_story" to CommandCategory.STORIES,
        "send_message" to CommandCategory.CHATS,
        "open_messages" to CommandCategory.NOTIFICATIONS,
        "open_home" to CommandCategory.NOTIFICATIONS,
        "open_settings" to CommandCategory.SETTINGS,
    )
    private val aliases = Reliability.aliases.map { AliasRow(it.first, "und", it.second) }
    private val generator = DatasetGenerator(Reliability.grammar, aliases, intentToCategory)

    @Test
    fun tenThousandCommandsAreDeterministicAndResolveAsExpected() {
        val corpus = generator.buildCorpus(10_000)
        assertTrue(corpus.size >= 10_000)
        for (row in corpus) {
            val r = Reliability.matcher.understand(row.text)
            assertEquals(r, Reliability.matcher.understand(row.text), "non-deterministic <<${row.text}>>")
            if (row.kind == VariantKind.VALID || row.kind == VariantKind.ALIAS) {
                assertTrue(r is ActionUnderstandingResult.Understood, "expected match for <<${row.text}>>")
                assertEquals(row.expectedIntentId, (r as ActionUnderstandingResult.Understood).intent.id.value)
            }
        }
    }

    @Test
    fun fiftyThousandVariationsNeverCrash() {
        val variations = generator.buildVariations(50_000)
        assertEquals(50_000, variations.size)
        for (row in variations) {
            // Must not throw; semantics-preserving variants still resolve, destructive ones at least don't crash.
            Reliability.matcher.understand(row.text)
        }
    }
}
