package com.example.mindos.core.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberPlatformFilePicker(
    onPicked: (PlatformFile) -> Unit,
    onError: (String) -> Unit,
): PlatformFilePicker = remember(onError) {
    object : PlatformFilePicker {
        override fun pick(kind: FileKind) {
            onError("La seleccion de archivos no esta disponible en escritorio.")
        }
    }
}
