package com.example.neuv3.features.social.data.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.neuv3.features.social.domain.feed.GlobalFeedRepository
import com.example.neuv3.features.social.domain.feed.InMemoryGlobalFeedRepository

@Composable
actual fun rememberGlobalFeedRepository(): GlobalFeedRepository =
    remember { InMemoryGlobalFeedRepository() }
