package com.example.neuv3.features.privacy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
actual fun rememberPrivacyEnabled(): MutableState<Boolean> {
    return remember { mutableStateOf(false) } // In-memory fallback
}
