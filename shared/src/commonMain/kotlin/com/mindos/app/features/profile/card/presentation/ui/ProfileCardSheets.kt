@file:Suppress("FunctionNaming", "MagicNumber", "LongMethod", "LongParameterList", "TooManyFunctions", "MatchingDeclarationName", "MaxLineLength")

package com.mindos.app.features.profile.card.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.features.profile.card.domain.GradientKind
import com.mindos.app.features.profile.card.presentation.ColorMath
import com.mindos.app.features.profile.card.presentation.HsvColor
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/** Which surface the color picker is editing. */
enum class ColorTarget { SCREEN, CARD }

private val PALETTES = listOf(
    listOf(0xFF0E1E3D, 0xFF1B3B6F, 0xFF3E6FB0, 0xFFC9A24B, 0xFFF5EFE0),
    listOf(0xFF1A1024, 0xFF3D1E4F, 0xFF7B3FA0, 0xFFE0556B, 0xFFFFD98A),
    listOf(0xFF06140F, 0xFF0F3D2E, 0xFF1E8A5B, 0xFF3FBF8F, 0xFFE8F5EE),
    listOf(0xFF1B0B0B, 0xFF5A1A1A, 0xFFB33030, 0xFFE8C77A, 0xFFF0E6D2),
)

private data class GradientSwatch(val stops: List<Long>, val kind: GradientKind)

private val GRADIENTS = listOf(
    GradientSwatch(listOf(0xFF1B2746, 0xFF0B1020), GradientKind.LINEAR_DIAGONAL),
    GradientSwatch(listOf(0xFF3D1E4F, 0xFF1A1024), GradientKind.LINEAR_DIAGONAL),
    GradientSwatch(listOf(0xFF0F3D2E, 0xFF06140F), GradientKind.LINEAR_DIAGONAL),
    GradientSwatch(listOf(0xFF5A1A1A, 0xFF1B0B0B), GradientKind.RADIAL_TOP),
    GradientSwatch(listOf(0xFFC9A24B, 0xFF5A4418), GradientKind.LINEAR_DIAGONAL),
    GradientSwatch(listOf(0xFF2B3354, 0xFF0A0E17), GradientKind.LINEAR_DIAGONAL),
)

/** Scrim shown behind any open sheet. */
@Composable
fun SheetScrim(visible: Boolean, onClose: () -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(Modifier.fillMaxSize().background(Color(0x9E030408)).clickable(onClick = onClose))
    }
}

