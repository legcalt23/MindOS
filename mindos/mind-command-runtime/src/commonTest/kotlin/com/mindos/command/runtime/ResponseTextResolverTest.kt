package com.mindos.command.runtime

import com.mindos.contracts.AmbiguousFailureDto
import com.mindos.contracts.NoMatchFailureDto
import com.mindos.contracts.RouterFailureResultDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ResponseTextResolverTest {
    private val resolver = DefaultResponseTextResolver()

    @Test
    fun localizesNoMatchAndAmbiguityWithoutLeakingCorrelationData() {
        val noMatch = CommandDispatchResult.RoutingFailed(
            "req-secret",
            RouterFailureResultDto("req-secret", NoMatchFailureDto),
        )
        val ambiguous = CommandDispatchResult.RoutingFailed(
            "req-secret",
            RouterFailureResultDto(
                "req-secret",
                AmbiguousFailureDto(listOf("open_profile", "open_chat")),
            ),
        )

        val spanish = resolver.resolve(noMatch, "es-ES").value
        val english = resolver.resolve(ambiguous, "en-US").value

        assertEquals("No he reconocido ese comando.", spanish)
        assertEquals("The command is ambiguous. Try being more specific.", english)
        assertFalse(spanish.contains("req-secret"))
        assertFalse(english.contains("open_profile"))
    }

    @Test
    fun doesNotTrustInjectedConfirmationPromptForUserResponse() {
        val result = CommandDispatchResult.NeedsConfirmation(
            requestId = "req-secret",
            actionType = "internal_action",
            prompt = "Leaked raw transcript and req-secret",
            token = ConfirmationToken("secret-token"),
            expiresAtMillis = 100L,
        )

        val response = resolver.resolve(result, "en").value

        assertEquals("Confirmation is required to continue.", response)
        assertFalse(response.contains("req-secret"))
        assertFalse(response.contains("raw transcript"))
    }

    @Test
    fun differentiatesSafetyDenial() {
        val result = CommandDispatchResult.Rejected(
            requestId = "req",
            reason = CommandRejectionReason.SafetyDenied("internal-policy-code"),
        )

        assertEquals("That command was denied for safety.", resolver.resolve(result, "und").value)
        assertEquals("Ese comando fue denegado por seguridad.", resolver.resolve(result, "es-MX").value)
    }
}
