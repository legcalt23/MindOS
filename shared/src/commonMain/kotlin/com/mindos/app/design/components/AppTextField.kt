@file:Suppress(
    // Every public API here is a Composable, which Kotlin/Compose conventionally
    // names in PascalCase; this repo's Detekt does not exempt @Composable, so the
    // suppression is applied once at file scope.
    "FunctionNaming",
    // The fields expose a flat, Material-style surface (mirroring OutlinedTextField)
    // and the private helpers thread the resolved state down — both legitimately
    // exceed Detekt's 6-parameter threshold.
    "LongParameterList",
)

package com.mindos.app.design.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mindos.app.design.AppAnimation
import com.mindos.app.design.AppColors
import com.mindos.app.design.AppShapes
import com.mindos.app.design.AppSpacing

/*
 * AppTextField — the single, reusable text input for MindOS.
 *
 * Visual/behavioural port of the Uiverse "floating label" CSS component
 * (by Maximinodotpy) into idiomatic Compose Multiplatform, adapted to the
 * project's dark Material 3 design system:
 *
 *  - Rounded 20.dp outline (AppShapes.ExtraLarge), transparent fill, 2.dp border.
 *  - Border is neutral grey at rest and lavender when focused OR filled
 *    (mirrors the CSS `:focus, :valid` selector).
 *  - The label starts inside the field (acting as a placeholder) and, when the
 *    field is focused or non-empty, animates up onto the top border at 0.9x
 *    scale with a solid background chip that "cuts" the outline cleanly.
 *  - ~300ms animation (AppAnimation.DurationMedium), single source of truth.
 *
 * Lives in commonMain so Android / iOS / Desktop all share one implementation.
 */

// ── Layout tokens (all sourced from the design system; no bare literals) ─────
private val InputMinHeight: Dp = AppSpacing.InputHeight
private val InputHorizontalPadding: Dp = AppSpacing.M
private val InputVerticalPadding: Dp = AppSpacing.M
private val InputBorderWidth: Dp = AppSpacing.XXS
private val IconGap: Dp = AppSpacing.S
private val PrefixGap: Dp = AppSpacing.XXS
private val LeadingSlotWidth: Dp = AppSpacing.IconSize
private val LabelOverflowReserve: Dp = AppSpacing.M
private val LabelChipHorizontalPadding: Dp = AppSpacing.XS
private val ErrorTextTopPadding: Dp = AppSpacing.XS
private val InputShape = AppShapes.ExtraLarge
private val LabelChipShape = AppShapes.Small

// ── Animation / geometry constants (const → ignored by Detekt MagicNumber) ───
private const val RESTING_PROGRESS: Float = 0f
private const val FLOATING_PROGRESS: Float = 1f
private const val RESTING_SCALE: Float = 1f
private const val FLOATING_SCALE: Float = 0.9f
private const val RESTING_BIAS_CENTER: Float = 0f
private const val TOP_BIAS: Float = -1f
private const val LABEL_HALF: Float = 0.5f
private const val NO_TRANSLATION: Float = 0f
private const val DISABLED_ALPHA: Float = 0.38f
private const val PLACEHOLDER_ALPHA: Float = 0.6f
private const val DEFAULT_MULTILINE_MAX_LINES: Int = 6

/**
 * Immutable colour bundle for [AppTextField]. Build one with
 * [AppTextFieldDefaults.colors]; override any slot to theme a field for a
 * non-default surface (e.g. pass a card colour to [labelBackground] so the
 * floating label cuts the border on that surface instead of the screen).
 */
@Immutable
data class AppTextFieldColors(
    val text: Color,
    val cursor: Color,
    val defaultBorder: Color,
    val focusedBorder: Color,
    val errorBorder: Color,
    val label: Color,
    val labelBackground: Color,
)

/** Default colour factories for [AppTextField], mirroring Material's pattern. */
object AppTextFieldDefaults {

    /**
     * Theme-derived colours. [labelBackground] defaults to the screen
     * background so the floating label chip cleanly "cuts" the outline; pass the
     * actual surface colour when the field sits on a card/sheet.
     */
    @Composable
    fun colors(
        text: Color = AppColors.OnBackground,
        cursor: Color = AppColors.CyanFocus,
        defaultBorder: Color = AppColors.InputBorder,
        focusedBorder: Color = AppColors.InputBorderFocused,
        errorBorder: Color = AppColors.Error,
        label: Color = AppColors.InputLabel,
        labelBackground: Color = MaterialTheme.colorScheme.background,
    ): AppTextFieldColors = AppTextFieldColors(
        text = text,
        cursor = cursor,
        defaultBorder = defaultBorder,
        focusedBorder = focusedBorder,
        errorBorder = errorBorder,
        label = label,
        labelBackground = labelBackground,
    )
}

