@file:Suppress("MagicNumber", "LongMethod", "FunctionNaming")

package com.mindos.app.uivariantes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.design.AppSpacing
import com.mindos.app.design.AppShapes

private val GlassWhite = Color.White.copy(alpha = 0.18f)
private val GlassStroke = Color.White.copy(alpha = 0.42f)
private val MutedWhite = Color.White.copy(alpha = 0.76f)
private val NightNavy = AppColors.DialogBackground

private data class TrainingAbility(
    val name: String,
    val icon: ImageVector,
    val accent: Color,
)

private val trainingAbilities = listOf(
    TrainingAbility("Enfoque", Icons.Filled.CenterFocusStrong, AppColors.ComposerBlue),
    TrainingAbility("Decisiones", Icons.Filled.Explore, AppColors.NeuViolet),
    TrainingAbility("Lógica", Icons.Filled.Settings, AppColors.ComposerTeal),
    TrainingAbility("Memoria", Icons.Filled.Psychology, AppColors.Warning),
    TrainingAbility("Rapidez", Icons.Filled.Timer, AppColors.ErrorSoft),
    TrainingAbility("Atención", Icons.Filled.Visibility, AppColors.ComposerAudioCyan),
    TrainingAbility("Adaptación", Icons.Filled.Bolt, AppColors.NeuViolet),
    TrainingAbility("Coordinación", Icons.Filled.Groups, AppColors.ComposerTeal),
)

private data class HomeTab(val label: String, val icon: ImageVector)

private val homeTabs = listOf(
    HomeTab("Inicio", Icons.Filled.Home),
    HomeTab("Entrenar", Icons.Filled.BarChart),
    HomeTab("Progreso", Icons.Filled.Speed),
    HomeTab("Explorar", Icons.Filled.Explore),
    HomeTab("Perfil", Icons.Filled.Person),
)

/**
 * Standalone Molvex home concept. Pass the two image painters from Compose resources
 * in the consuming KMP module; the screen keeps its preview interactions local.
 */
