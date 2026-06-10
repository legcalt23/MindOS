package com.mindos.app.features.social.data.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mindos.app.features.social.domain.feed.GlobalFeedRepository
import com.mindos.app.features.social.domain.feed.InMemoryGlobalFeedRepository

@Composable
actual fun rememberGlobalFeedRepository(): GlobalFeedRepository =
    remember { InMemoryGlobalFeedRepository() }
