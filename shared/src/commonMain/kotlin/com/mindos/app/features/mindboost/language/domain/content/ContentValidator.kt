package com.mindos.app.features.mindboost.language.domain.content

import com.mindos.app.features.mindboost.language.domain.model.TaskId
import com.mindos.app.features.mindboost.language.domain.model.TaskPassport

/**
 * Content validation (v6 §8.2 Content Compiler). The validator is the gate that stops
 * MindBoost from becoming "a library of pretty but psychometrically weak items".
 *
 * Design choice: it returns an **actionable report**, not a boolean. Every issue names a
 * stable [ContentValidationIssue.code], the offending [ContentValidationIssue.field] and a
 * human, fix-oriented [ContentValidationIssue.message]. [ValidationSeverity.BLOCKING]
 * issues forbid release; [ValidationSeverity.WARNING] issues require an uncertainty caveat
 * but don't block.
 *
 * Scope note: this validates passport *metadata completeness and cross-field consistency*.
 * Item-body checks that need the actual options/key (ambiguous key, non-diagnostic
 * distractors) require a richer `RawTask` and live in the Content Compiler slice; they are
 * intentionally out of scope here, not forgotten (§8.2).
 */
fun interface ContentValidator {
    fun validate(passport: TaskPassport): ContentValidationReport
}

/** Severity of a content issue. Only [BLOCKING] prevents an item from being released. */
enum class ValidationSeverity { BLOCKING, WARNING }

/** A single, actionable validation finding. */
data class ContentValidationIssue(
    val code: String,
    val field: String,
    val message: String,
    val severity: ValidationSeverity,
)

/** Full validation result for one passport. */
data class ContentValidationReport(
    val taskId: TaskId,
    val issues: List<ContentValidationIssue>,
) {
    val blocking: List<ContentValidationIssue> get() = issues.filter { it.severity == ValidationSeverity.BLOCKING }
    val warnings: List<ContentValidationIssue> get() = issues.filter { it.severity == ValidationSeverity.WARNING }

    /** An item may only enter the system when it has zero blocking issues. */
    val isValid: Boolean get() = blocking.isEmpty()
}
