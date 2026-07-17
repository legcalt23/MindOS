package com.mindos.app

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mindos.app.core.session.FirebaseSessionRepository
import com.mindos.app.features.mindboost.profile.presentation.state.displayLabel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate() so the system splash window is
        // handed over to the library, which then dismisses it once the first
        // Compose frame is ready (postSplashScreenTheme = Theme.MindOS).
        installSplashScreen()
        super.onCreate(savedInstanceState)
        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) == 0) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        }
        com.mindos.app.features.social.presentation.media.MediaPreloadManager.initialize(applicationContext)
        enableEdgeToEdge()

        setContent {
            val sessionRepository = remember {
                if (intent.getBooleanExtra("use_preview_auth", false)) {
                    com.mindos.app.core.session.PreviewSessionRepository(isAuthenticated = true)
                } else {
                    FirebaseSessionRepository()
                }
            }

            if (intent.getBooleanExtra("use_preview_auth", false)) {
                com.mindos.app.design.MindOSTheme {
                    VisualVerificationHarness()
                }
            } else {
                MindOSApp(
                    sessionRepository = sessionRepository,
                )
            }
        }
    }
}

@Composable
private fun VisualVerificationHarness() {
    var fixtureIndex by remember { mutableStateOf(0) }
    var fontScale by remember { mutableStateOf(1.0f) }

    val names = listOf("Zero", "Uniform", "Asymmetric", "Minimum", "Maximum", "Incomplete")
    val model = remember(fixtureIndex) {
        buildMockProfile(fixtureIndex)
    }

    CompositionLocalProvider(
        LocalDensity provides Density(
            density = LocalDensity.current.density,
            fontScale = fontScale
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            com.mindos.app.features.mindboost.profile.presentation.ui.MindBoostBrainProfileScreen(
                model = model,
                isLoading = false,
                onBack = {},
                onRefresh = {},
                modifier = Modifier.fillMaxSize().padding(top = 70.dp)
            )

            // Control panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .background(Color(0xFF1E1E1E))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = {
                    fixtureIndex = (fixtureIndex - 1 + 6) % 6
                }) {
                    Text("Prev")
                }
                Text(
                    text = "${names[fixtureIndex]} (Scale: $fontScale)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Button(onClick = {
                    fixtureIndex = (fixtureIndex + 1) % 6
                }) {
                    Text("Next")
                }
                Button(onClick = {
                    fontScale = if (fontScale == 1.0f) 1.5f else 1.0f
                }) {
                    Text("Font")
                }
            }
        }
    }
}

private fun buildMockProfile(index: Int): com.mindos.app.features.mindboost.profile.presentation.state.BrainProfileUiModel {
    val dims = com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension.values().map { dim ->
        val label = dim.displayLabel()
        when (index) {
            0 -> { // Zero
                createMockDimension(dim, label, 0, com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus.ESTABLISHED)
            }
            1 -> { // Uniform
                createMockDimension(dim, label, 50, com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus.ESTABLISHED)
            }
            2 -> { // Asymmetric
                val score = when (dim) {
                    com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension.ATTENTION -> 90
                    com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension.PROCESSING_SPEED -> 70
                    com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension.WORKING_MEMORY -> 40
                    com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension.EPISODIC_MEMORY -> 45
                    com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension.REASONING -> 80
                    com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension.VERBAL -> 85
                    com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension.DECISION_MAKING -> 20
                    com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension.EMOTIONAL_REGULATION -> 15
                    com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension.CREATIVITY -> 30
                }
                val status = if (score >= 70) {
                    com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus.ESTABLISHED
                } else {
                    com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus.EMERGING
                }
                createMockDimension(dim, label, score, status)
            }
            3 -> { // Minimum
                createMockDimension(dim, label, 10, com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus.EMERGING)
            }
            4 -> { // Maximum
                createMockDimension(dim, label, 100, com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus.ESTABLISHED)
            }
            else -> { // Incomplete
                val isCalibrating = dim.ordinal % 2 == 0
                if (isCalibrating) {
                    com.mindos.app.features.mindboost.profile.presentation.state.DimensionUi(
                        dimension = dim,
                        label = label,
                        status = com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus.CALIBRATING,
                        statusLabel = "Calibrando",
                        displayValue = "—",
                        score0to100 = null,
                        trend = com.mindos.app.features.mindboost.profile.domain.model.ProfileTrend.UNKNOWN,
                        trendLabel = "",
                        trendSymbol = "",
                        preliminary = true,
                        calibrationProgress = 40,
                        calibrationLabel = "Reuniendo datos · 40% …",
                        combineWeight = 1.0
                    )
                } else {
                    createMockDimension(dim, label, 60, com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus.ESTABLISHED)
                }
            }
        }
    }

    return com.mindos.app.features.mindboost.profile.presentation.state.BrainProfileUiModel(
        title = "Perfil Cognitivo",
        subtitle = "Visualización de prueba de rendimiento",
        overall = com.mindos.app.features.mindboost.profile.presentation.state.OverallUi(
            indexLabel = if (index == 5) "Calibrando" else "Índice General",
            hasIndex = index != 5,
            caption = "Fixture de visualización index=$index"
        ),
        dimensions = dims,
        recommendation = com.mindos.app.features.mindboost.profile.presentation.state.RecommendationUi(
            reason = com.mindos.app.features.mindboost.profile.domain.orchestration.RecommendationReason.DEVELOP_AREA,
            text = "Recomendamos seguir entrenando memoria y velocidad para optimizar el perfil.",
            moduleLabel = "Velocidad mental"
        ),
        disclaimer = "Prueba de renderizado interna. Sin fines médicos ni diagnósticos."
    )
}

private fun createMockDimension(
    dim: com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension,
    label: String,
    score: Int,
    status: com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus
): com.mindos.app.features.mindboost.profile.presentation.state.DimensionUi {
    return com.mindos.app.features.mindboost.profile.presentation.state.DimensionUi(
        dimension = dim,
        label = label,
        status = status,
        statusLabel = if (status == com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus.ESTABLISHED) "Establecido" else "Emergente",
        displayValue = score.toString(),
        score0to100 = score,
        trend = com.mindos.app.features.mindboost.profile.domain.model.ProfileTrend.STABLE,
        trendLabel = "Estable",
        trendSymbol = "→",
        preliminary = false,
        calibrationProgress = null,
        calibrationLabel = "",
        combineWeight = 1.0
    )
}
