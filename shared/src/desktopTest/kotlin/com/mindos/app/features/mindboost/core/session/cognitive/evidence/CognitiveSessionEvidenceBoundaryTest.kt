package com.mindos.app.features.mindboost.core.session.cognitive.evidence

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * SYSTEM-1 slice 6-R2/6-R3 · The MECHANICAL no-wiring barrier (P1-02/P1-11) PLUS the facts-only anti-regression
 * barrier (6R3 §7/§10). Four complementary checks:
 *  1. NO `import com.mindos.app.features.mindboost.profile.` line — structural, forward-proof (catches any current
 *     or future profile symbol, including a wildcard import of a type not yet named below).
 *  2. NO whole-file (comments included) word-boundary occurrence of a specific retired symbol — a tripwire against
 *     a fully-qualified inline reference or a "wire X here" comment.
 *  3. NO wiring to an aggregator/remote sink (Firestore/SyncQueue/aggregate()/service).
 *  4. NO reintroduction of a retired EVALUATIVE field/type shape (6R3 §7): Tier 1 bans the type tokens an
 *     evaluative ratio/score needs to exist at all; Tier 2 bans the specific retired field/type names.
 * Word-boundary (not raw `contains`) so this package's own `SessionEvidenceGates`/`SessionEvidenceAudit` never
 * false-positive against the banned `SessionEvidence` type. Desktop test: file IO is fine; not Detekt-scanned.
 */
class CognitiveSessionEvidenceBoundaryTest {

    private val forbiddenSymbols = listOf(
        "MindBoostProfileAggregator",
        "toCognitiveModuleId",
        "CognitiveModuleId",
        "DimensionStatus",
        "ProfileTrend",
        "SessionEvidence",
        "CalibrationPolicy",
        "ModuleContribution",
        "toModuleContribution",
        "ProfileEstimate",
        "CalibrationState",
    )

    // Consolidated (test-engineer, 6-R3 mutation-fixture pass): both lists now live in the single
    // EvidenceShapeScanRules test-side object (same package, same desktopTest compilation unit, byte-identical
    // values) so THIS real-tree scan and the permanent synthetic mutation fixture (EvidenceShapeScanRulesTest)
    // cannot drift from each other. The Gradle `evidenceBoundaryScanner` task keeps its OWN independent copy on
    // purpose (a different compilation unit — the Gradle Kotlin DSL cannot import desktopTest sources), preserving
    // the Gradle-vs-test "twin" defense-in-depth the reopen doc §10 calls for.
    private val tier1BannedTypeTokens = EvidenceShapeScanRules.tier1BannedTypeTokens
    private val tier2RetiredEvaluativeLiterals = EvidenceShapeScanRules.tier2RetiredEvaluativeLiterals

    @Test
    fun evidence_package_never_imports_profile() {
        val offenders = evidenceSources().flatMap { file ->
            file.readText().lineSequence()
                .map { it.trim() }
                .filter { it.startsWith("import com.mindos.app.features.mindboost.profile.") }
                .map { "${file.name}: $it" }
        }
        assertTrue(offenders.isEmpty(), "evidence package must never import profile.*; found: $offenders")
    }

    @Test
    fun evidence_package_never_reintroduces_profile_currency_symbols() {
        val offenders = evidenceSources().flatMap { file ->
            val text = file.readText()
            forbiddenSymbols
                .filter { Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(text) }
                .map { "${file.name} -> $it" }
        }
        assertTrue(offenders.isEmpty(), "evidence package must stay profile-currency-free; found: $offenders")
    }

