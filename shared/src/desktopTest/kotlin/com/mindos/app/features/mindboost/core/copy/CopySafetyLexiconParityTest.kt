package com.mindos.app.features.mindboost.core.copy

import com.mindos.app.features.mindboost.emotions.domain.safety.EmotionTerminologyScanner
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * INFRA-05 · R1 — the copy-safety gate exists TWICE: [ForbiddenLexicon] (consumed by runtime compliance
 * tests) and a hand-copied list inside `MindBoostCopySafetyScannerTask` in `shared/build.gradle.kts`
 * (the build gate — it must live outside the scanned tree so it never self-trips). Both are declared
 * "keep in sync" by comment only, and a comment does not fail a build: adding a term to one list while
 * forgetting the other yields a gate that scans a DIFFERENT vocabulary than the tests assert, silently.
 *
 * This pins them. Same shape as `TemporalQualityThresholdsParityTest`: a sentinel-delimited block, a
 * set comparison, and a mutation half proving the comparison actually compares.
 *
 * `MB-EMO-0023` added a second thing to pin. Both sides hold FORMS, not lemmas: [ForbiddenLexicon]
 * declares an `Inflection` per term and expands it, because a lexicon of masculine-singular lemmas is
 * fail-open on every other form — and Spanish disclaimers are feminine ("evaluacion clinica"), so the
 * blind spot sat on the worst case. Pinning only the flat list would let someone reclassify every term
 * to LITERAL and keep this file green, so the expansion RULE is pinned as well, in both directions:
 * the adjectives must cover feminine and plural, and the terms declared LITERAL must NOT be widened
 * (those three were measured false positives on honest copy).
 *
 * `MB-EMO-0030` adds the third part. A gate is a mechanism, a vocabulary, and a way of comparing —
 * and only the first two were pinned. The accent fold and the `\b` anchoring also exist twice, so
 * this file now compares those too: every fold pair the build script declares must agree with the
 * runtime scanner AND vice versa (a per-pair check alone cannot see the runtime gaining a fold the
 * gate lacks), and the two `Regex(...)` constructors are read from their own sources and compared to
 * each other rather than to a literal kept here, which would go stale silently.
 */
class CopySafetyLexiconParityTest {

    @Test
    fun theBuildGateScansExactlyTheCanonicalLexicon() {
        val fromBuildScript = parseGradleLexicon()

        // Anti-vacuity: a regex that matched nothing would make the set comparison pass over an empty set.
        assertTrue(fromBuildScript.size >= MIN_EXPECTED_TERMS, "parsed only ${fromBuildScript.size} terms — parser broken?")
        assertEquals(ForbiddenLexicon.ALL.toSet(), fromBuildScript, "build-gate lexicon and ForbiddenLexicon diverged")
    }

    @Test
    fun theComparisonActuallyCompares_discriminantHalf() {
        // Drop one term from the parsed side: the assertion above must be able to see it.
        val mutated = parseGradleLexicon().toMutableSet()
        val dropped = mutated.first()
        mutated.remove(dropped)

        assertTrue(ForbiddenLexicon.ALL.toSet() != mutated, "a missing term must break parity, not pass silently")
    }

    @Test
    fun theCanonicalLexiconHasNoDuplicates() {
        // A duplicate would make the set comparison pass while the two lists differ in length.
        val duplicates = ForbiddenLexicon.ALL.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        assertTrue(duplicates.isEmpty(), "duplicated lexicon terms: $duplicates")
    }

    @Test
    fun everyGenderNumberLemmaIsMasculineSingular() {
        // The expansion builds the feminine by dropping the last character, so a lemma that is not a
        // masculine singular would quietly produce garbage forms instead of failing. The lexicon's KDoc
        // delegates this precondition here on purpose: a malformed lexicon must break the BUILD, never
        // throw at first access inside a user's session.
        val adjectives = ForbiddenLexicon.TERMS
            .filter { it.inflection == ForbiddenLexicon.Inflection.GENDER_NUMBER }
        val malformed = adjectives.filterNot { it.lemma.endsWith("o") }
        assertTrue(malformed.isEmpty(), "GENDER_NUMBER lemmas must be masculine singular: ${malformed.map { it.lemma }}")

        // Anti-vacuity: an empty filter would satisfy the assertion above without checking anything.
        assertTrue(adjectives.size >= MIN_EXPECTED_ADJECTIVES, "expected declared adjectives, found ${adjectives.size}")
    }

