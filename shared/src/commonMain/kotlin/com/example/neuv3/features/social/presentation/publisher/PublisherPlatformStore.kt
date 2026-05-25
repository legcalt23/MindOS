package com.example.neuv3.features.social.presentation.publisher

import androidx.compose.runtime.Composable

@Composable
expect fun rememberPublisherLocalDraftStore(namespace: String): PublisherLocalDraftStore
