package com.mindos.action.validation

import com.mindos.action.model.MindAction

/**
 * Default validator: collects all failures (not fail-fast) so callers see every problem.
 * Order of checks is irrelevant to the result — deterministic.
 */
class DefaultActionValidator(
    private val authorizer: Authorizer = AllowAllAuthorizer,
    private val maxReferenceLength: Int = 256,
) : ActionValidator {

    override suspend fun validate(action: MindAction, context: ValidationContext): ValidationResult {
        val failures = ArrayList<ValidationFailure>()

        // Required fields + format.
        for (ref in action.references()) {
            when {
                ref.rawValue.isBlank() ->
                    failures += ValidationFailure.MissingField(ref.kind.id, "reference '${ref.kind.id}' is blank")
                ref.rawValue.length > maxReferenceLength ->
                    failures += ValidationFailure.MalformedField(ref.kind.id, "reference '${ref.kind.id}' exceeds $maxReferenceLength chars")
            }
        }

        // Resolver readiness.
        action.references().map { it.kind }.toSet().forEach { kind ->
            if (kind !in context.readyReferenceKinds) {
                failures += ValidationFailure.ResolverNotReady(kind, "no resolver registered for '${kind.id}'")
            }
        }

        // Authorization.
        if (!authorizer.isAuthorized(action, context)) {
            failures += ValidationFailure.Unauthorized("principal not authorized for '${action.actionType}'")
        }

        return if (failures.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(failures)
    }
}