/** HSV color picker bottom sheet with palettes, gradients, recents and saved swatches. */
@Composable
fun ColorPickerSheet(
    visible: Boolean,
    target: ColorTarget,
    initialArgb: Long,
    recent: List<Long>,
    saved: List<Long>,
    onApply: (Long) -> Unit,
    onSaveColor: (Long) -> Unit,
    onPickGradient: (List<Long>, GradientKind) -> Unit,
    onClose: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    ) {
        var hsv by remember(initialArgb, visible) { mutableStateOf(ColorMath.argbToHsv(initialArgb)) }
        val currentArgb = ColorMath.hsvToArgb(hsv.hue, hsv.saturation, hsv.value)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF101218), Color(0xFF0A0B10))))
                .padding(horizontal = 22.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            SheetGrabber()
            SheetTitle(
                title = if (target == ColorTarget.SCREEN) "Fondo de pantalla" else "Color de la tarjeta",
                subtitle = "Vista previa en tiempo real",
                onClose = onClose,
            )
            // preview
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(currentArgb)).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Color(currentArgb)).border(2.dp, Color(0x4DFFFFFF), RoundedCornerShape(11.dp)))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Vista previa", color = ProfileCardTokens.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(ColorMath.toHex(currentArgb), color = ProfileCardTokens.TextMuted, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                HsvWheel(hsv = hsv, onChange = { hsv = it }, modifier = Modifier.size(178.dp))
                Spacer(Modifier.width(18.dp))
                ValueBar(hsv = hsv, onChange = { hsv = it }, modifier = Modifier.width(30.dp).height(178.dp))
            }
            Spacer(Modifier.height(18.dp))
            SectionLabel("Paletas")
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                PALETTES.forEach { palette ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        palette.forEach { c ->
                            Box(
                                Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(9.dp)).background(Color(c))
                                    .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(9.dp))
                                    .clickable { hsv = ColorMath.argbToHsv(c) },
                            )
                        }
                    }
                }
            }
            if (target == ColorTarget.SCREEN) {
                Spacer(Modifier.height(18.dp))
                SectionLabel("Degradados")
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    GRADIENTS.take(3).forEach { g -> GradientSwatchView(g) { onPickGradient(g.stops, g.kind) } }
                }
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    GRADIENTS.drop(3).forEach { g -> GradientSwatchView(g) { onPickGradient(g.stops, g.kind) } }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                SwatchColumn("Recientes", recent, onPick = { hsv = ColorMath.argbToHsv(it) }, modifier = Modifier.weight(1f))
                SwatchColumn(
                    "Guardados", saved, onPick = { hsv = ColorMath.argbToHsv(it) },
                    onAdd = { onSaveColor(currentArgb) }, modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFD8D8DA))))
                    .clickable { onApply(currentArgb) }.padding(15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Aplicar color", color = ProfileCardTokens.ScreenInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HsvWheel(hsv: HsvColor, onChange: (HsvColor) -> Unit, modifier: Modifier = Modifier) {
    var sizePx by remember { mutableStateOf(IntSize.Zero) }
    fun update(pos: Offset) {
        val cx = sizePx.width / 2f
        val cy = sizePx.height / 2f
        if (cx <= 0f) return
        val dx = pos.x - cx
        val dy = pos.y - cy
        val hue = ((atan2(dy.toDouble(), dx.toDouble()) * 180.0 / kotlin.math.PI + 360.0) % 360.0).toFloat()
        val sat = (min(hypot(dx.toDouble(), dy.toDouble()).toFloat(), cx) / cx)
        onChange(hsv.copy(hue = hue, saturation = sat))
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(
                Brush.sweepGradient(
                    listOf(
                        Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red,
                    ),
                ),
            )
            .background(Brush.radialGradient(listOf(Color.White, Color.Transparent)))
            .onSizeChanged { sizePx = it }
            .pointerInput(Unit) { detectTapGestures { update(it) } }
            .pointerInput(Unit) { detectDragGestures(onDragStart = { update(it) }, onDrag = { ch, _ -> ch.consume(); update(ch.position) }) },
    ) {
        val cx = sizePx.width / 2f
        val angle = hsv.hue * kotlin.math.PI.toFloat() / 180f
        val r = hsv.saturation * cx
        val density = LocalDensity.current
        val knobX = with(density) { (cx + cos(angle.toDouble()).toFloat() * r).toDp() }
        val knobY = with(density) { (cx + sin(angle.toDouble()).toFloat() * r).toDp() }
        Box(
            Modifier.offset(x = knobX - 11.dp, y = knobY - 11.dp).size(22.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(Color(ColorMath.hsvToArgb(hsv.hue, hsv.saturation, hsv.value)))
                .border(3.dp, Color.White, RoundedCornerShape(percent = 50)),
        )
    }
}

@Composable
private fun ValueBar(hsv: HsvColor, onChange: (HsvColor) -> Unit, modifier: Modifier = Modifier) {
    var heightPx by remember { mutableStateOf(0) }
    val hueColor = Color(ColorMath.hsvToArgb(hsv.hue, 1f, 1f))
    fun update(pos: Offset) {
        if (heightPx <= 0) return
        onChange(hsv.copy(value = (1f - pos.y / heightPx).coerceIn(0f, 1f)))
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(hueColor, Color.Black)))
            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(16.dp))
            .onSizeChanged { heightPx = it.height }
            .pointerInput(Unit) { detectTapGestures { update(it) } }
            .pointerInput(Unit) { detectDragGestures(onDragStart = { update(it) }, onDrag = { ch, _ -> ch.consume(); update(ch.position) }) },
    ) {
        val density = LocalDensity.current
        val knobY = with(density) { ((1f - hsv.value) * heightPx).toDp() }
        Box(
            Modifier.fillMaxWidth().offset(y = knobY - 7.dp).height(14.dp)
                .clip(RoundedCornerShape(7.dp)).background(Color.White),
        )
    }
}

@Composable
private fun GradientSwatchView(swatch: GradientSwatch, onClick: () -> Unit) {
    val brush = when (swatch.kind) {
        GradientKind.LINEAR_DIAGONAL -> Brush.linearGradient(swatch.stops.map { Color(it) })
        GradientKind.RADIAL_TOP -> Brush.radialGradient(swatch.stops.map { Color(it) })
    }
    Box(Modifier.size(58.dp).clip(RoundedCornerShape(14.dp)).background(brush).border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(14.dp)).clickable(onClick = onClick))
}

