package com.mindos.core

import com.mindos.contracts.OpenChatActionDto
import com.mindos.contracts.OpenMessagesActionDto
import com.mindos.contracts.OpenProfileActionDto
import com.mindos.contracts.OpenSettingsActionDto
import com.mindos.contracts.OpenStoryActionDto
import com.mindos.contracts.OpenStoryOfUserActionDto
import com.mindos.contracts.RouterActionDto

fun interface RouterActionFactory {
    fun build(requestId: String, entities: Map<String, String>): RouterActionDto?
}

data class CoreActionSpec(
    val actionType: String,
    val patterns: List<RouterPatternSpec>,
    val aliases: List<RouterAliasSpec> = emptyList(),
    val factory: RouterActionFactory,
) {
    init {
        require(actionType.isNotBlank()) { "actionType must not be blank." }
        require(patterns.isNotEmpty() || aliases.isNotEmpty()) { "action spec must declare patterns or aliases." }
    }
}

class CoreActionRegistry(
    val specs: List<CoreActionSpec>,
) {
    init {
        require(specs.isNotEmpty()) { "action registry must not be empty." }
        require(specs.map { it.actionType }.distinct().size == specs.size) { "duplicate actionType in action registry." }
    }

    val routerRegistry: CoreRouterRegistry = CoreRouterRegistry(
        patterns = specs.flatMap { it.patterns },
        aliases = specs.flatMap { it.aliases },
    )

    private val factories = specs.associate { it.actionType to it.factory }

    fun buildAction(actionType: String, requestId: String, entities: Map<String, String>): RouterActionDto? =
        factories[actionType]?.build(requestId, entities)

    fun observableFields(action: RouterActionDto): Map<String, String> =
        when (action) {
            is OpenProfileActionDto -> mapOf("username" to action.username)
            is OpenStoryActionDto -> mapOf("storyRef" to action.storyRef)
            is OpenStoryOfUserActionDto -> mapOf("username" to action.username)
            is OpenChatActionDto -> mapOf("chatRef" to action.chatRef)
            is OpenMessagesActionDto,
            is OpenSettingsActionDto,
            -> emptyMap()
        }

    companion object {
        fun default(): CoreActionRegistry = CoreActionRegistry(
            specs = listOf(
                CoreActionSpec(
                    actionType = "open_settings",
                    patterns = listOf(
                        RouterPatternSpec("p_open_settings", "open settings", "open_settings", PatternKind.EXACT),
                        RouterPatternSpec("p_settings", "settings", "open_settings", PatternKind.EXACT),
                    ),
                    aliases = listOf(
                        RouterAliasSpec("ajustes", "es", "open_settings"),
                        RouterAliasSpec("configuracion", "es", "open_settings"),
                    ),
                    factory = RouterActionFactory { requestId, _ -> OpenSettingsActionDto(requestId) },
                ),
                CoreActionSpec(
                    actionType = "open_messages",
                    patterns = listOf(
                        RouterPatternSpec("p_open_messages", "open messages", "open_messages", PatternKind.EXACT),
                        RouterPatternSpec("p_messages", "messages", "open_messages", PatternKind.EXACT),
                    ),
                    aliases = listOf(
                        RouterAliasSpec("mensajes", "es", "open_messages"),
                        RouterAliasSpec("home", "und", "open_messages"),
                    ),
                    factory = RouterActionFactory { requestId, _ -> OpenMessagesActionDto(requestId) },
                ),
                CoreActionSpec(
                    actionType = "open_profile",
                    patterns = listOf(
                        RouterPatternSpec("p_open_profile", "open {target:username}", "open_profile", PatternKind.PARAMETRIC),
                    ),
                    factory = RouterActionFactory { requestId, entities ->
                        entities["target"]?.let { OpenProfileActionDto(requestId, it) }
                    },
                ),
                CoreActionSpec(
                    actionType = "open_story",
                    patterns = listOf(
                        RouterPatternSpec("p_open_story", "open story {storyId:story_id}", "open_story", PatternKind.PARAMETRIC),
                    ),
                    factory = RouterActionFactory { requestId, entities ->
                        entities["storyId"]?.let { OpenStoryActionDto(requestId, it) }
                    },
                ),
                CoreActionSpec(
                    actionType = "open_story_of_user",
                    patterns = listOf(
                        RouterPatternSpec(
                            id = "p_open_story_of_user_es",
                            text = "abre la historia de {target:username}",
                            actionType = "open_story_of_user",
                            kind = PatternKind.PARAMETRIC,
                            priority = 20,
                        ),
                        RouterPatternSpec(
                            id = "p_open_story_of_user_en",
                            text = "open {target:username} story",
                            actionType = "open_story_of_user",
                            kind = PatternKind.PARAMETRIC,
                            priority = 20,
                        ),
                    ),
                    factory = RouterActionFactory { requestId, entities ->
                        entities["target"]?.let { OpenStoryOfUserActionDto(requestId, it) }
                    },
                ),
                CoreActionSpec(
                    actionType = "open_chat",
                    patterns = listOf(
                        RouterPatternSpec("p_open_chat", "open chat {chatId:chat_id}", "open_chat", PatternKind.PARAMETRIC),
                    ),
                    factory = RouterActionFactory { requestId, entities ->
                        entities["chatId"]?.let { OpenChatActionDto(requestId, it) }
                    },
                ),
            ),
        )
    }
}
