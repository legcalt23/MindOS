package com.example.mindos.features.social.presentation.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.mindos.features.social.domain.feed.GlobalFeedDraftAttachment

@Composable
actual fun rememberFeedAttachmentPicker(
    onPicked: (List<GlobalFeedDraftAttachment>) -> Unit,
    onError: (String) -> Unit,
): FeedAttachmentPicker =
    remember(onError) {
        object : FeedAttachmentPicker {
            override fun pick(kind: FeedAttachmentPickKind) {
                onError("La seleccion de adjuntos esta disponible en Android.")
            }
        }
    }
