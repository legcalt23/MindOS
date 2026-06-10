package com.mindos.app.features.social.presentation.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal expect fun CoverClubArtwork(
    index: Int,
    fallbackLabel: String,
    modifier: Modifier = Modifier,
)
