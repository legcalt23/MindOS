@file:Suppress("FunctionNaming") // Composable entry points, PascalCase by Compose convention.

package com.mindos.app.features.mindboost.focus.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.core.a11y.platformMinimumTarget

/**
 * A round control whose only visible content is a glyph.
 *
 * The Focus V1 lane — the one the hub actually mounts — had three of these written by hand, and all three
 * carried the same two defects, which is what a hand-copied control is for.
 *
 * **Touch target.** They were 36dp (the two abort buttons) and 40dp (the home back button), below every
 * platform minimum, on the only ways out of a block and of the track. The hit area now comes from
 * [platformMinimumTarget], which answers 48dp on Android and 44pt on iOS rather than one compromise number.
 * The VISIBLE disc keeps whatever diameter its call site already used: these controls should not become
 * louder, only reachable.
 *
 * **Label.** A glyph is not a name. `Text("✕")` and `Text("‹")` inside a clickable `Box` with no
 * `contentDescription` left a screen reader announcing a character and nothing about what it does. The glyph
 * is declared decorative here and the description sits on the clickable node, so exactly one thing is
 * announced — [clearAndSetSemantics] on the child rather than `mergeDescendants` on the parent, because the
 * merge policy for descriptions ACCUMULATES and would announce both.
 */
@Composable
internal fun FocusGlyphButton(
    glyph: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: GlyphStyle = GlyphStyle(),
) {
    val description = contentDescription
    Box(
        modifier = modifier
            .size(platformMinimumTarget)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(style.visibleDiameter)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = glyph,
                color = style.glyphColor,
                fontSize = style.glyphSize,
                // Decorative: the description above is the single announcement for this control.
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
}

/**
 * How the control LOOKS, kept apart from what it does and says.
 *
 * The three fields travel together — they are the one disc a call site is describing — and the hit area is
 * deliberately not among them: that comes from [platformMinimumTarget] and is not a call site's to choose.
 */
internal data class GlyphStyle(
    val visibleDiameter: Dp = 36.dp,
    val glyphSize: TextUnit = 16.sp,
    val glyphColor: Color = AppColors.FocusReadout,
)

/**
 * The only way out of a running Focus block.
 *
 * NOT decided here: whether ending a block should ask for confirmation. Enlarging the hit area to the platform
 * minimum necessarily enlarges the accidental-hit area too, and a confirmation step mid-block changes the flow
 * of a timed task — that is a product decision, registered rather than smuggled in behind an accessibility fix.
 */
@Composable
internal fun FocusAbortButton(onAbort: () -> Unit, modifier: Modifier = Modifier) {
    FocusGlyphButton(
        glyph = "✕",
        // Says what the control does, not what it looks like: a screen reader user cannot see the glyph.
        contentDescription = "Salir del bloque",
        onClick = onAbort,
        modifier = modifier,
    )
}
