package com.mindos.command.runtime

import com.mindos.action.model.MindAction
import com.mindos.action.model.RequestId
import com.mindos.contracts.ContractSchema
import com.mindos.contracts.OpenChatActionDto
import com.mindos.contracts.OpenMessagesActionDto
import com.mindos.contracts.OpenProfileActionDto
import com.mindos.contracts.OpenSettingsActionDto
import com.mindos.contracts.OpenStoryActionDto
import com.mindos.contracts.OpenStoryOfUserActionDto
import com.mindos.contracts.RouterActionDto

sealed interface MindActionMappingResult {
    data class Mapped(val action: MindAction) : MindActionMappingResult
    data class Unsupported(val actionType: String) : MindActionMappingResult
    data class InvalidPayload(val reason: String) : MindActionMappingResult
}

fun interface RouterActionExecutionAdapter {
    fun map(action: RouterActionDto): MindActionMappingResult
}

class DefaultRouterActionExecutionAdapter : RouterActionExecutionAdapter {
    override fun map(action: RouterActionDto): MindActionMappingResult {
        if (action.schemaVersion != ContractSchema.Current) {
            return MindActionMappingResult.InvalidPayload("unsupported_schema_version")
        }

        val requestId = RequestId(action.requestId)
        val mapped = when (action) {
            is OpenProfileActionDto -> MindAction.OpenProfile(requestId, action.username)
            is OpenStoryActionDto -> MindAction.OpenStory(requestId, action.storyRef)
            is OpenStoryOfUserActionDto -> MindAction.OpenStoryOfUser(requestId, action.username)
            is OpenChatActionDto -> MindAction.OpenChat(requestId, action.chatRef)
            is OpenMessagesActionDto -> MindAction.OpenMessages(requestId)
            is OpenSettingsActionDto -> MindAction.OpenSettings(requestId)
        }
        return MindActionMappingResult.Mapped(mapped)
    }
}
