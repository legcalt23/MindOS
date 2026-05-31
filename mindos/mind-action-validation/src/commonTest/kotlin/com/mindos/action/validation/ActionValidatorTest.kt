package com.mindos.action.validation

import com.mindos.action.model.MindAction
import com.mindos.action.model.ReferenceKind
import com.mindos.action.model.RequestId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class ActionValidatorTest {

    private val id = RequestId("r1")
    private val validator = DefaultActionValidator()

    @Test
    fun validActionPasses() = runTest {
        val r = validator.validate(MindAction.OpenProfile(id, "ana"), ValidationContext())
        assertTrue(r is ValidationResult.Valid)
    }

    @Test
    fun referencelessActionIsValid() = runTest {
        assertTrue(validator.validate(MindAction.OpenSettings(id), ValidationContext()) is ValidationResult.Valid)
    }

    @Test
    fun blankReferenceIsMissingField() = runTest {
        val r = validator.validate(MindAction.OpenProfile(id, "  "), ValidationContext())
        assertTrue((r as ValidationResult.Invalid).failures.any { it is ValidationFailure.MissingField })
    }

    @Test
    fun tooLongReferenceIsMalformed() = runTest {
        val r = validator.validate(MindAction.OpenProfile(id, "a".repeat(300)), ValidationContext())
        assertTrue((r as ValidationResult.Invalid).failures.any { it is ValidationFailure.MalformedField })
    }

    @Test
    fun resolverNotReadyIsReported() = runTest {
        val ctx = ValidationContext(readyReferenceKinds = emptySet())
        val r = validator.validate(MindAction.OpenProfile(id, "ana"), ctx)
        assertTrue((r as ValidationResult.Invalid).failures.any { it is ValidationFailure.ResolverNotReady })
    }

    @Test
    fun unauthorizedIsReported() = runTest {
        val deny = Authorizer { _, _ -> false }
        val r = DefaultActionValidator(deny).validate(MindAction.OpenSettings(id), ValidationContext())
        assertTrue((r as ValidationResult.Invalid).failures.any { it is ValidationFailure.Unauthorized })
    }
}
