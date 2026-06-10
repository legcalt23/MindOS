@file:Suppress("FunctionNaming", "LongMethod", "MagicNumber")

package com.mindos.app.features.mindboost.focus.presentation.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.focus.domain.session.FocusReport
import com.mindos.app.features.mindboost.focus.domain.session.ReportLine
import com.mindos.app.features.mindboost.focus.domain.session.SessionTemplate
import com.mindos.app.features.mindboost.focus.presentation.ui.components.FocusAtmosphere

/**
 * Honest post-session summary. Every headline carries its caption and, when the
 * sample is too small, is visibly de-emphasized. The honest note never promises
 * far transfer. Cards reveal with a gentle staggered fade.
 */
@Composable
fun FocusSummaryScreen(
    report: FocusReport,
    template: SessionTemplate,
    onDone: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }
    val scroll = rememberScrollState()

    Box(modifier = modifier.fillMaxSize().background(AppColors.FocusCanvas)) {
        FocusAtmosphere(modifier = Modifier.fillMaxSize(), intensity = 0.7f)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(72.dp))
            Text(
                text = "Sesión completada",
                color = AppColors.FocusSignal,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${template.totalMinutes} minutos de ${template.displayName.lowercase()}",
                color = AppColors.ProfileTextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.height(26.dp))
            report.headline.forEachIndexed { index, line ->
                StaggeredReveal(revealed = revealed, index = index) {
                    MetricCard(line = line)
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(10.dp))
            HonestNote(text = report.honestNote)

            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(text = "Otra sesión", onClick = onRepeat, modifier = Modifier.weight(1f))
                PrimaryButton(text = "Hecho", onClick = onDone, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun StaggeredReveal(revealed: Boolean, index: Int, content: @Composable () -> Unit) {
    val progress by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMillis = 480, delayMillis = index * 90),
        label = "reveal",
    )
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 28f
        },
    ) {
        content()
    }
}

@Composable
private fun MetricCard(line: ReportLine) {
    val confident = line.metric.confident
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = if (confident) 0.05f else 0.025f))
            .border(
                1.dp,
                if (confident) AppColors.Stroke else AppColors.Stroke.copy(alpha = 0.5f),
                RoundedCornerShape(20.dp),
            )
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = line.label,
                color = AppColors.FocusReadout,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (!confident) {
                Spacer(Modifier.height(2.dp))
                Text(text = "incertidumbre alta", color = AppColors.FocusMiss.copy(alpha = 0.8f), fontSize = 10.sp)
            }
        }
        Text(
            text = line.caption,
            color = if (confident) AppColors.ProfileTextPrimary else AppColors.ProfileTextPrimary.copy(alpha = 0.45f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun HonestNote(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AppColors.FocusAuroraB.copy(alpha = 0.22f))
            .border(1.dp, AppColors.FocusReadout.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
            .padding(18.dp),
    ) {
        Text(
            text = text,
            color = AppColors.ProfileTextPrimary.copy(alpha = 0.82f),
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(AppColors.FocusSignal, AppColors.NeuCyan)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = AppColors.OnBright, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, AppColors.Stroke, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = AppColors.ProfileTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}
