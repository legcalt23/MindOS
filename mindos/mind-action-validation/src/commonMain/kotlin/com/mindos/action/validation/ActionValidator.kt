package com.mindos.action.validation

import com.mindos.action.model.MindAction
import com.mindos.action.model.ReferenceKind

/**
 * Per-request validation inputs. [readyReferenceKinds] expresses resolver readiness without
 * coupling validation to the resolver module; [principal] is an opaque identity for authorization.
 */
data class ValidationContext(
    val readyReferenceKinds: Set<ReferenceKind> = ReferenceKind.entries.toSet(),
    val principal: String? = null,
)

/** Authorization hook. Default allows everything; the app injects real policy. */
fun interface Authorizer {
    suspend fun isAuthorized(action: MindAction, context: ValidationContext): Boolean
}

object AllowAllAuthorizer : Authorizer {
    override suspend fun isAuthorized(action: MindAction, context: ValidationContext): Boolean = true
}

/**
 * Validates an action before any side-effect (ZIP03 ACTION_VALIDATOR). Checks required fields,
 * field format, authorization and resolver readiness. **Invalid actions must never execute.**
 */
interface ActionValidator {
    suspend fun validate(action: MindAction, context: ValidationContext): ValidationResult
}
