package com.mindos.app.features.privacy

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberPrivacyEnabled(): MutableState<Boolean> {
    val context = LocalContext.current
    val sharedPrefs = remember(context) {
        context.getSharedPreferences("privacy_prefs", Context.MODE_PRIVATE)
    }
    val state = remember {
        mutableStateOf(sharedPrefs.getBoolean("is_privacy_enabled", false))
    }
    
    LaunchedEffect(state.value) {
        sharedPrefs.edit().putBoolean("is_privacy_enabled", state.value).apply()
    }
    
    return state
}
