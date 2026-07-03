package com.mindos.command.runtime

import com.mindos.action.model.MindAction
import com.mindos.contracts.RouterAbuseContextDto

enum class CommandRisk {
    LOW_RISK,
    SENSITIVE,
    UNCLASSIFIED,
}

data class CommandSafetyContext(
    val requestId: String,
    val sessionId: String?,
    val localeTag: String,
    val source: CommandSource,
    val abuseContext: RouterAbuseContextDto = RouterAbuseContextDto(),
)

sealed interface CommandSafetyDecision {
    data object Allow : CommandSafetyDecision
    data class RequireConfirmation(val prompt: String, val ttlMs: Long) : CommandSafetyDecision
    data class Deny(val reason: String) : CommandSafetyDecision
}

fun interface CommandSafetyPolicy {
    fun evaluate(action: MindAction, context: CommandSafetyContext): CommandSafetyDecision
}

class DefaultCommandSafetyPolicy(
    private val lowRiskActionTypes: Set<String> = CurrentLowRiskActionTypes,
    private val sensitiveActionTypes: Set<String> = emptySet(),
    private val confirmationTtlMs: Long = 30_000L,
) : CommandSafetyPolicy {
    init {
        require(confirmationTtlMs > 0) { "confirmationTtlMs must be positive." }
        require(lowRiskActionTypes.intersect(sensitiveActionTypes).isEmpty()) {
            "An action type cannot be both low-risk and sensitive."
        }
    }

    override fun evaluate(action: MindAction, context: CommandSafetyContext): CommandSafetyDecision =
        when (classify(action.actionType)) {
            CommandRisk.LOW_RISK -> CommandSafetyDecision.Allow
            CommandRisk.SENSITIVE -> CommandSafetyDecision.RequireConfirmation(
                prompt = if (context.localeTag.lowercase().startsWith("es")) {
                    "Confirma que quieres realizar esta accion."
                } else {
                    "Confirm that you want to perform this action."
                },
                ttlMs = confirmationTtlMs,
            )
            CommandRisk.UNCLASSIFIED -> CommandSafetyDecision.Deny("unclassified_action")
        }

    private fun classify(actionType: String): CommandRisk = when (actionType) {
        in sensitiveActionTypes -> CommandRisk.SENSITIVE
        in lowRiskActionTypes -> CommandRisk.LOW_RISK
        else -> CommandRisk.UNCLASSIFIED
    }

    companion object {
        val CurrentLowRiskActionTypes = setOf(
            MindAction.OpenProfile.TYPE,
            MindAction.OpenStory.TYPE,
            MindAction.OpenStoryOfUser.TYPE,
            MindAction.OpenChat.TYPE,
            MindAction.OpenMessages.TYPE,
            MindAction.OpenSettings.TYPE,
        )
    }
}
