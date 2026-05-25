package com.example.neuv3.features.social.data.feed

import androidx.compose.runtime.Composable
import com.example.neuv3.features.social.domain.feed.GlobalFeedRepository

@Composable
expect fun rememberGlobalFeedRepository(): GlobalFeedRepository
