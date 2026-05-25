package com.example.neuv3.core.media

import androidx.compose.runtime.Composable

interface PlatformFilePicker {
    fun pick(kind: FileKind)
}

@Composable
expect fun rememberPlatformFilePicker(
    onPicked: (PlatformFile) -> Unit,
    onError: (String) -> Unit
): PlatformFilePicker
