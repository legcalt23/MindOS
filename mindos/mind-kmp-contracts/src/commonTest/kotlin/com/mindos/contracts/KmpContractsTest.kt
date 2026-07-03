package com.mindos.contracts

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class KmpContractsTest {

    @Test
    fun actionContractsAreStronglyTypedAndSerializable() {
        val action: RouterActionDto = OpenProfileActionDto(requestId = "req-1", username = "ana")

        val payload = ContractJson.encodeAction(action)
        val decoded = ContractJson.decodeAction(payload)

        assertTrue(payload.contains("\"type\":\"open_profile\""))
        assertEquals(action, decoded)
        assertEquals("open_profile", decoded.actionType)
    }

    @Test
    fun storyOfUserContractHasStableGoldenJson() {
        val action: RouterActionDto = OpenStoryOfUserActionDto(requestId = "req-story", username = "marta")

        val payload = ContractJson.encodeAction(action)

        assertEquals(
            """{"type":"open_story_of_user","requestId":"req-story","username":"marta","schemaVersion":1}""",
            payload,
        )
        assertEquals(action, ContractJson.decodeAction(payload))
    }

    @Test
    fun routerResultsRoundTripWithNestedActionContract() {
        val result: RouterResultDto = RouterSuccessDto(
            requestId = "req-2",
            action = OpenChatActionDto(requestId = "req-2", chatRef = "team"),
        )

        val decoded = ContractJson.decodeResult(ContractJson.encodeResult(result))

        val success = assertIs<RouterSuccessDto>(decoded)
        assertEquals("req-2", success.requestId)
        assertEquals("team", (success.action as OpenChatActionDto).chatRef)
    }

    @Test
    fun failureResultsUseTypedReasonsNotStrings() {
        val result: RouterResultDto = RouterFailureResultDto(
            requestId = "req-3",
            reason = UnsafeInputFailureDto(listOf("UNSAFE_CONTROL_CHAR")),
        )

        val decoded = assertIs<RouterFailureResultDto>(ContractJson.decodeResult(ContractJson.encodeResult(result)))

        assertEquals(listOf("UNSAFE_CONTROL_CHAR"), assertIs<UnsafeInputFailureDto>(decoded.reason).codes)
    }

    @Test
    fun jsonPolicyIgnoresUnknownFieldsForBackwardCompatibility() {
        val payload = """
            {
              "type": "success",
              "requestId": "req-4",
              "schemaVersion": 1,
              "unknownFutureField": "ignored",
              "action": {
                "type": "open_settings",
                "requestId": "req-4",
                "schemaVersion": 1,
                "futureActionField": "ignored"
              },
              "warnings": []
            }
        """.trimIndent()

        val decoded = assertIs<RouterSuccessDto>(ContractJson.decodeResult(payload))

        assertIs<OpenSettingsActionDto>(decoded.action)
    }

    @Test
    fun entityContractsAreVersionedAndValidateRequiredFields() {
        val user: EntityDto = UserEntityDto(id = "u_1", username = "ana")

        assertEquals(ContractSchema.Current, user.schemaVersion)
        assertFailsWith<IllegalArgumentException> { UserEntityDto(id = " ", username = "ana") }
    }

    @Test
    fun validatorReturnsTypedValidationReport() {
        val valid = DefaultActionContractValidator.validate(OpenMessagesActionDto(requestId = "req-5"))
        val invalid = DefaultActionContractValidator.validate(
            OpenProfileActionDto(requestId = "req-6", username = "ana", schemaVersion = 99),
        )

        assertIs<ValidValidationReportDto>(valid)
        val issues = assertIs<InvalidValidationReportDto>(invalid).issues
        assertEquals(ValidationIssueCode.UNSUPPORTED_SCHEMA_VERSION, issues.single().code)
    }

    @Test
    fun routerApiIsTestableWithAFakeImplementation() = runTest {
        val router = object : KmpCommandRouter {
            override suspend fun route(request: RouterRequestDto): RouterResultDto =
                RouterFailureResultDto(request.requestId, NoMatchFailureDto)
        }

        val result = router.route(RouterRequestDto(requestId = "req-7", input = "unknown"))

        assertIs<NoMatchFailureDto>(assertIs<RouterFailureResultDto>(result).reason)
    }

    @Test
    fun routerRequestCarriesOpaqueSecurityContext() {
        val request = RouterRequestDto(
            requestId = "req-voice",
            input = "abre ajustes",
            localeTag = "es-US",
            sessionId = "opaque-session",
            source = RouterCommandSourceDto.VOICE_BATCH,
            abuseContext = RouterAbuseContextDto(
                opaqueUserHash = "opaque-user",
                deviceHash = "opaque-device",
                sessionHash = "opaque-session",
            ),
        )

        assertEquals("opaque-session", request.abuseContext.sessionHash)
        assertEquals(RouterCommandSourceDto.VOICE_BATCH, request.source)
    }
}
