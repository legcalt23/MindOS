// Every function here is a Composable; this repo's Detekt does not exempt
// @Composable from the camelCase FunctionNaming rule, so suppress at file scope.
@file:Suppress("FunctionNaming")

package com.mindos.app.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mindos.app.design.AppColors
import com.mindos.app.design.AppSpacing
import com.mindos.app.design.MindOSTheme

/*
 * Living usage gallery for [AppTextField] and its variants.
 *
 * This project does not wire Compose `@Preview` tooling into commonMain (there
 * are no @Preview usages anywhere in shared/commonMain), so these are plain
 * composables you can drop into any dev/debug screen — or an IDE interactive
 * run — to see every state at once. A "focused" state can't be forced
 * statically; a filled field shows the same floated-label layout (the only
 * focus delta is the lavender border), so run the app to see the transition.
 */

/** Renders every [AppTextField] state on the app background — one dev entry point. */
@Composable
fun AppTextFieldGallery() {
    SampleSurface {
        EmptySample()
        FilledSample()
        ErrorSample()
        DisabledSample()
        PasswordSample()
        MultilineSample()
        PrefixPlaceholderSample()
    }
}

@Composable
private fun SampleSurface(content: @Composable ColumnScope.() -> Unit) {
    MindOSTheme {
        Column(
            modifier = Modifier
                .background(AppColors.Background)
                .fillMaxWidth()
                .padding(AppSpacing.L),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.M),
            content = content,
        )
    }
}

/** Empty + unfocused: the label rests inside the field as a placeholder. */
@Composable
private fun EmptySample() {
    var value by remember { mutableStateOf("") }
    AppTextField(value = value, onValueChange = { value = it }, label = "Nombre visible")
}

/** Filled: the label has floated onto the border with the lavender outline. */
@Composable
private fun FilledSample() {
    var value by remember { mutableStateOf("ana@mindos.app") }
    AppTextField(value = value, onValueChange = { value = it }, label = "Email")
}

/** Error: red border plus a supporting error message under the field. */
@Composable
private fun ErrorSample() {
    var value by remember { mutableStateOf("ana") }
    AppTextField(
        value = value,
        onValueChange = { value = it },
        label = "Email",
        isError = true,
        errorText = "Introduce un email valido",
    )
}

/** Disabled: dimmed border, label and text; not editable. */
@Composable
private fun DisabledSample() {
    AppTextField(
        value = "Solo lectura",
        onValueChange = {},
        label = "Campo deshabilitado",
        enabled = false,
    )
}

/** Password: masked value with an eye toggle in the trailing slot. */
@Composable
private fun PasswordSample() {
    var value by remember { mutableStateOf("supersecreto") }
    AppPasswordTextField(value = value, onValueChange = { value = it }, label = "Contrasena")
}

/** Multiline: label floats to the first line at rest, up onto the border filled. */
@Composable
private fun MultilineSample() {
    var value by remember { mutableStateOf("Una nota que ocupa\nvarias lineas de texto.") }
    AppMultilineTextField(value = value, onValueChange = { value = it }, label = "Nota")
}

/** Prefix + placeholder: a username field (@) and a URL field with an example hint. */
@Composable
private fun PrefixPlaceholderSample() {
    var username by remember { mutableStateOf("") }
    AppTextField(
        value = username,
        onValueChange = { username = it },
        label = "Username",
        prefix = { Text("@", color = AppColors.InputLabel) },
        placeholder = "usuario",
    )
    var url by remember { mutableStateOf("") }
    AppTextField(
        value = url,
        onValueChange = { url = it },
        label = "Enlace web",
        placeholder = "https://mindos.app",
    )
}
