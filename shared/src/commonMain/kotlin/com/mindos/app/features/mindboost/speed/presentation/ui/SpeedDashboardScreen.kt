@file:Suppress("FunctionNaming", "MagicNumber")

package com.mindos.app.features.mindboost.speed.presentation.ui

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.core.navigation.PlatformBackHandler
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.speed.domain.score.CognitiveProfileScore
import com.mindos.app.features.mindboost.speed.presentation.SpeedDashboardRow
import com.mindos.app.features.mindboost.speed.presentation.SpeedStrings
import com.mindos.app.features.mindboost.speed.presentation.toDashboardModel

/**
 * Dashboard v1 (MVP-3). Muestra las 3 barras de perfil + el patrón descriptivo de la corrida.
 *
 * **No autoritativo por diseño:** un aviso PRELIMINAR/no-validado/no-normativo encabeza la
 * pantalla SIEMPRE (mientras el gate físico §3/§15 esté abierto). El lenguaje es funcional, no
 * normativo, y describe el patrón de ESTA corrida, no a la persona. No hay percentiles de
 * población (no hay norma validada).
 */
@Composable
fun SpeedDashboardScreen(
    score: CognitiveProfileScore,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val model = score.toDashboardModel()
    PlatformBackHandler(enabled = true, onBack = onBack)
    Column(
        modifier = modifier.fillMaxSize().background(AppColors.FocusCanvas).statusBarsPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            SpeedStrings.DASHBOARD_TITLE,
            color = AppColors.FocusSignal,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )

        if (model.preliminary) DisclaimerBanner()

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(SpeedStrings.DASHBOARD_PATTERN, color = AppColors.FocusReadout, fontSize = 13.sp)
            Text(model.patternLabel, color = AppColors.FocusSignal, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(4.dp))
        model.rows.forEach { ProfileBar(it) }

        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(SpeedStrings.DASHBOARD_TOTAL, color = AppColors.FocusReadout, fontSize = 13.sp)
            Text(model.totalDisplay, color = AppColors.FocusSignal, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        model.ceilingNote?.let {
            Text(it, color = AppColors.FocusMiss, fontSize = 12.sp)
        }

        Spacer(Modifier.height(12.dp))
        Text(
            SpeedStrings.BACK,
            color = AppColors.FocusReadout,
            fontSize = 14.sp,
            modifier = Modifier.clickable { onBack() }.padding(8.dp),
        )
    }
}

@Composable
private fun DisclaimerBanner() {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .border(1.dp, AppColors.FocusMiss, RoundedCornerShape(12.dp))
            .background(AppColors.FocusMiss.copy(alpha = 0.10f))
            .padding(12.dp),
    ) {
        Text(SpeedStrings.DASHBOARD_DISCLAIMER, color = AppColors.FocusMiss, fontSize = 12.sp)
    }
}

@Composable
private fun ProfileBar(row: SpeedDashboardRow) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(row.label, color = AppColors.FocusReadout, fontSize = 13.sp)
            Text(row.displayValue, color = AppColors.FocusSignal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(
            Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                .background(AppColors.FocusReadout.copy(alpha = 0.2f)),
        ) {
            val fraction = row.value?.coerceIn(0.0, 1.0)?.toFloat()
            if (fraction != null) {
                Box(
                    Modifier.fillMaxWidth(fraction).height(8.dp).clip(RoundedCornerShape(4.dp))
                        .background(AppColors.FocusSignal),
                )
            }
        }
    }
}
