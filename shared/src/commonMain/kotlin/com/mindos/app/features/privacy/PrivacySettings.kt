package com.mindos.app.features.privacy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState

@Composable
expect fun rememberPrivacyEnabled(): MutableState<Boolean>