    @Test
    fun spanishAdjectivesCoverFeminineAndPlural() {
        // The finding itself: `\bclinico\b` never matched "clinica", so the gate ran green over a
        // production disclaimer. Roots are assembled from fragments so this file holds no contiguous
        // forbidden literal, the same hygiene the scanner and emotions compliance tests use.
        val roots = listOf("clin" + "ico", "diagnost" + "ico", "terap" + "eutico")
        for (root in roots) {
            val stem = root.dropLast(1)
            for (form in listOf(root, stem + "a", root + "s", stem + "as")) {
                assertTrue(form in ForbiddenLexicon.ALL, "lexicon must carry the form '$form'")
                assertTrue(
                    MindBoostCopySafetyScanner.containsForbidden("no es una evaluacion $form"),
                    "matcher must catch '$form' even when the sentence negates it",
                )
            }
        }
    }

    @Test
    fun literalTermsAreNotWidened_discriminantHalf() {
        // THREE of these are measured false positives of a blanket inflection rule, kept out by an
        // explicit declaration: "odd" is ordinary English (SeparatorItemFactoryTest), "cuotas" is this
        // repo's word for a sampling quota (VectorFilterPlanner), "low-stakes" is honest KDoc
        // (EmotionCalibrationPrompts). Reclassifying any of the three starts failing the build on copy
        // that is not a claim. "bets" and "bankrolls" are NOT in that class and this comment used to
        // say they were: re-measured for `MB-EMO-0031`, both hit ZERO times across the 934 files the
        // gate walks. They stay out because nothing asks them in, not because they would misfire.
        for (form in listOf("odd", "cuotas", "stakes", "bets", "bankrolls")) {
            assertFalse(form in ForbiddenLexicon.ALL, "'$form' must stay out of the lexicon")
        }
        // The half that proves the assertion above can fail: the lemmas themselves ARE still covered.
        for (lemma in listOf("odds", "cuota", "stake")) {
            assertTrue(lemma in ForbiddenLexicon.ALL, "the literal lemma '$lemma' must stay covered")
        }
    }

    @Test
    fun aPhraseIsStrictlyWeakerThanItsHeadWord() {
        // `MB-EMO-0031` asked for the LITERAL gambling lemmas to be narrowed into phrases, quoting the
        // lexicon's own "polysemic roots are PHRASES, never bare words" rule. Measured, that rule's
        // stated reason — false positives on legitimate copy — is exactly what these lemmas lack. On
        // the same 934 files the gate walks, the roots the repo DID turn into phrases hit constantly
        // (`transferencia` 34 times in 20 files, `mental` 13, `trabajo` 9, `CI` 5, `balance` 4), while
        // `cuota`, `stake` and `odds` hit ZERO. So the rule's precondition is absent, and what the
        // narrowing would actually do is what this test makes mechanical rather than argued: the
        // matcher is `\b`-anchored, so every occurrence of a phrase contains an occurrence of its head
        // word at the same index. A phrase can therefore only ever catch a SUBSET of what its head
        // catches — narrowing a blocking gate is a loosening, bought here for no measured relief.
        val candidates = listOf(
            "cuota" to "cuota de apuesta",
            "stake" to "stake de la apuesta",
            "odds" to "odds de la apuesta",
        )
        // Real gambling copy, plus the three honest sentences the measurement found in the tree.
        val corpus = listOf(
            "tu cuota de hoy es 2.5", "la cuota de apuesta es 2.5", "sube el stake y mira los odds",
            "stake de la apuesta bajo", "odds de la apuesta a favor",
            "cuotas balanceadas 50/50 congruente/incongruente", "a low-stakes warm-up that never counts",
            "a handful of odd items while the bulk stays a giveaway",
        )
        for ((head, phrase) in candidates) {
            val caughtByHead = corpus.filter { MindBoostCopySafetyScanner.containsForbidden(it, listOf(head)) }
            val caughtByPhrase = corpus.filter { MindBoostCopySafetyScanner.containsForbidden(it, listOf(phrase)) }

            assertTrue(
                caughtByHead.containsAll(caughtByPhrase),
                "'$phrase' catches copy that '$head' does not — the subset property this rests on is false",
            )
            // The discriminating half: the coverage loss is concrete, not hypothetical. Without it the
            // subset assertion would pass vacuously for a phrase that catches nothing at all.
            val lost = caughtByHead - caughtByPhrase.toSet()
            assertTrue(
                lost.isNotEmpty(),
                "narrowing '$head' to '$phrase' would lose no copy — then this test pins nothing",
            )
        }
    }

