package com.mindos.action.decompose

import com.mindos.action.model.MindAction
import com.mindos.action.model.RequestIdFactory
import com.mindos.security.CommandInputSource
import com.mindos.security.CommandInputSecurityGate
import com.mindos.security.DefaultCommandInputSecurityGate
import com.mindos.security.SecurityContext
import com.mindos.security.SecurityDecision

/**
 * Converts external/internal links and notification payloads into typed [MindAction]s. Every link
 * passes through the same security gate used by text and DTO/API inputs before any action reaches
 * validation, resolution, or navigation.
 */
class DeepLinkBridge(
    private val requestIds: RequestIdFactory,
    private val securityGate: CommandInputSecurityGate = DefaultCommandInputSecurityGate(),
) {

    fun resolve(link: String): MindAction? {
        when (securityGate.inspectDeepLink(link, SecurityContext(source = CommandInputSource.DEEP_LINK))) {
            SecurityDecision.Allowed -> Unit
            is SecurityDecision.Rejected -> return null
        }

        val path = extractPath(link) ?: return null
        val parts = path.split('/').filter { it.isNotEmpty() }
        if (parts.isEmpty()) return null
        val action = when (parts[0]) {
            "profile" -> parts.getOrNull(1)?.let { MindAction.OpenProfile(requestIds.next(), it) }
            "story" -> parts.getOrNull(1)?.let { MindAction.OpenStory(requestIds.next(), it) }
            "chat" -> parts.getOrNull(1)?.let { MindAction.OpenChat(requestIds.next(), it) }
            "messages", "notifications" -> MindAction.OpenMessages(requestIds.next())
            "settings" -> MindAction.OpenSettings(requestIds.next())
            else -> null
        }
        return action?.takeIf { isActionSafe(it) }
    }

    private fun isActionSafe(action: MindAction): Boolean {
        val fields = action.references().associate { it.kind.id to it.rawValue }
        return securityGate.inspectResolvedFields(
            fields,
            SecurityContext(requestId = action.requestId.value, source = CommandInputSource.DEEP_LINK),
        ) is SecurityDecision.Allowed
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
