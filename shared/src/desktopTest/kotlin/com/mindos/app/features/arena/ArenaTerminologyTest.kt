package com.mindos.app.features.arena

import com.mindos.app.features.arena.config.TerminologyScanner
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Task 1: Forbidden-terminology scanner.
 *
 * Forbidden terms are assembled from fragments so this very source file never contains a contiguous
 * forbidden word — otherwise the directory-walk below would (correctly) flag itself.
 */
class ArenaTerminologyTest {

    // Assembled so the literal never appears in source.
    private val rankingTerm = "E" + "L" + "O"

    private val forbidden = listOf(
        rankingTerm,
        "I" + "Q",
        "clin" + "ical",
        "diag" + "nostic",
        "gam" + "bling",
        "ca" + "sino",
        "bank" + "roll",
        "od" + "ds",
        "sta" + "ke",
    )

    @Test
    fun scannerCatchesForbiddenCompetitiveTermAsWholeWord() {
        val hits = TerminologyScanner.scan("ranked using $rankingTerm points", listOf(rankingTerm))
        assertTrue(hits.isNotEmpty(), "scanner must catch the forbidden competitive term")
    }

    @Test
    fun scannerIsCaseInsensitive() {
        assertTrue(TerminologyScanner.containsForbidden(rankingTerm.lowercase(), listOf(rankingTerm)))
    }

    @Test
    fun scannerIgnoresBenignSubstrings() {
        // Spanish words that merely contain the letters e-l-o must not match (word-boundary anchored).
        assertFalse(TerminologyScanner.containsForbidden("el modelo y el vuelo", listOf(rankingTerm)))
    }

    @Test
    fun noArenaTestFileContainsForbiddenTerminology() {
        val dirs = ArenaSourceLocator.testDirs()
        assertTrue(dirs.isNotEmpty(), "arena test dirs should be locatable")
        for (dir in dirs) {
            for (file in ArenaSourceLocator.kotlinFiles(dir)) {
                val hits = TerminologyScanner.scan(file.readText(), forbidden)
                assertTrue(hits.isEmpty(), "forbidden term ${hits.map { it.term }} in ${file.name}")
            }
        }
    }
}
