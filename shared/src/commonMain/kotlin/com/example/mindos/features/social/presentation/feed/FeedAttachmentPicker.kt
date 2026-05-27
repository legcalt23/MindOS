package com.example.mindos.features.social.presentation.feed

import androidx.compose.runtime.Composable
import com.example.mindos.features.social.domain.feed.GlobalFeedDraftAttachment

enum class FeedAttachmentPickKind {
    Photos,
    Videos,
    Files,
}

interface FeedAttachmentPicker {
    fun pick(kind: FeedAttachmentPickKind)
}

@Composable
expect fun rememberFeedAttachmentPicker(
    onPicked: (List<GlobalFeedDraftAttachment>) -> Unit,
    onError: (String) -> Unit,
): FeedAttachmentPicker
