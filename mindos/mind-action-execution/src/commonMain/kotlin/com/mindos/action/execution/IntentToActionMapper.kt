package com.mindos.action.execution

import com.mindos.action.entity.ExtractedEntity
import com.mindos.action.model.MindAction
import com.mindos.action.model.RequestId
import com.mindos.action.model.RequestIdFactory
import com.mindos.action.understanding.MindIntent
import com.mindos.action.understanding.SlotName

/**
 * Builds a [MindAction] for one intent id from its extracted entities. Returning null means the
 * intent is recognized but its required entities are absent (the caller treats it as a no-op).
 */
fun interface IntentActionBuilder {
    fun build(requestId: RequestId, entities: Map<SlotName, ExtractedEntity>): MindAction?
}

/**
 * The production bridge between ZIP02 understanding and ZIP03 execution: maps a recognized
 * `MindIntent` (+ entities) to a typed, pre-resolution [MindAction]. Table-driven and extensible —
 * apps register builders for their own intents without forking the engine. This replaces the
 * ad-hoc bridge that previously lived only in ZIP02's end-to-end test.
 */
class IntentToActionMapper(
    private val requestIds: RequestIdFactory,
    private val builders: Map<String, IntentActionBuilder> = defaultBuilders(),
) {
    fun map(intent: MindIntent, entities: Map<SlotName, ExtractedEntity>): MindAction? =
        builders[intent.id.value]?.build(requestIds.next(), entities)

    companion object {
        /** Default builders for the five v1 navigation intents (ZIP03 NAVIGATION contracts). */
        fun defaultBuilders(): Map<String, IntentActionBuilder> = mapOf(
            MindAction.OpenProfile.TYPE to IntentActionBuilder { id, e ->
                e[SlotName("target")]?.let { MindAction.OpenProfile(id, it.value) }
            },
            MindAction.OpenStory.TYPE to IntentActionBuilder { id, e ->
                e[SlotName("storyId")]?.let { MindAction.OpenStory(id, it.value) }
            },
            MindAction.OpenStoryOfUser.TYPE to IntentActionBuilder { id, e ->
                e[SlotName("target")]?.let { MindAction.OpenStoryOfUser(id, it.value) }
            },
            MindAction.OpenChat.TYPE to IntentActionBuilder { id, e ->
                e[SlotName("chatId")]?.let { MindAction.OpenChat(id, it.value) }
            },
            MindAction.OpenMessages.TYPE to IntentActionBuilder { id, _ -> MindAction.OpenMessages(id) },
            MindAction.OpenSettings.TYPE to IntentActionBuilder { id, _ -> MindAction.OpenSettings(id) },
        )
    }
}
