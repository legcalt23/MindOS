package com.mindos.app.features.social.data.feed

import androidx.compose.runtime.Composable
import com.mindos.app.features.social.domain.feed.GlobalFeedRepository

@Composable
expect fun rememberGlobalFeedRepository(): GlobalFeedRepository
