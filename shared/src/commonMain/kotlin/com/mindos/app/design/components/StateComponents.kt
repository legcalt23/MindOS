package com.mindos.app.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mindos.app.core.ui.UserFacingError
import com.mindos.app.design.AppColors
import com.mindos.app.design.AppSpacing

/**
 * Minimum interactive touch-target size (Material / WCAG 2.5.5). Applied to
 * shared action buttons so they remain reachable for motor-impaired users.
 */
private val MinTouchTarget = 48.dp

/**
 * Standardized loading indicator shown when a screen or section is loading.
 *
 * Accessibility: announces "Cargando" to screen readers.
 */
@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier,
    message: String = "Cargando...",
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = message },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(AppSpacing.XXXXL),
                color = AppColors.NeuGreen,
                strokeWidth = AppSpacing.XXS,
            )
            Spacer(modifier = Modifier.height(AppSpacing.L))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.Mist,
            )
        }
    }
}

/**
 * Standardized empty state shown when data loaded but result set is empty.
 */
@Composable
fun EmptyScreen(
    title: String = "Sin contenido",
    message: String = "No hay nada que mostrar aqui.",
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppSpacing.ScreenHorizontal)
            // Announce the empty state to screen readers when it appears.
            .semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.OnBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(AppSpacing.S))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.Mist,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Standardized error state with optional retry button.
 */
@Composable
fun ErrorScreen(
    error: UserFacingError,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppSpacing.ScreenHorizontal)
            // Announce the error to screen readers as soon as it is shown.
            .semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = error.title,
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.Error,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(AppSpacing.S))
            Text(
                text = error.message,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.Mist,
                textAlign = TextAlign.Center,
            )
            if (error.isRetryable && onRetry != null) {
                Spacer(modifier = Modifier.height(AppSpacing.XXL))
                Button(
                    onClick = onRetry,
                    // Guarantee a 48dp minimum touch target (Material / WCAG 2.5.5);
                    // the default Material3 button height is below this threshold.
                    modifier = Modifier.defaultMinSize(
                        minWidth = MinTouchTarget,
                        minHeight = MinTouchTarget,
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.NeuGreen,
                        contentColor = AppColors.DeepBlack,
                    ),
                ) {
                    Text("Reintentar")
                }
            }
        }
    }
}

/**
 * Standardized offline state.
 */
@Composable
fun OfflineScreen(
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    ErrorScreen(
        error = UserFacingError(
            title = "Sin conexion",
            message = "No se pudo conectar. Verifica tu conexion a internet.",
            code = "offline",
            isRetryable = true,
        ),
        onRetry = onRetry,
        modifier = modifier,
    )
}