    @Test
    fun matcherAccentFoldIsPinnedInBothDirections() {
        // `MB-EMO-0030`: the WORDS were pinned above, the MATCHER was not. Both sides fold Spanish
        // diacritics with their own hand-written map, so dropping a pair on one side would leave the
        // other green while the two gates policed different rules — the same silent-drift shape the
        // lexicon parity exists to stop, one layer down.
        val declared = parseAccentPairs()
        assertTrue(declared.size >= MIN_ACCENT_PAIRS, "parsed only ${declared.size} fold pairs — parser broken?")

        for ((from, to) in declared) {
            assertEquals(
                to.toString(),
                MindBoostCopySafetyScanner.fold(from.toString()),
                "build gate folds '$from' to '$to'; the runtime scanner does not agree",
            )
        }

        // The other direction, which a per-pair check alone would miss: the runtime must not fold a
        // character the build gate leaves alone. Probes are built from code points, not literals, so
        // this pin cannot be broken by a re-encoding of its own source file.
        val accented = listOf(0xE1, 0xE9, 0xED, 0xF3, 0xFA, 0xFC, 0xF1).map { it.toChar() }
        val untouched = listOf(0xE0, 0xE8, 0xEC, 0xF2, 0xF9, 0xE7, 0xE2).map { it.toChar() }
        for (ch in accented + untouched) {
            val runtimeFolds = MindBoostCopySafetyScanner.fold(ch.toString()) != ch.toString()
            val gateDeclares = declared.containsKey(ch)
            assertEquals(gateDeclares, runtimeFolds, "fold coverage differs for '$ch' (U+${ch.code.toString(16)})")
        }
        // Anti-vacuity: the biconditional above is only informative if both classes are populated.
        assertTrue(accented.all { MindBoostCopySafetyScanner.fold(it.toString()) != it.toString() })
        assertTrue(untouched.all { MindBoostCopySafetyScanner.fold(it.toString()) == it.toString() })

        // The finding said this matcher existed twice. Measured, it existed FOUR times: the two above
        // plus EmotionTerminologyScanner (same map AND same `\b` construction) and a private fold in
        // AICoachSafetyPolicy. The third is public, so it is pinned here rather than left latent. The
        // fourth is GONE: `MB-EMO-0032` deleted that table and folded the coach onto the canonical
        // function, so there is nothing left to compare — its dependence on the fold is pinned by
        // behaviour instead, on the crisis path, in `AICoachSafetyPolicyTest`. Only the two Gradle-side
        // copies remain duplicated, and only because a build task cannot import module code.
        for (ch in accented + untouched) {
            assertEquals(
                MindBoostCopySafetyScanner.fold(ch.toString()),
                EmotionTerminologyScanner.fold(ch.toString()),
                "EmotionTerminologyScanner folds '$ch' differently from the canonical scanner",
            )
        }
    }

    @Test
    fun matcherWordAnchoringIsPinned() {
        // Neither side's expectation is hard-coded here: both constructors are read from source and
        // compared to each other, so this cannot pass by agreeing with a stale literal in this file.
        val fromGate = firstRegexConstructor(parseGradleMatcherBlock())
        val fromRuntime = firstRegexConstructor(locate(SCANNER_PATH).readText())
        assertEquals(fromRuntime, fromGate, "build gate and runtime scanner anchor matches differently")

        // Discriminant half: a changed anchor must be visible to the comparison above.
        assertTrue(fromGate.contains("\\\\b"), "expected word-boundary anchoring, got: $fromGate")
        assertTrue(fromRuntime != fromGate.replace("\\\\b", "\\\\B"), "the comparison must see an anchor change")
    }

    /**
     * `MB-EMO-0032`. The behavioural tests in `AICoachSafetyPolicyTest` catch a fold that DRIFTS; they
     * cannot catch a fold that is copied back correctly, because a correct copy behaves identically
     * today and only drifts later. This is the guard for that: production may declare a hand-rolled
     * accent table in exactly two places, and re-inlining a third fails here on the same day it is
     * written rather than on the day it diverges.
     *
     * Ceiling, declared, in both directions. A table built some third way — a `String` of characters
     * zipped, a `Char` arithmetic trick — is invisible to this: it pins the two constructions that
     * exist in the tree, not the idea. And [stripComments] is deliberately naive — it does not track
     * string literals, so a line-comment or block-comment opener appearing INSIDE one blanks source
     * that follows it and could hide a table declared after it. That direction is fail-OPEN, which is why
     * this guard is defence in depth behind the behavioural pins, never the only thing standing.
     */
    @Test
    fun productionDeclaresTheAccentTableInExactlyTwoPlaces() {
        val expected = setOf("MindBoostCopySafetyScanner.kt", "EmotionTerminologyScanner.kt")
        val commonMain = locate("shared/src/commonMain/kotlin/com/mindos/app")
        val files = commonMain.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue(files.size >= MIN_PRODUCTION_FILES, "walked only ${files.size} production files — root wrong?")

        val declaring = files.filter { ACCENT_TABLE.containsMatchIn(stripComments(it.readText())) }
        assertEquals(
            expected,
            declaring.map { it.name }.toSet(),
            "hand-rolled accent tables in production changed; the coach's own copy was deleted by MB-EMO-0032",
        )
        // Discriminating half: the detector must actually fire, or an empty result would read as clean.
        assertTrue(declaring.isNotEmpty(), "accent-table detector matched nothing — it is broken, not the tree")
    }

