package com.mindos.action.decompose

import com.mindos.action.model.MindAction
import com.mindos.action.model.RequestIdFactory

/**
 * Converts external/internal links and notification payloads into typed [MindAction]s (ZIP10
 * DEEPLINKS). Deterministic and UI-free. The produced action flows through the normal validate →
 * resolve → navigate pipeline, so deep links get the same safety as typed commands.
 *
 * Supported forms (path after scheme/host):
 *   profile/{user} · story/{id} · chat/{id} · messages | notifications · settings
 * Schemes: `mindos://…`, `https://mindos.app/…`, `http://mindos.app/…`, or a bare `/path`.
 */
class DeepLinkBridge(private val requestIds: RequestIdFactory) {

    fun resolve(link: String): MindAction? {
        val path = extractPath(link) ?: return null
        val parts = path.split('/').filter { it.isNotEmpty() }
        if (parts.isEmpty()) return null
        return when (parts[0]) {
            "profile" -> parts.getOrNull(1)?.let { MindAction.OpenProfile(requestIds.next(), it) }
            "story" -> parts.getOrNull(1)?.let { MindAction.OpenStory(requestIds.next(), it) }
            "chat" -> parts.getOrNull(1)?.let { MindAction.OpenChat(requestIds.next(), it) }
            "messages", "notifications" -> MindAction.OpenMessages(requestIds.next())
            "settings" -> MindAction.OpenSettings(requestIds.next())
            else -> null
        }
    }

    private fun extractPath(link: String): String? {
        val trimmed = link.trim()
        val afterHost = when {
            trimmed.startsWith(SCHEME) -> trimmed.removePrefix(SCHEME)
            trimmed.startsWith(HTTPS_HOST) -> trimmed.removePrefix(HTTPS_HOST)
            trimmed.startsWith(HTTP_HOST) -> trimmed.removePrefix(HTTP_HOST)
            trimmed.startsWith("/") -> trimmed.removePrefix("/")
            else -> return null
        }
        return afterHost.substringBefore('?').substringBefore('#')
    }

    private companion object {
        const val SCHEME = "mindos://"
        const val HTTPS_HOST = "https://mindos.app/"
        const val HTTP_HOST = "http://mindos.app/"
    }
}
