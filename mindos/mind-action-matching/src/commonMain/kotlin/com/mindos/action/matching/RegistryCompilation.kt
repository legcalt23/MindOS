package com.mindos.action.matching

import com.mindos.action.entity.EntityExtractorRegistry
import com.mindos.action.normalization.NormalizationConfig

/** Severity of a compilation issue. ERROR fails the build; WARNING is informational. */
enum class IssueSeverity { ERROR, WARNING }

/** Categories of issues the compiler can report (ZIP02 §7). */
enum class IssueCode {
    EMPTY_REGISTRY,
    DUPLICATE_PATTERN_ID,
    TEMPLATE_PARSE_ERROR,
    UNSUPPORTED_SLOT_TYPE,
    INVALID_GREEDY_SLOT,
    EMPTY_EXACT_PHRASE,
    DUPLICATE_EXACT_PHRASE,
    DUPLICATE_ALIAS,
    PATTERN_CONFLICT,
    INCOMPLETE_LOCALE,
}

/** A single compile-time finding with a human-actionable [message]. */
data class RegistryIssue(
    val severity: IssueSeverity,
    val code: IssueCode,
    val message: String,
)

/** Options controlling compilation. Conflicts fail the build by default (fail at build, not prod). */
data class CompileOptions(
    val normalization: NormalizationConfig = NormalizationConfig.Default,
    val entityRegistry: EntityExtractorRegistry = EntityExtractorRegistry.default(),
    /** When true, [IssueCode.PATTERN_CONFLICT] is an ERROR; when false it is a WARNING. */
    val failOnConflicts: Boolean = true,
)

/** Outcome of compilation. A typed result — never throws for invalid registries. */
sealed interface RegistryCompilationResult {
    val warnings: List<RegistryIssue>

    data class Success(
        val registry: CompiledRegistry,
        override val warnings: List<RegistryIssue>,
    ) : RegistryCompilationResult

    data class Failure(
        val errors: List<RegistryIssue>,
        override val warnings: List<RegistryIssue>,
    ) : RegistryCompilationResult

    /** Convenience for tests/wiring that treat a failed compile as fatal. */
    fun getOrThrow(): CompiledRegistry = when (this) {
        is Success -> registry
        is Failure -> error(
            "Registry compilation failed with ${errors.size} error(s):\n" +
                errors.joinToString("\n") { " - [${it.code}] ${it.message}" },
        )
    }
}
