package com.mindos.app.features.social.presentation.media

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ViewerShell(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    fileName: String,
    info: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(400)) + scaleIn(tween(500, delayMillis = 100), initialScale = 0.92f),
        exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.95f),
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            ViewerAtmosphere(modifier = Modifier.fillMaxSize())

            // Content Layer
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                content()
            }

            // Overlay Layer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                ViewerCloseButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )

                ViewerFileInfo(
                    fileName = fileName,
                    info = info,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun ViewerAtmosphere(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(ViewerDesign.Black)
        
        // Dynamic Radial Gradient for "Premium" feel
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    ViewerDesign.Green.copy(alpha = 0.12f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.15f, size.height * 0.2f),
                radius = size.minDimension * 0.8f
            )
        )
        
        // Subtle Noise/Grain or Atmosphere Lines
        repeat(5) { index ->
            val y = size.height * (0.2f + index * 0.12f)
            drawLine(
                color = ViewerDesign.Green.copy(alpha = 0.03f),
                start = Offset(0f, y),
                end = Offset(size.width, y - 40f),
                strokeWidth = 1f
            )
        }
    }
}