    @Test
    fun evidence_package_is_not_wired_to_an_aggregator_or_remote_sink() {
        val forbidden = listOf("Firestore", "SyncQueue", ".aggregate(", "MindBoostProfileService")
        val offenders = evidenceSources().flatMap { file ->
            file.readText().lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("//") && !it.startsWith("*") && !it.startsWith("/*") }
                .filter { line -> forbidden.any { line.contains(it) } }
                .map { "${file.name}: $it" }
        }
        assertTrue(offenders.isEmpty(), "evidence package must stay unwired; found: $offenders")
    }

    @Test
    fun evidence_package_never_reintroduces_a_retired_evaluative_shape() {
        val banned = tier1BannedTypeTokens + tier2RetiredEvaluativeLiterals
        val offenders = evidenceSources().flatMap { file ->
            val text = file.readText()
            banned
                .filter { Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(text) }
                .map { "${file.name} -> $it" }
        }
        assertTrue(offenders.isEmpty(), "evidence package must stay facts-only (6R3 §7); found: $offenders")
    }

    /**
     * 6R3 §18 C2.2 · [ComparabilityVerdict.Bridged] is retained vocabulary (a real, versioned equating authority
     * would need it) but has NO construction site until that authority exists. The only line naming `Bridged(`
     * anywhere in the package must be its own declaration.
     */
    @Test
    fun bridged_comparability_has_no_construction_site_in_production() {
        val offenders = evidenceSources().flatMap { file ->
            file.readText().lineSequence()
                .map { it.trim() }
                .filter { it.contains("Bridged(") && !it.startsWith("data class Bridged") }
                .map { "${file.name}: $it" }
        }
        assertTrue(
            offenders.isEmpty(),
            "Bridged must have no construction site until a versioned equating authority exists; found: $offenders",
        )
    }

    /** 6R3 §18 C2.2 · No type in the evidence package is `@Serializable` yet — a future persistence schema is a
     *  deliberate, versioned decision, never an accidental annotation drift. */
    @Test
    fun evidence_package_carries_no_serializable_annotation_yet() {
        val offenders = evidenceSources().flatMap { file ->
            file.readText().lineSequence()
                .map { it.trim() }
                .filter { it.startsWith("@Serializable") }
                .map { "${file.name}: $it" }
        }
        assertTrue(offenders.isEmpty(), "no evidence type should be @Serializable without a deliberate schema " +
            "decision; found: $offenders")
    }

    /**
     * 6R3 §18 C3 (consumer-sweep, desktopTest twin of the Gradle `evidenceConsumerSweepScanner` task) · No
     * commonMain file OUTSIDE this package may reference one of its public type names — the mechanical barrier
     * that blocks a future profile/rank/cloud wiring attempt until an explicit, reviewed decision. The allowlist
     * mirrors the Gradle task's: exactly one file, a pre-existing KDoc cross-reference (not an import/construction)
     * this agent must not edit (owned by a different wave).
     */
    @Test
    fun no_commonMain_file_outside_the_evidence_package_references_its_public_types() {
        val commonMainRoot = locate("src/commonMain/kotlin")
            ?: error("expected to locate the commonMain root from ${File(".").absolutePath}")
        val evidenceDir = evidenceSources().first().parentFile
        val allowlist = setOf(
            File(
                commonMainRoot,
                "com/mindos/app/features/mindboost/core/session/cognitive/SecureCognitiveSessionRunStore.kt",
            ).canonicalFile,
        )
        val protectedTypeNames = listOf(
            "CognitiveSessionEvidence", "ExerciseScaleEvidence", "MeasurementLineage", "SubjectScopedSessionBatch",
            "CognitiveSessionEvidenceExtractor", "EvaluationAsOf", "ClockProvenance", "ScoreScaleKey",
            "ComparabilityVerdict", "LineagePolicy", "ExclusionReason", "ExclusionCount", "StateCount",
            "RouteEligibilityCount", "PolicyVersionBundle", "SessionEvidenceAudit", "SessionEvidenceState",
            "EvidenceRouteEligibility", "SessionEvidenceDisposition", "SessionEvidenceStatePolicy", "RtCriticality",
            "RtCriticalityDeclaration", "RtCriticalityRegistry", "SessionEvidenceGates",
        )
        val evidencePath = evidenceDir.canonicalFile.toPath()
        val offenders = commonMainRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.canonicalFile.toPath().startsWith(evidencePath) }
            .filterNot { it.canonicalFile in allowlist }
            .flatMap { file ->
                val text = file.readText()
                protectedTypeNames
                    .filter { Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(text) }
                    .map { "${file.path} -> $it" }
            }
            .toList()
        assertTrue(
            offenders.isEmpty(),
            "no commonMain file outside the evidence package may reference its public types; found: $offenders",
        )
    }

    private fun evidenceSources(): List<File> {
        val relative = "src/commonMain/kotlin/com/mindos/app/features/mindboost/core/session/cognitive/evidence"
        val dir = locate(relative)
        val files = dir?.walkTopDown()?.filter { it.isFile && it.extension == "kt" }?.toList() ?: emptyList()
        assertTrue(files.isNotEmpty(), "expected to locate evidence package sources from ${File(".").absolutePath}")
        return files
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
