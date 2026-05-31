package com.mindos.commandrouter.resolver

import com.mindos.commandrouter.actions.ActionTypes
import com.mindos.commandrouter.actions.NavigateAction
import com.mindos.commandrouter.patterns.PatternMatch

class NavigationIntentResolver(
    private val factory: ActionFactory,
    override val id: ResolverId = ResolverId("navigation-intent-resolver")
) : CommandResolver {

    override suspend fun resolve(match: PatternMatch, context: ResolutionContext): ResolutionResult {
        val targetId = match.extractedParameters["target"]
            ?: return ResolutionResult.Failed(
                ResolutionFailure.InvalidParameters(
                    patternId = match.patternId.value,
                    reason = "Missing required parameter: target"
                )
            )

        val remainingParams = match.extractedParameters - "target"

        val action = NavigateAction(
            actionId = factory.nextId(),
            metadata = factory.metadata(
                type = ActionTypes.Navigate,
                source = context.source,
                correlationId = context.correlationId
            ),
            targetId = targetId,
            parameters = remainingParams
        )
        return ResolutionResult.Resolved(action, id)
    }
}
