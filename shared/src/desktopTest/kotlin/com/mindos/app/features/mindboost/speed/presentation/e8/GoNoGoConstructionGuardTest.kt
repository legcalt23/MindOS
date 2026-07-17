package com.mindos.app.features.mindboost.speed.presentation.e8

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * SYSTEM-1 slice 6-R3 (P2-01, item 6 -- build-time defense) · [GoNoGoSessionStateHolder]'s `sessionRecordStore`
 * and `cognitiveRunStore` are MANDATORY, no-default constructor parameters (see its KDoc): a default
 * `InMemory*Store()` would let a careless production call-site silently persist E8's write-ahead lifecycle or
 * official cognitive history to a throwaway in-process store, losing per-account isolation. The Kotlin compiler
 * enforces "every call site must supply a value" only until someone re-adds a default -- at which point the
 * compiler falls silent, both for a NEW careless call site (compiles fine, silently omits the arg) and for a
 * regression that reintroduces the default with NO call site changing at all. These checks scan the REAL
 * production source tree as TEXT (not the compiled symbol table), so both regressions are caught even though
 * they would still compile. Desktop test: file IO is fine; not Detekt-scanned.
 */
class GoNoGoConstructionGuardTest {

    @Test
    fun everyProductionConstructionSite_suppliesBothMandatoryStores() {
        val offenders = commonMainSources().flatMap { file ->
            extractCalls(file.readText(), "GoNoGoSessionStateHolder(").mapNotNull { callBody ->
                val missing = listOf("sessionRecordStore", "cognitiveRunStore").filterNot { callBody.contains(it) }
                if (missing.isEmpty()) null else "${file.path} is missing: $missing"
            }
        }
        assertTrue(
            offenders.isEmpty(),
            "every production GoNoGoSessionStateHolder(...) call must name both mandatory stores; found: $offenders",
        )
    }

    /** Guards against the check above vacuously passing if the construction site were ever removed/renamed. */
    @Test
    fun atLeastOneProductionConstructionSiteExists() {
        val callSites = commonMainSources().sumOf { extractCalls(it.readText(), "GoNoGoSessionStateHolder(").size }
        assertTrue(callSites > 0, "expected at least one production construction site of GoNoGoSessionStateHolder")
    }

    @Test
    fun theConstructorNeverReintroducesAnInMemoryDefault_forEitherMandatoryStore() {
        val holderFile = commonMainSources().single { it.name == "GoNoGoSessionStateHolder.kt" }
        val text = holderFile.readText()
        val reintroduced = listOf(
            Regex("""sessionRecordStore\s*:\s*SessionRecordStore\s*="""),
            Regex("""cognitiveRunStore\s*:\s*CognitiveSessionRunStore\s*="""),
        ).filter { it.containsMatchIn(text) }
        assertTrue(
            reintroduced.isEmpty(),
            "GoNoGoSessionStateHolder's mandatory stores must never regain a default value; matched: $reintroduced",
        )
    }

    /** Balanced-paren extraction of every `token`+`(...)` call body in [text] (handles nested calls in args). */
    private fun extractCalls(text: String, token: String): List<String> {
        val bodies = mutableListOf<String>()
        var searchFrom = 0
        while (true) {
            val start = text.indexOf(token, searchFrom)
            if (start < 0) break
            val openParen = start + token.length - 1
            var depth = 1
            var i = openParen + 1
            while (i < text.length && depth > 0) {
                when (text[i]) {
                    '(' -> depth++
                    ')' -> depth--
                }
                i++
            }
            bodies.add(text.substring(openParen + 1, i - 1))
            searchFrom = i
        }
        return bodies
    }

    private fun commonMainSources(): List<File> {
        val root = locate("src/commonMain/kotlin")
            ?: error("expected to locate the commonMain root from ${File(".").absolutePath}")
        return root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    private fun locate(relative: String): File? {
        var base: File? = File(".").absoluteFile
        repeat(MAX_DEPTH) {
            val current = base ?: return null
            File(current, relative).takeIf { it.isDirectory }?.let { return it }
            File(current, "shared/$relative").takeIf { it.isDirectory }?.let { return it }
            base = current.parentFile
        }
        return null
    }

    private companion object {
        const val MAX_DEPTH = 8
    }
}
