package com.example.mindos.features.social.presentation.profile.chroma

import androidx.compose.ui.geometry.Offset

/**
 * Motor Cinemático de Chroma Core.
 * Transforma los gestos 2D en navegación por volumen 3D utilizando Oklch.
 */
object ChromaKinematics {

    // Threshold details para la fricción biométrica
    private const val FAST_VELOCITY_THRESHOLD = 800f // px/s
    private const val SLOW_VELOCITY_THRESHOLD = 150f  // px/s

    /**
     * @param currentColor Color Oklch en mutación continua
     * @param dragDelta Offset delta del frame actual (px)
     * @param velocityY Vector y de velocidad estimada (px/s)
     * @param canvasWidth Anchura del canvas para escalado relativo
     * @param canvasHeight Altura del canvas para escalado relativo
     */
    fun processKinematics(
        currentColor: Oklch,
        dragDelta: Offset,
        velocityY: Float, 
        canvasWidth: Float,
        canvasHeight: Float
    ): Oklch {
        // X = Hue (0..360) (Paneo Horizontal Cíclico)
        // Sensibilidad: cuan rápido rota el espectro respecto al ancho
        val hueSensitivity = 360f / (canvasWidth * 1.5f)
        var newH = (currentColor.h + dragDelta.x * hueSensitivity) % 360f
        if (newH < 0f) newH += 360f

        // Motor Cinemático Vertical Y = Luz vs Saturación
        val absVelY = kotlin.math.abs(velocityY).coerceIn(0f, 4000f)
        
        // Ponderación Dinámica (Dynamic Weights)
        // Rápido = Impacta principalmente en Lightness
        // Lento = Impacta principalmente en Chroma (saturación fina)
        var lightWeight = (absVelY - SLOW_VELOCITY_THRESHOLD) / (FAST_VELOCITY_THRESHOLD - SLOW_VELOCITY_THRESHOLD)
        lightWeight = lightWeight.coerceIn(0.15f, 1.0f) // Siempre retenemos algo de control de luz
        
        var chromaWeight = 1.0f - lightWeight
        chromaWeight = chromaWeight.coerceIn(0.15f, 1.0f)

        // Lightness: Deslizar arriba (-Y) -> Más luz. Deslizar abajo (+Y) -> Más oscuro.
        val lightSensitivity = 2.0f / canvasHeight 
        var newL = currentColor.l - (dragDelta.y * lightSensitivity * lightWeight)
        // Cap estético (para que no rompa la UI a puro #000000 absurdo o #FFFFFF brillante que quema la vista)
        newL = newL.coerceIn(0.10f, 0.95f) 

        // Chroma: Deslizar arriba (-Y) -> Más vivo/neón. Deslizar abajo (+Y) -> Más sobrio/pastel.
        val chromaSensitivity = 0.6f / canvasHeight
        var newC = currentColor.c - (dragDelta.y * chromaSensitivity * chromaWeight)
        // Cap estético de Chroma
        newC = newC.coerceIn(0.015f, 0.38f) 
        
        return Oklch(l = newL, c = newC, h = newH)
    }
}
