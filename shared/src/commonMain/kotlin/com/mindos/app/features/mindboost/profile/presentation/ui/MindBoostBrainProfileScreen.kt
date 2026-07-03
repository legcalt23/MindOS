@file:Suppress("FunctionNaming", "MagicNumber", "LongMethod") // Compose screen: PascalCase + layout literals.

package com.mindos.app.features.mindboost.profile.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.design.AppSpacing
import com.mindos.app.design.LocalFavoriteAccent
import com.mindos.app.design.components.AppBarsLoader
import com.mindos.app.design.favoriteAccentColor
import com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus
import com.mindos.app.features.mindboost.profile.domain.model.ProfileTrend
import com.mindos.app.features.mindboost.profile.presentation.state.BrainProfileUiModel
import com.mindos.app.features.mindboost.profile.presentation.state.DimensionUi
import com.mindos.app.features.mindboost.profile.presentation.state.OverallUi
import com.mindos.app.features.mindboost.profile.presentation.state.RecommendationUi
import com.mindos.app.features.mindboost.profile.presentation.ui.components.BrainProfilePolygon

/**
 * The cognitive "brain profile" screen (Fase 2A, swipe-left counterpart to the app card). Renders the
 * [BrainProfileUiModel]: an overall summary, the dimension radar, a training suggestion, the per-dimension
 * breakdown (CALIBRATING shows "—", never 0), and the non-clinical disclaimer. No animations by default.
 *
 * Stateless: navigation/refresh are callbacks; the state holder owns loading. Wiring into Home is Ola 5.
 */
@Composable
fun MindBoostBrainProfileScreen(
    model: BrainProfileUiModel?,
    isLoading: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(AppColors.ProfileCanvas)) {
        if (model == null) {
            LoadingState()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = AppSpacing.ScreenHorizontal, vertical = AppSpacing.L),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.SectionGap),
            ) {
                Header(title = model.title, subtitle = model.subtitle, onBack = onBack)
                OverallCard(model.overall)
                BrainProfilePolygon(model.dimensions, Modifier.align(Alignment.CenterHorizontally))
                RecommendationCard(model.recommendation, isLoading = isLoading, onRefresh = onRefresh)
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.S)) {
                    model.dimensions.forEach { DimensionRow(it) }
                }
                Text(model.disclaimer, color = AppColors.TextMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppBarsLoader(color = favoriteAccentColor(LocalFavoriteAccent.current, fallback = AppColors.FocusSignal))
        Spacer(Modifier.size(AppSpacing.M))
        Text("Construyendo tu perfil…", color = AppColors.Mist, fontSize = 14.sp)
    }
}

@Composable
private fun Header(title: String, subtitle: String, onBack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.XS)) {
        Text(
            text = "‹ Volver",
            color = AppColors.Mist,
            fontSize = 14.sp,
            modifier = Modifier.clickable(onClick = onBack).padding(vertical = AppSpacing.XS),
        )
        Text(title, color = AppColors.ProfileTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = AppColors.Mist, fontSize = 13.sp)
    }
}

@Composable
private fun OverallCard(overall: OverallUi) {
    Card {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.L),
        ) {
            Text(
                text = overall.indexLabel,
                color = if (overall.hasIndex) AppColors.FocusSignal else AppColors.TextMuted,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
            )
            Column {
                Text(
                    text = if (overall.hasIndex) "Índice global (preliminar)" else "Calibrando",
                    color = AppColors.ProfileTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(overall.caption, color = AppColors.Mist, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun RecommendationCard(recommendation: RecommendationUi, isLoading: Boolean, onRefresh: () -> Unit) {
    Card {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.S)) {
            Text("Sugerencia", color = AppColors.FocusReadout, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(recommendation.text, color = AppColors.ProfileTextPrimary, fontSize = 14.sp)
            Text(
                text = if (isLoading) "Actualizando…" else "Actualizar",
                color = if (isLoading) AppColors.TextMuted else AppColors.FocusSignal,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable(enabled = !isLoading, onClick = onRefresh)
                    .padding(vertical = AppSpacing.XS),
            )
        }
    }
}

@Composable
private fun DimensionRow(dimension: DimensionUi) {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.XXS),
            ) {
                Text(
                    text = dimension.label,
                    color = AppColors.ProfileTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.S),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusChip(dimension.status, dimension.statusLabel)
                    if (dimension.preliminary && dimension.status != DimensionStatus.CALIBRATING) {
                        Text("preliminar", color = AppColors.TextMuted, fontSize = 11.sp)
                    }
                }
                // While calibrating: show progress toward the first reading instead of a bare "—".
                if (dimension.calibrationLabel.isNotEmpty()) {
                    Text(dimension.calibrationLabel, color = AppColors.TextMuted, fontSize = 11.sp)
                    val progress = dimension.calibrationProgress ?: 0
                    if (progress > 0) {
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.XXS),
                            color = AppColors.FocusSignal,
                            trackColor = AppColors.Stroke,
                        )
                    }
                }
            }
            if (dimension.trendSymbol.isNotEmpty()) {
                Text(
                    text = dimension.trendSymbol,
                    color = trendColor(dimension.trend),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(end = AppSpacing.S),
                )
            }
            Text(
                text = dimension.displayValue,
                color = if (dimension.score0to100 == null) AppColors.TextMuted else AppColors.ProfileTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun StatusChip(status: DimensionStatus, label: String) {
    val color = statusColor(status)
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = AppSpacing.S, vertical = AppSpacing.XXS),
    ) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface, RoundedCornerShape(16.dp))
            .padding(AppSpacing.CardPadding),
    ) {
        content()
    }
}

private fun statusColor(status: DimensionStatus): Color = when (status) {
    DimensionStatus.ESTABLISHED -> AppColors.FocusSignal
    DimensionStatus.EMERGING -> AppColors.NeuViolet
    DimensionStatus.CALIBRATING -> AppColors.TextMuted
}

private fun trendColor(trend: ProfileTrend): Color = when (trend) {
    ProfileTrend.IMPROVING -> AppColors.FocusHit
    ProfileTrend.DECLINING -> AppColors.FocusError
    ProfileTrend.STABLE, ProfileTrend.UNKNOWN -> AppColors.TextMuted
}