@Composable
private fun SwatchColumn(
    label: String,
    colors: List<Long>,
    onPick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onAdd: (() -> Unit)? = null,
) {
    Column(modifier) {
        SectionLabel(label)
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            colors.take(4).forEach { c ->
                Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(Color(c)).border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(9.dp)).clickable { onPick(c) })
            }
            if (onAdd != null) {
                Box(
                    Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(Color(0x0DFFFFFF)).border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(9.dp)).clickable(onClick = onAdd),
                    contentAlignment = Alignment.Center,
                ) { Text("+", color = ProfileCardTokens.TextMuted, fontSize = 18.sp) }
            }
        }
    }
}

/** Card-background sheet: solid color, image/GIF, reset. */
@Composable
fun CardBackgroundSheet(
    visible: Boolean,
    username: String,
    hasMedia: Boolean,
    onOpenSolid: () -> Unit,
    onPickImage: () -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    ) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF101218), Color(0xFF0A0B10)))).padding(horizontal = 22.dp),
        ) {
            SheetGrabber()
            SheetTitle(title = "Fondo de la tarjeta", subtitle = "Sólido · Imagen · GIF", onClose = onClose)
            Box(
                Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF1C2440), Color(0xFF0E1426)))),
                contentAlignment = Alignment.BottomStart,
            ) {
                Text(username, color = ProfileCardTokens.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(16.dp))
            }
            Spacer(Modifier.height(18.dp))
            SheetOption("🎨", "Color sólido", "Rueda cromática y paletas", onOpenSolid)
            Spacer(Modifier.height(10.dp))
            SheetOption("🖼️", "Imagen o GIF", "Se aplica al instante (cover)", onPickImage)
            if (hasMedia) {
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Color(0x1FE0556B))
                        .border(1.dp, Color(0x66E0556B), RoundedCornerShape(15.dp)).clickable(onClick = onReset).padding(13.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("↺ Restablecer fondo", color = ProfileCardTokens.Negative, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun SheetOption(glyph: String, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Color(0x0AFFFFFF))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(15.dp)).clickable(onClick = onClick).padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(11.dp)).background(Color(0x2E4F7CFF)), contentAlignment = Alignment.Center) {
            Text(glyph, fontSize = 19.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = ProfileCardTokens.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = ProfileCardTokens.TextFaint, fontSize = 11.5.sp)
        }
        Text("›", color = ProfileCardTokens.TextGhost, fontSize = 18.sp)
    }
}

/** Post preview modal (centered card). */
@Composable
fun PostPreviewModal(visible: Boolean, title: String, meta: String, gradient: Pair<Long, Long>?, isVideo: Boolean, onClose: () -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(Modifier.fillMaxSize().clickable(onClick = onClose), contentAlignment = Alignment.Center) {
            Column(
                Modifier.width(300.dp).clip(RoundedCornerShape(22.dp)).background(ProfileCardTokens.SurfaceLow)
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(22.dp)),
            ) {
                Box(
                    Modifier.fillMaxWidth().height(320.dp).background(
                        Brush.linearGradient(listOf(Color(gradient?.first ?: 0xFF11151F), Color(gradient?.second ?: 0xFF11151F))),
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isVideo) {
                        Box(Modifier.size(54.dp).clip(RoundedCornerShape(percent = 50)).background(Color(0x73000000)), contentAlignment = Alignment.Center) {
                            Text("▶", color = ProfileCardTokens.White, fontSize = 20.sp)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(title, color = ProfileCardTokens.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(meta, color = ProfileCardTokens.TextFaint, fontSize = 10.sp)
                    }
                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(percent = 50)).background(Color(0x14FFFFFF)).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                        Text("✕", color = ProfileCardTokens.White, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

// ---- small shared sheet pieces ----

@Composable
private fun SheetGrabber() {
    Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.width(38.dp).height(4.dp).clip(RoundedCornerShape(3.dp)).background(Color(0x2EFFFFFF)))
    }
}

@Composable
private fun SheetTitle(title: String, subtitle: String, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = ProfileCardTokens.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = ProfileCardTokens.TextGhost, fontSize = 10.sp)
        }
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(percent = 50)).background(Color(0x14FFFFFF)).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
            Text("✕", color = ProfileCardTokens.White, fontSize = 16.sp)
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), color = ProfileCardTokens.TextGhost, fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(9.dp))
}
