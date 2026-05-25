package com.example.neuv3.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.neuv3.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "NEUv3",
    ) {
        App()
    }
}
