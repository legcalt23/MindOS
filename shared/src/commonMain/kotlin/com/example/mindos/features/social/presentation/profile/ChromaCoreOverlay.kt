package com.example.mindos.features.social.presentation.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ChromaCoreOverlay(
    session: ChromaCoreSessionState,
    onColorUpdate: (Long, ChromaCoreInteractionMode) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Stage 1: Suspend Background via Animated Visibility Overlay Layer
    AnimatedVisibility(
        visible = session.isVisible,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(200)),
        modifier = modifier.fillMaxSize()
    ) {
        val transition = updateTransition(targetState = session.isVisible, label = "ChromaCorePhaseTransition")
        
        // Phase 2: Background Dim/Blur (Isolation)
        val bgDimAlpha by transition.animateFloat(
            transitionSpec = { tween(400) },
            label = "BackgroundDimming"
        ) { visible ->
            if (visible) 0.65f else 0f
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Prevent accidental taps on the background bottom half
                .pointerInput(Unit) { }
                .background(Color.Black.copy(alpha = bgDimAlpha))
        ) {
            
            // Assembly: Rise of the panel
            AnimatedVisibility(
                visible = session.isVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
                ) + fadeIn(tween(400)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.52f) // Takes approximately the bottom 50%
            ) {
                // Phase 3 is encapsulated inside the core engine itself (Pulse / Ignition)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CHROMA CORE",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    Text(
                        text = when (session.interactionMode) {
                            ChromaCoreInteractionMode.Idle -> "Arrastra el anillo para explorar"
                            ChromaCoreInteractionMode.Dragging -> "Explorando campo tonal"
                            ChromaCoreInteractionMode.Precision -> "Modo de precisión fina activo"
                            ChromaCoreInteractionMode.Settling -> "Calculando luminancia..."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    // Core Engine Mount
                    Box(modifier = Modifier.weight(1f)) {
                        ChromaInteractiveEngine(
                            session = session,
                            onColorUpdate = onColorUpdate
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Text("Restaurar")
                        }
                        
                        Button(
                            onClick = onConfirm,
                            enabled = session.canConfirm,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(session.previewColor),
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.weight(1f).padding(start = 8.dp)
                        ) {
                            Text(
                                text = "Guardar",
                                color = if (session.canConfirm) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

