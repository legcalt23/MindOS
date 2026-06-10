package com.mindos.app.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.mindos.app.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "MindOS",
    ) {
        App()
    }
}
