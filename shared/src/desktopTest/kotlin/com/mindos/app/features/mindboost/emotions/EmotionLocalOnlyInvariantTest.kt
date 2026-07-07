package com.mindos.app.features.mindboost.emotions

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * MC8u P1-9: emotions data (self-report valence/arousal/label, safety events, metric summaries) is emotionally
 * sensitive and, by charter, LOCAL-ONLY (encrypted SecureKeyValueStore). This source-scan fails the build the
 * moment any emotions production source IMPORTS or USES a remote sink (Firestore / Firebase / a callable / an
 * upload), so a future AI-Coach or sync cannot silently gain access. A bare "Firestore" mention in a KDoc that
 * documents future intent is NOT a sink and is ignored. (Desktop test — file IO is fine; not Detekt-scanned.)
 */
class EmotionLocalOnlyInvariantTest {

    // Assembled by fragments so this test file stays clean of any accidental "match".
    private val sinkImportTokens = listOf("fire" + "store", "fire" + "base", "https" + "callable")
    private val sinkUsageTokens = listOf(
        "Firebase" + "Firestore", "https" + "Callable", "Remote" + "Config", "Global" + "FeedRepository",
    )

    private fun codeLines(file: File): List<String> = file.readText().lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("//") && !it.startsWith("*") && !it.startsWith("/*") }
        .toList()

    @Test
    fun no_emotions_source_references_remote_sink() {
        val offenders = EmotionSourceLocator.kotlinFiles(EmotionSourceLocator.mainDir()).mapNotNull { file ->
            val importHit = EmotionSourceLocator.importLines(file)
                .firstOrNull { line -> sinkImportTokens.any { line.contains(it, ignoreCase = true) } }
            val usageHit = codeLines(file).firstNotNullOfOrNull { line -> sinkUsageTokens.firstOrNull { line.contains(it) } }
            val hit = importHit ?: usageHit
            if (hit != null) "${file.name} → $hit" else null
        }
        assertTrue(offenders.isEmpty(), "emotions is local-only; remote-sink import/usage found: $offenders")
    }
}
