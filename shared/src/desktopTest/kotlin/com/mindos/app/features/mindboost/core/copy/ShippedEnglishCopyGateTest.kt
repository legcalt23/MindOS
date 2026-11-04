package com.mindos.app.features.mindboost.core.copy

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `MB-EMO-0029` — the copy-safety gate carries a Spanish lexicon, so an English claim shipped
 * unseen: `CreativityExport.DISCLAIMER` denied a health claim by naming it, and that string is the
 * first line of every CSV the user exports and a field of every JSON. The build was green over it
 * for the whole life of the feature.
 *
 * This is the gate for that surface. It is deliberately NOT a widening of the Spanish gate, and the
 * reason is measured rather than argued — see [ForbiddenLexicon.ENGLISH_CLAIMS]. In one line: over
 * the same tree, a wide English vocabulary hits 159 KDoc paragraphs and 21 test guard lists against
 * one production literal, so whole-file English matching would break 180 honest sites to catch 1.
 * Scoping to production string literals catches that 1 with no false positive.
 *
 * Three things are pinned, because a scanner has three parts and only pinning the vocabulary is how
 * `MB-EMO-0023` happened:
 *  - the WORDS: [ForbiddenLexicon.ENGLISH_CLAIMS];
 *  - the SURFACE: the module list is parsed from `shared/build.gradle.kts`, never hand-copied, so a
 *    module added to the Spanish gate is covered here the same day (a third hand-kept copy of that
 *    list is exactly the drift `CopySafetyLexiconParityTest` exists to prevent);
 *  - the EXTRACTOR: [literalsOf] must separate literals from comments, or "0 hits" would mean "the
 *    parser returned nothing" — a check that parses zero things reports everything clean.
 *
 * Ceiling, declared: this reads source text. A claim assembled at runtime from fragments, or loaded
 * from a resource, is invisible to it — as it is to the Spanish gate.
 */
class ShippedEnglishCopyGateTest {

    @Test
    fun noProductionStringLiteralCarriesAnEnglishClaim() {
        val files = productionFiles()
        assertTrue(files.size >= MIN_FILES, "walked only ${files.size} production files — roots wrong?")

        var literals = 0
        val offenders = mutableListOf<String>()
        for (file in files) {
            for (literal in literalsOf(file.readText())) {
                literals++
                val hits = MindBoostCopySafetyScanner.scan(literal.text, ForbiddenLexicon.ENGLISH_CLAIMS)
                if (hits.isNotEmpty()) {
                    offenders += "${file.name}:${literal.line} ${hits.map { it.term }} -> ${literal.text.take(120)}"
                }
            }
        }

        // Anti-vacuity: an extractor that yielded nothing would report the whole tree clean.
        assertTrue(literals >= MIN_LITERALS, "extracted only $literals string literals — extractor broken?")
        assertTrue(offenders.isEmpty(), "shipped English claim(s) in production copy:\n${offenders.joinToString("\n")}")
    }

    @Test
    fun theGateSeesLiteralsAndIgnoresComments_discriminantHalf() {
        // Assembled from the lexicon so this file holds no contiguous claim literal of its own.
        val term = ForbiddenLexicon.ENGLISH_CLAIMS.first { it == "diagno" + "sis" }
        val source = """
            |// this $term mention is documentation, not copy
            |/* neither is this $term */
            |val shipped = "a $term in shipped copy"
            |val ident = ${term}Count
        """.trimMargin()

        val found = literalsOf(source)
            .flatMap { lit -> MindBoostCopySafetyScanner.scan(lit.text, ForbiddenLexicon.ENGLISH_CLAIMS) }

        // Exactly one: the literal. Two comment mentions and one identifier must not count, or the
        // gate would inherit the 180 false positives that made whole-file English matching unusable.
        assertEquals(1, found.size, "expected only the literal to match, got ${found.map { it.term }}")
    }

    @Test
    fun englishClaimsStayOutOfTheWholeFileLexicon() {
        // The separation IS the design. Promoting any of these into ALL restores whole-file English
        // matching and with it the 159 KDoc hits — the build would fail on honest documentation.
        val leaked = ForbiddenLexicon.ENGLISH_CLAIMS.filter { it in ForbiddenLexicon.ALL }
        assertTrue(leaked.isEmpty(), "English claim terms must not enter the whole-file lexicon: $leaked")

        // Discriminant half: the two lists are both non-empty, so the emptiness above means something.
        assertTrue(ForbiddenLexicon.ENGLISH_CLAIMS.size >= MIN_ENGLISH_TERMS)
        assertTrue(ForbiddenLexicon.ALL.size >= MIN_ENGLISH_TERMS)
    }

    // ---------------------------------------------------------------- surface

    /**
     * The Spanish gate walks the feature modules named in the build script; this gate walks the same
     * ones plus `design`. The list is PARSED, never hand-copied — see [ProductionCopyCorpus].
     */
    private fun productionFiles(): List<File> {
        val modules = ProductionCopyCorpus.scannedFeatureModules()
        assertTrue(modules.size >= MIN_MODULES, "parsed only ${modules.size} scanned modules — parser broken?")
        val roots = modules.map { "${ProductionCopyCorpus.COMMON_MAIN}/features/mindboost/$it" } +
            "${ProductionCopyCorpus.COMMON_MAIN}/design"
        return ProductionCopyCorpus.ktFilesUnder(roots)
    }

    private fun literalsOf(src: String): List<SourceLiteral> = ProductionCopyCorpus.literalsOf(src)

    private companion object {
        const val MIN_MODULES = 9
        const val MIN_FILES = 400
        const val MIN_LITERALS = 2_000
        const val MIN_ENGLISH_TERMS = 30
    }
}
