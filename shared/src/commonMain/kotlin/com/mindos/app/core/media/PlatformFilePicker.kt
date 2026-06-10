package com.mindos.app.core.media

import androidx.compose.runtime.Composable

interface PlatformFilePicker {
    fun pick(kind: FileKind)
}

@Composable
expect fun rememberPlatformFilePicker(
    onPicked: (PlatformFile) -> Unit,
    onError: (String) -> Unit
): PlatformFilePicker
