package com.example.mindos.features.social.data.feed

import androidx.compose.runtime.Composable
import com.example.mindos.features.social.domain.feed.GlobalFeedRepository

@Composable
expect fun rememberGlobalFeedRepository(): GlobalFeedRepository
