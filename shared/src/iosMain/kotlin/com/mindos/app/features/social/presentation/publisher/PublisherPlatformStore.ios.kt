package com.mindos.app.features.social.presentation.publisher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberPublisherLocalDraftStore(namespace: String): PublisherLocalDraftStore =
    remember(namespace) {
        InMemoryPublisherLocalDraftStore()
    }
