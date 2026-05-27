package com.example.mindos.features.dm.domain.model

import com.example.mindos.core.security.SafeInput

object DMConversationIds {
    private const val Prefix = "dm1"

    fun participants(user1Id: String, user2Id: String): List<String> {
        require(user1Id.isValidParticipantId()) { "Invalid first DM participant id" }
        require(user2Id.isValidParticipantId()) { "Invalid second DM participant id" }
        require(user1Id != user2Id) { "Cannot create a DM chat with yourself" }
        return listOf(user1Id, user2Id).sorted()
    }

    fun chatId(user1Id: String, user2Id: String): String {
        val participants = participants(user1Id, user2Id)
        return chatIdForSortedParticipants(participants[0], participants[1])
    }

    fun chatIdOrNull(user1Id: String, user2Id: String): String? =
        runCatching { chatId(user1Id, user2Id) }.getOrNull()

    fun chatIdForParticipantsOrNull(participants: List<String>): String? {
        val unique = participants.distinct()
        if (unique.size != 2) return null
        return chatIdOrNull(unique[0], unique[1])
    }

    fun legacyChatId(user1Id: String, user2Id: String): String {
        val participants = participants(user1Id, user2Id)
        return participants.joinToString("_")
    }

    private fun chatIdForSortedParticipants(first: String, second: String): String =
        "${Prefix}_${first.length}_${first}_${second.length}_${second}"

    private fun String.isValidParticipantId(): Boolean =
        SafeInput.isSafeDocumentId(this)
}