/**
 * True when the label should sit floated on the border rather than resting
 * inside the field: on focus, when there is text, or when a [prefix] is present
 * (a prefix always occupies the field body, so the label stays up). Pure so it
 * can be unit-tested without a composition.
 */
internal fun appTextFieldIsFloating(focused: Boolean, value: String, hasPrefix: Boolean): Boolean =
    focused || value.isNotEmpty() || hasPrefix

/**
 * True when the optional placeholder hint should be shown: only while the label
 * has floated up (so it does not clash with the resting label) and the field is
 * empty. Pure so it can be unit-tested without a composition.
 */
internal fun appTextFieldShowsPlaceholder(isFloating: Boolean, value: String, placeholder: String?): Boolean =
    placeholder != null && isFloating && value.isEmpty()

/**
 * The canonical MindOS text field. Flat, Material-style API so migrating a
 * `TextField` / `OutlinedTextField` is a near drop-in replacement.
 *
 * @param label floating label; also acts as the resting placeholder.
 * @param placeholder optional hint shown inside the field once the label floats
 *   up and the field is empty (e.g. an example value or format).
 * @param prefix optional inline content shown before the text (e.g. an `@`);
 *   its presence keeps the label floated.
 * @param errorText optional message shown under the field when [isError].
 */
@Composable
// one cohesive field: state derivation + layered layout; splitting only scatters it (file is at the fn cap)
@Suppress("LongMethod", "CyclomaticComplexMethod")
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    errorText: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    prefix: (@Composable () -> Unit)? = null,
    placeholder: String? = null,
    colors: AppTextFieldColors = AppTextFieldDefaults.colors(),
) {
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val isFloating = appTextFieldIsFloating(focused, value, prefix != null)

    val progress by animateFloatAsState(
        targetValue = if (isFloating) FLOATING_PROGRESS else RESTING_PROGRESS,
        animationSpec = tween(AppAnimation.DurationMedium),
        label = "appTextFieldFloat",
    )
    val borderColor by animateColorAsState(
        targetValue = borderTargetColor(enabled, isError, isFloating, colors),
        animationSpec = tween(AppAnimation.DurationMedium),
        label = "appTextFieldBorder",
    )

    val textColor = if (enabled) colors.text else colors.text.copy(alpha = DISABLED_ALPHA)
    val labelColor = labelTargetColor(enabled, isError, colors)
    val restingBias = if (singleLine) RESTING_BIAS_CENTER else TOP_BIAS
    val labelStart = InputBorderWidth + InputHorizontalPadding +
        (if (leadingIcon != null) LeadingSlotWidth + IconGap else NO_TRANSLATION.dp)
    val contentTopPaddingPx = with(density) { (InputBorderWidth + InputVerticalPadding).toPx() }
    val labelVerticalBias = restingBias + (TOP_BIAS - restingBias) * progress

    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().padding(top = LabelOverflowReserve)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = InputMinHeight)
                    .border(InputBorderWidth, borderColor, InputShape)
                    .padding(horizontal = InputHorizontalPadding, vertical = InputVerticalPadding),
                verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(Modifier.width(IconGap))
                }
                if (prefix != null) {
                    prefix()
                    Spacer(Modifier.width(PrefixGap))
                }
                Box(Modifier.weight(RESTING_SCALE)) {
                    placeholder?.let { hint ->
                        if (appTextFieldShowsPlaceholder(isFloating, value, hint)) {
                            Text(
                                text = hint,
                                color = colors.label.copy(alpha = PLACEHOLDER_ALPHA),
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    FieldInput(
                        value = value,
                        onValueChange = onValueChange,
                        enabled = enabled,
                        readOnly = readOnly,
                        singleLine = singleLine,
                        maxLines = maxLines,
                        minLines = minLines,
                        textColor = textColor,
                        cursorColor = colors.cursor,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        visualTransformation = visualTransformation,
                        interactionSource = interactionSource,
                        isError = isError,
                        errorText = errorText,
                    )
                }
                if (trailingIcon != null) {
                    Spacer(Modifier.width(IconGap))
                    trailingIcon()
                }
            }
            FloatingLabel(
                label = label,
                progress = progress,
                color = labelColor,
                backgroundColor = colors.labelBackground,
                singleLine = singleLine,
                contentTopPaddingPx = contentTopPaddingPx,
                modifier = Modifier
                    .align(BiasAlignment(horizontalBias = TOP_BIAS, verticalBias = labelVerticalBias))
                    .padding(start = labelStart),
            )
        }
        errorText?.let {
            if (isError && it.isNotBlank()) {
                Text(
                    text = it,
                    color = colors.errorBorder,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = InputHorizontalPadding, top = ErrorTextTopPadding),
                )
            }
        }
    }
}

