package com.mindos.app.features.mindboost.core.session.cognitive.evidence

/**
 * SYSTEM-1 6R3 §18 C3 (B2, kmp-architect W4 review) · A PURE, desktopTest-only mirror of the
 * `evidenceTopLevelDeclarationAllowlistScanner` Gradle task (`shared/build.gradle.kts`) — the FOURTH mechanical
 * barrier for the evidence-only contract, closing a gap the existing three leave open: the token scanner
 * (`evidenceBoundaryScanner`) bans specific type TOKENS, and the golden reflection test
 * ([EvidencePublicShapeGoldenTest]) pins the FIELD shape of the 24 EXISTING types it already knows to check.
 * Neither catches an in-package TOP-LEVEL extension property/function computing a scaled ratio that avoids every
 * banned token, nor a brand-new, differently-named top-level wrapper type the golden was never told to check.
 *
 * [topLevelDeclarationName] extracts the NAME of a top-level declaration from a single source line (column-0
 * anchored — a line starting with whitespace is a class/object MEMBER under this repo's enforced ktlint/detekt
 * formatting, never top-level), exposed standalone so [EvidenceTopLevelAllowlistBypassMutationTest] can call it
 * directly. [scanText] applies it over every line of a snippet/file and returns every extracted name NOT on
 * [allowedDeclarations] — empty means clean.
 *
 * [allowedDeclarations] is a deliberate COPY of the Gradle task's `evidenceTopLevelAllowedDeclarations` list, not a
 * shared reference across build units (the Gradle Kotlin DSL task and this JVM desktopTest compilation unit are
 * different compilation units) — kept in lockstep by [EvidenceTopLevelAllowlistParityTest], which reads
 * `shared/build.gradle.kts` as text and asserts the two never drift (same technique
 * [EvidenceScannerParityTest] already uses for the tier1/tier2 literal lists).
 *
 * OUT OF SCOPE (documented, not solved here): a computed MEMBER property with no backing field INSIDE an
 * already-pinned class (as opposed to a top-level extension) needs member-level parsing, disproportionate to a
 * plain-text regex scanner — still an open, still-honest gap.
 */
object EvidenceTopLevelScanRules {

    /** MUST stay byte-identical (same entries, SAME ORDER) to the Gradle task's `evidenceTopLevelAllowedDeclarations`
     *  — the exact 24 top-level declarations the real evidence package has today (cross-checked against
     *  [CognitiveSessionEvidenceBoundaryTest]'s own `protectedTypeNames` consumer-sweep list, which enumerates the
     *  same 24 names as the package's public type-level vocabulary). */
    val allowedDeclarations: List<String> = listOf(
        "CognitiveSessionEvidence", "ExerciseScaleEvidence", "ScoreScaleKey", "ComparabilityVerdict",
        "MeasurementLineage", "LineagePolicy", "SubjectScopedSessionBatch", "SessionEvidenceState",
        "EvidenceRouteEligibility", "SessionEvidenceDisposition", "SessionEvidenceStatePolicy", "ClockProvenance",
        "EvaluationAsOf", "RtCriticality", "RtCriticalityDeclaration", "RtCriticalityRegistry",
        "SessionEvidenceGates", "ExclusionReason", "ExclusionCount", "StateCount", "RouteEligibilityCount",
        "PolicyVersionBundle", "SessionEvidenceAudit", "CognitiveSessionEvidenceExtractor",
    )

    // Deliberate byte-identical copy of the Gradle task's `modifierAndKeyword` regex.
    private val modifierAndKeyword = Regex(
        "^(?:(?:public|internal|private|protected|sealed|data|enum|value|annotation|abstract|open|final|" +
            "inline|external|expect|actual|suspend|operator|infix|tailrec|const|override|noinline|" +
            "crossinline)\\s+)*(class|interface|object|fun|val|var|typealias)\\b(.*)$",
    )

    /**
     * The name of the top-level declaration on [rawLine], or `null` if [rawLine] is not a column-0-anchored
     * declaration at all (indented => a class/object member, never top-level; no recognized keyword; empty head
     * after the keyword). For `fun`/`val`/`var` this resolves the extension-form name, e.g. `fun Receiver.name(` ->
     * `"name"`, `val Receiver.name: Int` -> `"name"` — the receiver is discarded, only the declared member name
     * matters for the allowlist check.
     */
    fun topLevelDeclarationName(rawLine: String): String? {
        if (rawLine.isEmpty() || rawLine.first().isWhitespace()) return null
        val match = modifierAndKeyword.find(rawLine) ?: return null
        val keyword = match.groupValues[1]
        val rest = match.groupValues[2].trim()
        if (rest.isEmpty()) return null
        return if (keyword == "class" || keyword == "interface" || keyword == "object" || keyword == "typealias") {
            Regex("^([A-Za-z_][A-Za-z0-9_]*)").find(rest)?.groupValues?.get(1)
        } else {
            val withoutGenerics = Regex("^<[^>]*>\\s*").replace(rest, "")
            val head = if (keyword == "fun") {
                withoutGenerics.substringBefore("(")
            } else {
                withoutGenerics.substringBefore(":").substringBefore("=")
            }.trim()
            Regex("^[A-Za-z_][A-Za-z0-9_]*$").find(head.substringAfterLast("."))?.value
        }
    }

    /** Every top-level declaration name found in [text] that is NOT on [allowedDeclarations]. Empty = clean. */
    fun scanText(text: String): List<String> =
        text.lineSequence()
            .mapNotNull { topLevelDeclarationName(it) }
            .filterNot { it in allowedDeclarations }
            .toList()
}
