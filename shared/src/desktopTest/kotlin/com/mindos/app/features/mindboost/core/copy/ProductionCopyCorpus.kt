package com.mindos.app.features.mindboost.core.copy

import java.io.File

/** One Kotlin string literal with its 1-based line number. */
internal data class SourceLiteral(val text: String, val line: Int)

/**
 * The shipped-copy corpus the literal-scoped gates read.
 *
 * There are two of those gates now — [ShippedEnglishCopyGateTest] (`MB-EMO-0029`) and
 * [FocusClaimLevelGateTest] (`MB-FOCUS-0089`) — and they share a subject: what the product actually
 * SAYS, as opposed to what its comments reason about. A second hand-written copy of this extractor
 * would be a second owner of that definition, which is the drift `CopySafetyLexiconParityTest`
 * exists to prevent one level up, so it lives here once.
 *
 * A gate over prose is only as good as its extractor: one that silently parsed nothing would report
 * the whole tree clean. Both callers therefore assert a floor on what came back, and the extractor
 * itself is pinned by a discriminating test that feeds it the same term as a comment, as an
 * identifier and as a literal, and requires only the literal to count.
 *
 * Declared ceiling, unchanged from the English gate: this reads SOURCE TEXT. A claim assembled at
 * runtime from fragments, loaded from a resource, or interpolated from a variable is invisible to
 * it. Interpolation is the sharp edge: a literal that merely embeds a template expression is still
 * read whole, so its fixed words are caught, but a claim split across two interpolated halves is
 * not — the same ceiling the whole-file Gradle scanner has, and it is declared, not assumed away.
 */
internal object ProductionCopyCorpus {

    const val COMMON_MAIN: String = "shared/src/commonMain/kotlin/com/mindos/app"

    private const val MODULE_LIST_DECL = "val mindboostCopyFeatureModules"
    private const val MODULE_LIST_WINDOW = 400
    private const val TRIPLE_QUOTE = "\"\"\""

    /** Walks up from the test working dir until the repo-relative path resolves. */
    fun locate(relative: String): File? {
        var current: File? = File(".").absoluteFile
        var found: File? = null
        while (current != null && found == null) {
            found = File(current, relative).takeIf { it.exists() }
            current = current.parentFile
        }
        return found
    }

    /** Every `.kt` file under the given repo-relative roots, skipping roots that do not resolve. */
    fun ktFilesUnder(roots: List<String>): List<File> =
        roots.mapNotNull { locate(it) }
            .flatMap { dir -> dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList() }

    /**
     * The feature-module list the Gradle copy-safety scanner walks, PARSED from the build script
     * rather than re-declared: a module added to that gate is covered here the same day.
     */
    fun scannedFeatureModules(): List<String> {
        val script = requireNotNull(locate("shared/build.gradle.kts")) { "build script not found" }.readText()
        val at = script.indexOf(MODULE_LIST_DECL)
        require(at >= 0) { "`$MODULE_LIST_DECL` not found in shared/build.gradle.kts" }
        val block = script.substring(at, minOf(at + MODULE_LIST_WINDOW, script.length))
        val list = block.substringAfter("listOf(").substringBefore(")")
        return Regex("\"([^\"]+)\"").findAll(list).map { it.groupValues[1] }.toList()
    }

    /**
     * Kotlin string literals with their 1-based line, comments excluded. Crude but sufficient, and
     * PROVEN by the discriminating test on each caller — an extractor nobody tested is how a green
     * gate ends up scanning nothing.
     */
    @Suppress("NestedBlockDepth")
    fun literalsOf(src: String): List<SourceLiteral> {
        val out = mutableListOf<SourceLiteral>()
        val end = src.length
        var i = 0
        var line = 1
        fun advanceTo(target: Int) {
            var k = i
            while (k < target) {
                if (src[k] == '\n') line++
                k++
            }
            i = target
        }
        while (i < end) {
            when {
                src.startsWith("//", i) -> advanceTo(src.indexOf('\n', i).takeIf { it >= 0 } ?: end)
                src.startsWith("/*", i) -> advanceTo(src.indexOf("*/", i + 2).takeIf { it >= 0 }?.plus(2) ?: end)
                src.startsWith(TRIPLE_QUOTE, i) -> {
                    val close = src.indexOf(TRIPLE_QUOTE, i + 3).takeIf { it >= 0 } ?: end
                    out += SourceLiteral(src.substring(i + 3, close), line)
                    advanceTo(minOf(close + 3, end))
                }
                src[i] == '"' -> {
                    var j = i + 1
                    while (j < end && src[j] != '"' && src[j] != '\n') {
                        if (src[j] == '\\') j++
                        j++
                    }
                    val close = minOf(j, end)
                    out += SourceLiteral(src.substring(i + 1, close), line)
                    advanceTo(minOf(close + 1, end))
                }
                else -> advanceTo(i + 1)
            }
        }
        return out
    }
}