/** The raw editable surface. Kept private; [AppTextField] owns the visuals. */
@Composable
private fun FieldInput(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    readOnly: Boolean,
    singleLine: Boolean,
    maxLines: Int,
    minLines: Int,
    textColor: Color,
    cursorColor: Color,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    visualTransformation: VisualTransformation,
    interactionSource: MutableInteractionSource,
    isError: Boolean,
    errorText: String?,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                errorText?.let { if (isError && it.isNotBlank()) error(it) }
            },
        enabled = enabled,
        readOnly = readOnly,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = textColor),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else maxLines,
        minLines = if (singleLine) 1 else minLines,
        visualTransformation = visualTransformation,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(cursorColor),
    )
}

/**
 * The animated floating label. Uses a single [progress] (0 = resting inside the
 * field, 1 = floating on the border) to drive translation, scale and the
 * background chip that cuts the outline. Measures its own height so the floated
 * label straddles the border precisely regardless of font scaling.
 */
@Composable
private fun FloatingLabel(
    label: String,
    progress: Float,
    color: Color,
    backgroundColor: Color,
    singleLine: Boolean,
    contentTopPaddingPx: Float,
    modifier: Modifier = Modifier,
) {
    var heightPx by remember { mutableFloatStateOf(NO_TRANSLATION) }
    val restingTranslation = if (singleLine) NO_TRANSLATION else contentTopPaddingPx
    val floatingTranslation = -heightPx * LABEL_HALF
    val translation = restingTranslation + (floatingTranslation - restingTranslation) * progress
    val scale = RESTING_SCALE + (FLOATING_SCALE - RESTING_SCALE) * progress
    val chipPadding = LabelChipHorizontalPadding * progress

    Box(
        modifier = modifier
            .graphicsLayer {
                translationY = translation
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(pivotFractionX = NO_TRANSLATION, pivotFractionY = LABEL_HALF)
            }
            .onGloballyPositioned { heightPx = it.size.height.toFloat() }
            .background(backgroundColor.copy(alpha = progress), LabelChipShape)
            .padding(horizontal = chipPadding),
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
        )
    }
}

private fun borderTargetColor(
    enabled: Boolean,
    isError: Boolean,
    isFloating: Boolean,
    colors: AppTextFieldColors,
): Color = when {
    !enabled -> colors.defaultBorder.copy(alpha = DISABLED_ALPHA)
    isError -> colors.errorBorder
    isFloating -> colors.focusedBorder
    else -> colors.defaultBorder
}

private fun labelTargetColor(
    enabled: Boolean,
    isError: Boolean,
    colors: AppTextFieldColors,
): Color = when {
    !enabled -> colors.label.copy(alpha = DISABLED_ALPHA)
    isError -> colors.errorBorder
    else -> colors.label
}

/**
 * Password variant: [AppTextField] with a masked value and an eye toggle to
 * reveal/hide it. Same visual base as every other field.
 */
@Composable
fun AppPasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorText: String? = null,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    colors: AppTextFieldColors = AppTextFieldDefaults.colors(),
) {
    var visible by remember { mutableStateOf(false) }
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        enabled = enabled,
        isError = isError,
        errorText = errorText,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = keyboardActions,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }, enabled = enabled) {
                Icon(
                    imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visible) "Ocultar contrasena" else "Mostrar contrasena",
                    tint = colors.label,
                )
            }
        },
        colors = colors,
    )
}

/**
 * Search variant: single-line field with a leading search icon and an IME
 * "search" action. Shares the same visual base.
 */
@Composable
fun AppSearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Buscar",
    enabled: Boolean = true,
    onSearch: (() -> Unit)? = null,
) {
    val colors = AppTextFieldDefaults.colors()
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = onSearch?.let { action -> KeyboardActions(onSearch = { action() }) }
            ?: KeyboardActions.Default,
        leadingIcon = {
            Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = colors.label)
        },
        colors = colors,
    )
}

/**
 * Multiline variant: [AppTextField] configured for wrapping text. The label
 * floats to the first line at rest and up onto the border once filled/focused.
 */
@Composable
fun AppMultilineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorText: String? = null,
    minLines: Int = 1,
    maxLines: Int = DEFAULT_MULTILINE_MAX_LINES,
) {
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        enabled = enabled,
        isError = isError,
        errorText = errorText,
        singleLine = false,
        maxLines = maxLines,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    )
}
