package com.mindos.command.runtime

import com.mindos.action.model.MindAction
import com.mindos.contracts.OpenChatActionDto
import com.mindos.contracts.OpenMessagesActionDto
import com.mindos.contracts.OpenProfileActionDto
import com.mindos.contracts.OpenSettingsActionDto
import com.mindos.contracts.OpenStoryActionDto
import com.mindos.contracts.OpenStoryOfUserActionDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RouterActionExecutionAdapterTest {
    private val adapter = DefaultRouterActionExecutionAdapter()

    @Test
    fun mapsEveryCurrentPublicActionDto() {
        val actions = listOf(
            OpenProfileActionDto("req", "ana") to MindAction.OpenProfile::class,
            OpenStoryActionDto("req", "story") to MindAction.OpenStory::class,
            OpenStoryOfUserActionDto("req", "ana") to MindAction.OpenStoryOfUser::class,
            OpenChatActionDto("req", "team") to MindAction.OpenChat::class,
            OpenMessagesActionDto("req") to MindAction.OpenMessages::class,
            OpenSettingsActionDto("req") to MindAction.OpenSettings::class,
        )

        actions.forEach { (dto, expectedClass) ->
            val mapped = assertIs<MindActionMappingResult.Mapped>(adapter.map(dto)).action
            assertEquals(expectedClass, mapped::class)
            assertEquals("req", mapped.requestId.value)
        }
    }

    @Test
    fun rejectsUnsupportedSchemaVersion() {
        val result = adapter.map(OpenSettingsActionDto("req", schemaVersion = 999))

        assertEquals(
            "unsupported_schema_version",
            assertIs<MindActionMappingResult.InvalidPayload>(result).reason,
        )
    }
}
