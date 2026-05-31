package com.mindos.testkit

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Generates the machine-readable dataset assets (ZIP11). Writes a committed representative SAMPLE
 * (~150 rows/category) under `datasets/sample/` and the full 10k+/50k+ sets on-demand under
 * `build/datasets/` (not committed). Deterministic: identical bytes each run, so committed samples
 * stay golden — if the generator changes, the sample files change and must be re-committed.
 */
class DatasetAssetsTest {

    private val grammar = CommandGrammar(
        templates = listOf(
            CommandTemplate("open_settings", "open settings"),
            CommandTemplate("open_settings", "settings"),
            CommandTemplate("open_profile", "open {target}"),
            CommandTemplate("open_story", "open story {storyId}"),
            CommandTemplate("open_chat", "open chat {chatId}"),
        ),
        slots = mapOf(
            "target" to listOf("ana", "bob", "carol", "dave", "emma"),
            "storyId" to listOf("abc123", "st_42", "xyz789", "id0001"),
            "chatId" to listOf("team", "random", "family", "work"),
        ),
    )
    private val intentToCategory = mapOf(
        "open_profile" to CommandCategory.PROFILES,
        "open_story" to CommandCategory.STORIES,
        "open_chat" to CommandCategory.CHATS,
        "open_messages" to CommandCategory.NOTIFICATIONS,
        "open_settings" to CommandCategory.SETTINGS,
    )
    private val generator = DatasetGenerator(grammar, AliasDataset.rows, intentToCategory)

    @Test
    fun generatesCommittedSampleAndFullAssets() {
        val sampleDir = File("datasets/sample").apply { mkdirs() }
        val buildDir = File("build/datasets").apply { mkdirs() }

        // Committed sample: representative rows per category.
        val corpus = generator.buildCorpus(6000)
        for (category in CommandCategory.entries.filter { it != CommandCategory.MIXED }) {
            val rows = corpus.filter { it.category == category }.take(150)
            File(sampleDir, "${category.name.lowercase()}.json").writeText(DatasetExporter.rowsToJson(rows))
        }
        File(sampleDir, "aliases.json").writeText(DatasetExporter.aliasesToJson(AliasDataset.rows))
        File(sampleDir, "ambiguity.json").writeText(DatasetExporter.ambiguityToJson(AmbiguityDataset.cases))

        // Full sets on-demand (not committed).
        File(buildDir, "corpus-10k.json").writeText(DatasetExporter.rowsToJson(generator.buildCorpus(10_000)))
        File(buildDir, "variations-50k.json").writeText(DatasetExporter.rowsToJson(generator.buildVariations(50_000)))

        assertTrue(File(sampleDir, "profiles.json").exists())
        assertTrue(File(sampleDir, "aliases.json").exists())
        assertTrue(File(buildDir, "corpus-10k.json").length() > 0)
    }

    @Test
    fun fullSetsReachRequiredScale() {
        assertTrue(generator.buildCorpus(10_000).size >= 10_000)
        assertEquals(50_000, generator.buildVariations(50_000).size)
    }
}
