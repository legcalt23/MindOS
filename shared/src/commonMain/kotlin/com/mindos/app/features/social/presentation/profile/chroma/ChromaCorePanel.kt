package com.mindos.app.features.social.presentation.profile.chroma

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val ChromaAbyss = Color(0xFF030303)
val ChromaDivider = Color(0xFF151515)

/**
 * Panel interactivo para el Sistema de Color Chroma Core (Motor Cinemático).
 * Ocupa estrictamente la mitad inferior de la pantalla sin scroll ni elementos molestos.
 */
@Composable
fun ChromaCorePanel(
    initialColor: Color,
    onColorChanged: (Color) -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Aislamiento Múltiple de Estado
    var sessionColor by remember { mutableStateOf(initialColor) }
    var currentOklch by remember { mutableStateOf(Oklch.fromColor(initialColor)) }
    
    // UI tracking state (Interactua con DrawScope sin redibujar layouts masivos)
    var isInteracting by remember { mutableStateOf(false) }
    var touchPosition by remember { mutableStateOf<Offset?>(null) }
    var currentVelocityY by remember { mutableStateOf(0f) }

    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f) // Ocupa estrictamente el bottom half real
            .background(ChromaAbyss)
    ) {
        // 1. TACTICAL HEADER (Área de Acciones Clara y Rígida)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(ChromaAbyss)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    onColorChanged(initialColor)
                    onCancel()
                },
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(Icons.Default.Close, contentDescription = "Ignorar", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("IGNORAR", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            
            TextButton(
                onClick = {
                    onApply()
                    // Feedback físico asertivo al guardar
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("APLICAR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.Check, contentDescription = "Aplicar", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
        
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(ChromaDivider))

        // 2. THE KINEMATIC MATRIX (Gesto Único)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(ChromaAbyss)
                .clipToBounds()
        ) {
            val canvasW = constraints.maxWidth.toFloat()
            val canvasH = constraints.maxHeight.toFloat()
            val velocityTracker = remember { VelocityTracker() }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(initialColor) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                isInteracting = true
                                touchPosition = offset
                                velocityTracker.resetTracking()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragEnd = {
                                isInteracting = false
                                touchPosition = null
                            },
                            onDragCancel = {
                                isInteracting = false
                                touchPosition = null
                            }
                        ) { change, dragAmount ->
                            // El Consume total bloquea Swipe To Dismiss o Scrollers OS nativos
                            change.consume() 
                            
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            val velocity = velocityTracker.calculateVelocity()
                            currentVelocityY = velocity.y
                            touchPosition = change.position

                            val nextOklch = ChromaKinematics.processKinematics(
                                currentColor = currentOklch,
                                dragDelta = dragAmount,
                                velocityY = currentVelocityY,
                                canvasWidth = canvasW,
                                canvasHeight = canvasH
                            )
                            currentOklch = nextOklch
                            
                            val newSrgb = nextOklch.toColor()
                            sessionColor = newSrgb
                            onColorChanged(newSrgb) // Solo emite hacia el preview superior, Canvas local dibuja desde Color state in-place 
                        }
                    }
            ) {
                // Fase de Frame Nativos: Se actualiza a 60/120fps sin invocar recomposiciones (Jank free)
                val colorValue = sessionColor 
                
                touchPosition?.let { pos ->
                    // Feedback Visual del Motor Cinemático:
                    // Si corre rápido -> Efecto Vertical Estirado (Luz)
                    // Si corre lento -> Efecto Sólido Concentrado (Saturación Fina)
                    val absVel = kotlin.math.abs(currentVelocityY)
                    val stretchFactor = (absVel / 1500f).coerceIn(0f, 1f)
                    
                    val radiusX = 180f - (stretchFactor * 40f)
                    val radiusY = 180f + (stretchFactor * 80f)
                    val coreIntensity = 1f - (stretchFactor * 0.3f) // Se vuelve más etéreo si se mueve rápido
                    
                    drawContext.canvas.save()
                    drawContext.transform.translate(pos.x, pos.y)
                    drawContext.transform.scale(radiusX/180f, radiusY/180f)
                    
                    // Aura Plasma
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colorValue.copy(alpha = 1f),
                                colorValue.copy(alpha = 0.7f * coreIntensity),
                                Color.Transparent
                            ),
                            center = Offset.Zero,
                            radius = 180f
                        ),
                        radius = 180f,
                        center = Offset.Zero
                    )
                    
                    // Núcleo de Alta Precisión
                    drawCircle(
                        color = Color.White.copy(alpha = 0.6f * coreIntensity),
                        radius = 45f - (stretchFactor * 20f),
                        center = Offset.Zero
                    )
                    
                    drawContext.canvas.restore()
                }
            }
        }
    }
}
