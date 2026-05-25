package com.example.neuv3.features.intro

import androidx.compose.ui.graphics.Color

object MindOSIntroConfig {
    const val DURATION_MS = 5000L
    const val PARTICLE_COUNT = 80
    const val MAX_CONNECTIONS = 45
    const val CONNECTION_DISTANCE = 180f
    const val BRAND_NAME = "Mind OS"

    const val CORE_RADIUS = 52f
    const val CORE_ROTATION_SPEED = 0.0004f
    const val CORE_BREATH_AMPLITUDE = 0.025f
    const val CORE_BREATH_SPEED = 0.002f

    const val TYPE_SIZE = 44f
    const val TYPE_TRACKING = 10f

    object Phase {
        val BLACK_BREATH = 0L..800L
        val CONSTELLATION = 800L..1700L
        val CONVERGENCE = 1700L..3000L
        val NAME_EMERGES = 3000L..4200L
        val TRANSITION = 4200L..5000L
    }

    object Palette {
        val DeepVoid = Color(0xFF05060A)
        val CosmicInk = Color(0xFF0A0B14)
        val Graphite = Color(0xFF13141F)
        val QuantumViolet = Color(0xFF7B6BFF)
        val AuroraLilac = Color(0xFFA78BFA)
        val SynapseBlue = Color(0xFF5B8DEF)
        val StellarWhite = Color(0xFFF5F0FF)
        val HoloSilver = Color(0xFFE6E4F0)
    }
}
