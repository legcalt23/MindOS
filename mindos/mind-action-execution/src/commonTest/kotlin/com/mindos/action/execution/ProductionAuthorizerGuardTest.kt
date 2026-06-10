package com.mindos.action.execution

import com.mindos.action.validation.ActionValidator
import com.mindos.action.validation.Authorizer
import com.mindos.action.validation.DefaultActionValidator
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * P1.3: production wiring must reject a DefaultActionValidator left on the
 * permissive AllowAllAuthorizer default, so a deployment can never ship with
 * authorization effectively disabled.
 */
class ProductionAuthorizerGuardTest {

    private fun productionConfigWith(validator: ActionValidator) = ActionExecutionRuntimeConfig(
        environment = ActionExecutionEnvironment.PRODUCTION,
        validator = validator,
        resolvers = sampleResolvers(),
        coordinator = FakeCoordinator(),
    )

    @Test
    fun productionRejectsAllowAllAuthorizerDefault() {
        val ex = assertFailsWith<IllegalArgumentException> {
            productionConfigWith(DefaultActionValidator())
        }
        assertTrue(
            ex.message?.contains("AllowAllAuthorizer") == true,
            "expected the production wiring failure to flag AllowAllAuthorizer, was: ${ex.message}",
        )
    }

    @Test
    fun productionDoesNotFlagAuthorizerWhenRealPolicyProvided() {
        val realAuthorizer = Authorizer { _, _ -> true }
        // Config is still unsafe for other (NoOp) reasons, so it throws — but it
        // must NOT cite the authorizer once a real policy is injected.
        val ex = assertFailsWith<IllegalArgumentException> {
            productionConfigWith(DefaultActionValidator(realAuthorizer))
        }
        assertFalse(
            ex.message?.contains("AllowAllAuthorizer") == true,
            "real authorizer must not be flagged as permissive, was: ${ex.message}",
        )
    }
}