@Composable
fun MolvexHomeVariant(
    mountainPainter: Painter,
    brainPainter: Painter,
    displayName: String = "Usuario",
    onStartMission: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("Inicio") }
    var selectedAbility by remember { mutableStateOf<String?>(null) }
    var duration by remember { mutableStateOf(10) }
    var showDurationPicker by remember { mutableStateOf(false) }
    var missionStarted by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NightNavy),
    ) {
        Image(
            painter = mountainPainter,
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(410.dp)
                .align(Alignment.TopCenter),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to NightNavy.copy(alpha = 0.12f),
                        0.24f to NightNavy.copy(alpha = 0.42f),
                        0.52f to NightNavy.copy(alpha = 0.93f),
                        1f to NightNavy,
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = AppSpacing.L)
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp, bottom = 114.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            HomeBrandHeader(displayName, onOpenProfile)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "¡Buenas noches!",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp),
                    color = Color.White,
                )
                Text(
                    text = "Hoy puede ser un gran día.",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp),
                    color = MutedWhite,
                )
            }

            MissionCard(
                brainPainter = brainPainter,
                duration = duration,
                missionStarted = missionStarted,
                onStart = {
                    missionStarted = !missionStarted
                    if (missionStarted) onStartMission()
                },
                onChangeDuration = { showDurationPicker = true },
            )

            SearchField(
                query = query,
                onQueryChange = { query = it },
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Entrenamiento por capacidad",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "Ver todo  ›",
                        color = AppColors.CyanFocus,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .clip(AppShapes.Small)
                            .clickable { selectedTab = "Entrenar" }
                            .padding(start = 8.dp, vertical = 8.dp),
                    )
                }

                val visibleAbilities = trainingAbilities.filter {
                    it.name.contains(query.trim(), ignoreCase = true)
                }
                if (visibleAbilities.isEmpty()) {
                    Text(
                        text = "No hay capacidades con ese nombre.",
                        color = MutedWhite,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 18.dp),
                    )
                } else {
                    visibleAbilities.chunked(4).forEach { rowAbilities ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowAbilities.forEach { ability ->
                                AbilityTile(
                                    ability = ability,
                                    selected = ability.name == selectedAbility,
                                    onClick = { selectedAbility = ability.name },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(4 - rowAbilities.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }

            EvolutionPanel()
        }

        HomeBottomBar(
            selectedTab = selectedTab,
            onSelect = { selectedTab = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (showDurationPicker) {
        AlertDialog(
            onDismissRequest = { showDurationPicker = false },
            containerColor = NightNavy,
            title = { Text("Duración de la misión", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(5, 10, 15, 20).forEach { minutes ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AppShapes.Small)
                                .clickable {
                                    duration = minutes
                                    showDurationPicker = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("$minutes minutos", color = Color.White, modifier = Modifier.weight(1f))
                            if (duration == minutes) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = AppColors.NeuCyan)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDurationPicker = false }) {
                    Text("Cerrar", color = AppColors.CyanFocus)
                }
            },
        )
    }
}

@Composable
private fun HomeBrandHeader(displayName: String, onOpenProfile: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = buildAnnotatedString {
                    append("M")
                    withStyle(SpanStyle(color = AppColors.NeuCyan)) { append("O") }
                    append("LVEX")
                },
                color = Color.White,
                fontSize = 27.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "Tu compañero inteligente\nen el camino al éxito",
                color = MutedWhite,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 17.sp,
            )
        }
        Row(
            modifier = Modifier
                .clip(AppShapes.Pill)
                .background(GlassWhite)
                .border(BorderStroke(1.dp, GlassStroke), AppShapes.Pill)
                .clickable(onClick = onOpenProfile)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(AppColors.ComposerBlue, AppColors.NeuViolet)),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = "Hola, $displayName",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 96.dp),
                )
                Text("Nivel básico", color = MutedWhite, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
private fun MissionCard(
    brainPainter: Painter,
    duration: Int,
    missionStarted: Boolean,
    onStart: () -> Unit,
    onChangeDuration: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(18.dp, AppShapes.Large)
            .clip(AppShapes.Large)
            .background(
                Brush.linearGradient(
                    listOf(
                        AppColors.ComposerBlue.copy(alpha = 0.42f),
                        AppColors.DialogField.copy(alpha = 0.78f),
                        AppColors.StrokeDialog.copy(alpha = 0.58f),
                    ),
                ),
            )
            .border(BorderStroke(1.dp, GlassStroke), AppShapes.Large)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = AppColors.ComposerBlue.copy(alpha = 0.54f),
                shape = AppShapes.Pill,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Filled.LocationOn, null, tint = AppColors.NeuCyan, modifier = Modifier.size(18.dp))
                    Text("Mapa de ruta", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Estabilidad bajo presión",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 25.sp, lineHeight = 29.sp),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Entrenamiento enfocado en mantener el ritmo durante fluctuaciones de velocidad.",
                    color = Color.White.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp,
                )
            }
            Image(
                painter = brainPainter,
                contentDescription = "Ilustración de un cerebro conectado",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MissionDetail(Icons.Filled.FitnessCenter, "Enfoque", "$duration min", Modifier.weight(1f))
            MissionDetail(Icons.Filled.BarChart, "Dificultad", "Básico", Modifier.weight(1f))
            MissionDetail(Icons.Filled.Psychology, "Tipo", "Reflexivo", Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.weight(1.15f).height(44.dp).clickable(onClick = onStart),
                shape = AppShapes.Pill,
                color = Color.Transparent,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
            ) {
                Box(
                    modifier = Modifier.background(
                        Brush.horizontalGradient(listOf(AppColors.ComposerAudioCyan, AppColors.NeuViolet)),
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (missionStarted) Icons.Filled.Check else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            if (missionStarted) "Misión en marcha" else "Iniciar misión",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
            }
            Surface(
                modifier = Modifier.weight(0.85f).height(44.dp).clickable(onClick = onChangeDuration),
                shape = AppShapes.Pill,
                color = GlassWhite,
                border = BorderStroke(1.dp, GlassStroke),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Filled.Tune, null, tint = Color.White, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Duración", color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun MissionDetail(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(AppShapes.Large)
            .background(Color.White.copy(alpha = 0.15f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)), AppShapes.Large)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = AppColors.CyanFocus, modifier = Modifier.size(21.dp))
        Column {
            Text(label, color = MutedWhite, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            Text(value, color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(AppShapes.Pill)
            .background(Color.White.copy(alpha = 0.18f))
            .border(BorderStroke(1.dp, GlassStroke), AppShapes.Pill)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = "Buscar capacidades", tint = Color.White, modifier = Modifier.size(23.dp))
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
            decorationBox = { innerTextField ->
                Box {
                    if (query.isEmpty()) {
                        Text("¿Qué quieres entrenar hoy?", color = MutedWhite, style = MaterialTheme.typography.bodyLarge)
                    }
                    innerTextField()
                }
            },
        )
        Icon(Icons.Filled.Mic, contentDescription = "Entrada de voz", tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun AbilityTile(
    ability: TrainingAbility,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = AppShapes.Large
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(if (selected) ability.accent.copy(alpha = 0.32f) else GlassWhite)
            .border(BorderStroke(1.dp, if (selected) ability.accent else GlassStroke), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(ability.accent, ability.accent.copy(alpha = 0.62f))),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(ability.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(25.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            ability.name,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "Entrenamiento",
            color = MutedWhite,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            maxLines = 1,
        )
    }
}

@Composable
private fun EvolutionPanel() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.ExtraLarge)
            .background(
                Brush.horizontalGradient(
                    listOf(AppColors.ComposerBlue.copy(alpha = 0.36f), Color.White.copy(alpha = 0.20f)),
                ),
            )
            .border(BorderStroke(1.dp, GlassStroke), AppShapes.ExtraLarge)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(AppColors.ComposerAudioCyan.copy(alpha = 0.48f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Bolt, null, tint = Color.White, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Tu evolución", color = Color.White, fontWeight = FontWeight.Bold)
            Text("Tu progreso aparecerá aquí cuando completes sesiones.", color = MutedWhite, style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = Color.White, modifier = Modifier.size(23.dp))
    }
}

@Composable
private fun HomeBottomBar(
    selectedTab: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .clip(AppShapes.ExtraLarge)
            .background(AppColors.DialogField.copy(alpha = 0.94f))
            .border(BorderStroke(1.dp, GlassStroke), AppShapes.ExtraLarge)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        homeTabs.forEach { tab ->
            val selected = tab.label == selectedTab
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(AppShapes.Large)
                    .clickable { onSelect(tab.label) }
                    .padding(vertical = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 54.dp, height = 34.dp)
                        .clip(AppShapes.Pill)
                        .background(
                            if (selected) Brush.horizontalGradient(
                                listOf(AppColors.ComposerAudioCyan, AppColors.NeuViolet),
                            ) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(tab.icon, contentDescription = tab.label, tint = Color.White, modifier = Modifier.size(23.dp))
                }
                Text(
                    tab.label,
                    color = if (selected) Color.White else MutedWhite,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}
