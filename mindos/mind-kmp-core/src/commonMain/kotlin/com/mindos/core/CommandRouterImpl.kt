package com.mindos.core

import com.mindos.contracts.ActionContractValidator
import com.mindos.contracts.ContractSchema
import com.mindos.contracts.KmpCommandRouter
import com.mindos.contracts.OpenChatActionDto
import com.mindos.contracts.OpenMessagesActionDto
import com.mindos.contracts.OpenProfileActionDto
import com.mindos.contracts.OpenSettingsActionDto
import com.mindos.contracts.OpenStoryActionDto
import com.mindos.contracts.RouterActionDto
import com.mindos.contracts.RouterFailureResultDto
import com.mindos.contracts.RouterRequestDto
import com.mindos.contracts.RouterResultDto
import com.mindos.contracts.RouterSuccessDto
import com.mindos.contracts.UnsupportedSchemaVersionFailureDto
import com.mindos.contracts.ValidValidationReportDto
import com.mindos.contracts.ValidationFailedFailureDto
import com.mindos.contracts.ValidationIssueCode
import com.mindos.contracts.ValidationIssueDto

class CommandRouterImpl(
    private val matcher: PatternMatcher = PatternMatcherImpl(),
    private val extractor: EntityExtractorImpl = EntityExtractorImpl(),
    private val validator: ActionContractValidator = ActionValidatorImpl(),
) : KmpCommandRouter {

    override suspend fun route(request: RouterRequestDto): RouterResultDto {
        if (request.schemaVersion != ContractSchema.Current) {
            return RouterFailureResultDto(
                requestId = request.requestId,
                reason = UnsupportedSchemaVersionFailureDto(ContractSchema.Current, request.schemaVersion),
            )
        }

        return when (val match = matcher.match(request)) {
            is CoreMatchResult.Matched -> {
                val action = match.toAction(request.requestId)
                    ?: return RouterFailureResultDto(
                        request.requestId,
                        ValidationFailedFailureDto(
                            listOf(
                                ValidationIssueDto(
                                    field = "actionType",
                                    code = ValidationIssueCode.UNSUPPORTED_ACTION,
                                    message = "Unsupported action type '${match.actionType}'.",
                                ),
                            ),
                        ),
                    )
                when (val validation = validator.validate(action)) {
                    is ValidValidationReportDto -> RouterSuccessDto(request.requestId, action)
                    is com.mindos.contracts.InvalidValidationReportDto -> RouterFailureResultDto(
                        request.requestId,
                        ValidationFailedFailureDto(validation.issues),
                    )
                }
            }
            is CoreMatchResult.Ambiguous -> RouterFailureResultDto(
                request.requestId,
                RouterError.AmbiguousCommand(match.candidateActionTypes).toFailureDto(),
            )
            is CoreMatchResult.Failed -> RouterFailureResultDto(
                request.requestId,
                match.error.toFailureDto(),
            )
        }
    }

    fun extract(input: String, typeId: String): CoreEntityExtractionResult =
        extractor.extract(input, typeId)

    private fun CoreMatchResult.Matched.toAction(requestId: String): RouterActionDto? =
        when (actionType) {
            "open_profile" -> entities["target"]?.let { OpenProfileActionDto(requestId, it) }
            "open_story" -> entities["storyId"]?.let { OpenStoryActionDto(requestId, it) }
            "open_chat" -> entities["chatId"]?.let { OpenChatActionDto(requestId, it) }
            "open_messages" -> OpenMessagesActionDto(requestId)
            "open_settings" -> OpenSettingsActionDto(requestId)
            else -> null
        }
}
