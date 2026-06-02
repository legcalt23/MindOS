package com.mindos.core

import com.mindos.contracts.ActionContractValidator
import com.mindos.contracts.ContractSchema
import com.mindos.contracts.InvalidValidationReportDto
import com.mindos.contracts.KmpCommandRouter
import com.mindos.contracts.RouterFailureResultDto
import com.mindos.contracts.RouterRequestDto
import com.mindos.contracts.RouterResultDto
import com.mindos.contracts.RouterSuccessDto
import com.mindos.contracts.UnsupportedSchemaVersionFailureDto
import com.mindos.contracts.ValidValidationReportDto
import com.mindos.contracts.ValidationFailedFailureDto
import com.mindos.contracts.ValidationIssueCode
import com.mindos.contracts.ValidationIssueDto
import com.mindos.security.CommandInputSource
import com.mindos.security.CommandInputSecurityGate
import com.mindos.security.DefaultCommandInputSecurityGate
import com.mindos.security.SecurityContext
import com.mindos.security.SecurityDecision

class CommandRouterImpl(
    private val actionRegistry: CoreActionRegistry = CoreActionRegistry.default(),
    private val matcher: PatternMatcher = PatternMatcherImpl(registry = actionRegistry.routerRegistry),
    private val extractor: EntityExtractorImpl = EntityExtractorImpl(),
    private val validator: ActionContractValidator = ActionValidatorImpl(),
    private val securityGate: CommandInputSecurityGate = DefaultCommandInputSecurityGate(),
) : KmpCommandRouter {

    override suspend fun route(request: RouterRequestDto): RouterResultDto {
        if (request.schemaVersion != ContractSchema.Current) {
            return RouterFailureResultDto(
                requestId = request.requestId,
                reason = UnsupportedSchemaVersionFailureDto(ContractSchema.Current, request.schemaVersion),
            )
        }

        when (val decision = securityGate.inspectText(request.input, request.securityContext(CommandInputSource.TEXT))) {
            SecurityDecision.Allowed -> Unit
            is SecurityDecision.Rejected -> return RouterFailureResultDto(
                request.requestId,
                RouterError.UnsafeInput(decision.codes).toFailureDto(),
            )
        }

        return when (val match = matcher.match(request)) {
            is CoreMatchResult.Matched -> {
                val action = actionRegistry.buildAction(match.actionType, request.requestId, match.entities)
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
                when (
                    val security = securityGate.inspectResolvedFields(
                        actionRegistry.observableFields(action),
                        request.securityContext(CommandInputSource.DTO),
                    )
                ) {
                    SecurityDecision.Allowed -> Unit
                    is SecurityDecision.Rejected -> return RouterFailureResultDto(
                        request.requestId,
                        RouterError.UnsafeInput(security.codes).toFailureDto(),
                    )
                }
                when (val validation = validator.validate(action)) {
                    is ValidValidationReportDto -> RouterSuccessDto(request.requestId, action)
                    is InvalidValidationReportDto -> RouterFailureResultDto(
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

    private fun RouterRequestDto.securityContext(source: CommandInputSource): SecurityContext =
        SecurityContext(requestId = requestId, source = source, localeTag = localeTag)
}
