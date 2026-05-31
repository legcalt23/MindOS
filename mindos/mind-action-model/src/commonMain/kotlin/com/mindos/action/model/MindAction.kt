package com.mindos.action.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A typed, **pre-resolution** executable action — the boundary between understanding (ZIP02) and
 * execution (ZIP03). It carries raw, human-facing references (e.g. a username), NOT resolved ids.
 * The resolver turns these into a [com.mindos] navigation command with canonical ids.
 *
 * Sealed and serializable: invalid/unknown actions cannot exist; new actions extend the set.
 */
@Serializable
sealed interface MindAction {
    val requestId: RequestId

    /** Stable type key for telemetry/audit (PII-free). */
    val actionType: String

    /** References this action needs resolved before it can become a navigation command. */
    fun references(): List<ActionReference>

    @Serializable @SerialName("open_profile")
    data class OpenProfile(override val requestId: RequestId, val userRef: String) : MindAction {
        override val actionType: String get() = TYPE
        override fun references(): List<ActionReference> =
            listOf(ActionReference(ReferenceKind.USERNAME_TO_USER_ID, userRef))

        companion object { const val TYPE = "open_profile" }
    }

    @Serializable @SerialName("open_story")
    data class OpenStory(override val requestId: RequestId, val storyRef: String) : MindAction {
        override val actionType: String get() = TYPE
        override fun references(): List<ActionReference> =
            listOf(ActionReference(ReferenceKind.STORY_SLUG_TO_STORY_ID, storyRef))

        companion object { const val TYPE = "open_story" }
    }

    @Serializable @SerialName("open_chat")
    data class OpenChat(override val requestId: RequestId, val chatRef: String) : MindAction {
        override val actionType: String get() = TYPE
        override fun references(): List<ActionReference> =
            listOf(ActionReference(ReferenceKind.CHAT_ALIAS_TO_CHAT_ID, chatRef))

        companion object { const val TYPE = "open_chat" }
    }

    @Serializable @SerialName("open_messages")
    data class OpenMessages(override val requestId: RequestId) : MindAction {
        override val actionType: String get() = TYPE
        override fun references(): List<ActionReference> = emptyList()

        companion object { const val TYPE = "open_messages" }
    }

    @Serializable @SerialName("open_settings")
    data class OpenSettings(override val requestId: RequestId) : MindAction {
        override val actionType: String get() = TYPE
        override fun references(): List<ActionReference> = emptyList()

        companion object { const val TYPE = "open_settings" }
    }
}
