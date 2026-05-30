package com.example.mindos.features.social.presentation.publisher

import androidx.compose.runtime.Composable

@Composable
expect fun rememberPublisherLocalDraftStore(namespace: String): PublisherLocalDraftStore
