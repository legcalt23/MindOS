package com.mindos.testkit

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DatasetSelfTest {

    private val grammar = CommandGrammar(
        templates = listOf(
            CommandTemplate("open_settings", "open settings"),
            CommandTemplate("open_profile", "open {target}"),
            CommandTemplate("open_story", "open story {storyId}"),
        ),
        slots = mapOf(
            "target" to listOf("ana", "bob"),
            "storyId" to listOf("abc123", "st_42"),
        ),
    )
    private val intentToCategory = mapOf(
        "open_profile" to CommandCategory.PROFILES,
        "open_story" to CommandCategory.STORIES,
        "open_settings" to CommandCategory.SETTINGS,
        "open_messages" to CommandCategory.NOTIFICATIONS,
    )
    private val generator = DatasetGenerator(grammar, AliasDataset.rows, intentToCategory)

    @Test
    fun corpusRowsCarryCategoryAndExpectedIntent() {
        val rows = generator.buildCorpus(2000)
        assertTrue(rows.size >= 2000)
        rows.filter { it.kind == VariantKind.VALID }.forEach {
            assertTrue(it.expectedIntentId != null)
            assertTrue(it.category != CommandCategory.MIXED)
        }
    }

    @Test
    fun variationsCoverAllVariantKinds() {
        val kinds = generator.buildVariations(3000).map { it.kind }.toSet()
        assertTrue(VariantKind.TYPO in kinds)
        assertTrue(VariantKind.SPACING in kinds)
        assertTrue(VariantKind.CAPITALIZATION in kinds)
        assertTrue(VariantKind.ALIAS in kinds)
    }

    @Test
    fun exporterIsDeterministicAndParseable() {
        val a = generator.buildCorpus(500, seed = 7)
        val b = generator.buildCorpus(500, seed = 7)
        val jsonA = DatasetExporter.rowsToJson(a)
        assertEquals(jsonA, DatasetExporter.rowsToJson(b))
        val parsed = Json.decodeFromString<List<DatasetRow>>(jsonA)
        assertEquals(a, parsed)
    }

    @Test
    fun aliasDatasetSeparatesLocales() {
        assertTrue(AliasDataset.forLocale("es").all { it.locale == "es" })
        assertTrue(AliasDataset.forLocale("en").all { it.locale == "en" })
        assertTrue(AliasDataset.forLocale("es").isNotEmpty())
    }
}
