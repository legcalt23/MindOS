package com.example.mindos.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.mindos.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "MindOS",
    ) {
        App()
    }
}
