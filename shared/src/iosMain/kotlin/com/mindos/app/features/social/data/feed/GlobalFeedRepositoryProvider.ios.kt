package com.mindos.app.features.social.data.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mindos.app.features.social.domain.feed.DisabledGlobalFeedRepository
import com.mindos.app.features.social.domain.feed.GlobalFeedRepository

@Composable
actual fun rememberGlobalFeedRepository(): GlobalFeedRepository =
    remember {
        DisabledGlobalFeedRepository(
            reason = "iOS global feed is disabled until Firebase session and App Check are wired.",
        )
    }