    /** Blanks out `//` and block comments so a table quoted in a KDoc is not read as a declaration. */
    private fun stripComments(src: String): String {
        val out = StringBuilder(src.length)
        var i = 0
        while (i < src.length) {
            val line = src.startsWith("//", i)
            val block = src.startsWith("/*", i)
            if (!line && !block) {
                out.append(src[i]); i++; continue
            }
            val close = if (line) src.indexOf('\n', i).takeIf { it >= 0 } ?: src.length
            else src.indexOf("*/", i + 2).takeIf { it >= 0 }?.plus(2) ?: src.length
            for (k in i until close) if (src[k] == '\n') out.append('\n')
            i = close
        }
        return out.toString()
    }

    private fun parseGradleMatcherBlock(): String {
        val script = locate("shared/build.gradle.kts").readText()
        val start = script.indexOf(MATCHER_START)
        val end = script.indexOf(MATCHER_END)
        assertTrue(start >= 0 && end > start, "matcher parity sentinels missing from shared/build.gradle.kts")
        return script.substring(start, end)
    }

    private fun parseAccentPairs(): Map<Char, Char> =
        Regex("'(.)'\\s*->\\s*'(.)'").findAll(parseGradleMatcherBlock())
            .associate { it.groupValues[1].first() to it.groupValues[2].first() }

    /** The first `Regex("...")` constructor in [source], balanced-paren delimited. */
    private fun firstRegexConstructor(source: String): String {
        val at = source.indexOf("Regex(\"")
        assertTrue(at >= 0, "no Regex(\"...\") constructor found")
        var depth = 0
        var i = at
        var end = -1
        while (i < source.length && end < 0) {
            if (source[i] == '(') depth++
            if (source[i] == ')') {
                depth--
                if (depth == 0) end = i + 1
            }
            i++
        }
        assertTrue(end > at, "unbalanced Regex(...) constructor")
        return source.substring(at, end)
    }

    private fun parseGradleLexicon(): Set<String> {
        val script = locate("shared/build.gradle.kts").readText()
        val start = script.indexOf(BLOCK_START)
        val end = script.indexOf(BLOCK_END)
        assertTrue(start >= 0 && end > start, "parity sentinels missing from shared/build.gradle.kts")

        val block = script.substring(start, end)
        return Regex("\"([^\"]+)\"").findAll(block).map { it.groupValues[1] }.toSet()
    }

    /** Walks up from the test working dir until the repo-relative path resolves (module vs root runner). */
    private fun locate(relative: String): File {
        var current: File? = File(".").absoluteFile
        while (current != null) {
            File(current, relative).takeIf { it.exists() }?.let { return it }
            current = current.parentFile
        }
        error("expected to locate $relative from ${File(".").absolutePath}")
    }

    private companion object {
        const val BLOCK_START = "COPY_SAFETY_LEXICON_PARITY_START"
        const val BLOCK_END = "COPY_SAFETY_LEXICON_PARITY_END"
        const val MATCHER_START = "COPY_SAFETY_MATCHER_PARITY_START"
        const val MATCHER_END = "COPY_SAFETY_MATCHER_PARITY_END"
        const val SCANNER_PATH =
            "shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/copy/MindBoostCopySafetyScanner.kt"
        const val MIN_EXPECTED_TERMS = 80
        const val MIN_EXPECTED_ADJECTIVES = 3
        const val MIN_ACCENT_PAIRS = 7
        const val MIN_PRODUCTION_FILES = 400

        /** The two constructions the tree actually uses: `mapOf('a-acute' to 'a')` and a `when` arm. */
        val ACCENT_TABLE = Regex("'á'\\s*(to|->)\\s*'a'")
    }
}
