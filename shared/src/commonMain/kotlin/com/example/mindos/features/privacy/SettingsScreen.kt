package com.example.mindos.features.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mindos.core.logging.ObservabilityHub
import com.example.mindos.core.session.SessionRepository
import com.example.mindos.design.AppColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** The user must type this exact word to enable the irreversible delete action. */
private const val DELETE_CONFIRMATION_WORD = "ELIMINAR"

@Composable
fun SettingsScreen(
    privacyState: MutableState<Boolean>,
    sessionRepository: SessionRepository,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var confirmText by remember { mutableStateOf("") }
    var isDeleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.DeepBlack)
            .statusBarsPadding()
            .padding(22.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "CERRAR",
                color = AppColors.OnBackground,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onBack)
                    .background(Color.White.copy(alpha = 0.055f))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "AJUSTES",
                color = AppColors.OnBackground,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 35.sp, fontWeight = FontWeight.Black),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if (privacyState.value) "PRIVACIDAD: ACTIVA" else "PRIVACIDAD: INACTIVA",
                color = AppColors.NeuGreen,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.045f))
                    .clickable { privacyState.value = !privacyState.value }
                    .padding(18.dp),
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Danger zone: account deletion (store requirement / GDPR Art. 17) ──
            Text(
                text = "ZONA DE PELIGRO",
                color = AppColors.Mist,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "ELIMINAR CUENTA",
                color = AppColors.Error,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.Error.copy(alpha = 0.10f))
                    .clickable {
                        confirmText = ""
                        deleteError = null
                        showDeleteDialog = true
                    }
                    .padding(18.dp),
            )
        }
    }

    if (showDeleteDialog) {
        val canConfirm = confirmText.trim() == DELETE_CONFIRMATION_WORD && !isDeleting
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
            title = { Text("Eliminar cuenta") },
            text = {
                Column {
                    Text(
                        "Esta acción es permanente e irreversible. Se eliminarán tu perfil, " +
                            "publicaciones, mensajes y datos asociados.",
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Perderás tu saldo de monedas, que NO es reembolsable.",
                        color = AppColors.Error,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Escribe $DELETE_CONFIRMATION_WORD para confirmar:")
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = confirmText,
                        onValueChange = { confirmText = it },
                        singleLine = true,
                        enabled = !isDeleting,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    deleteError?.let { message ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(message, color = AppColors.Error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = canConfirm,
                    onClick = {
                        isDeleting = true
                        deleteError = null
                        scope.launch {
                            try {
                                // On success the session clears → authState emits null →
                                // App.kt routes back to Auth. No explicit navigation needed.
                                sessionRepository.deleteAccount()
                            } catch (error: CancellationException) {
                                throw error
                            } catch (@Suppress("TooGenericExceptionCaught") error: Throwable) {
                                // Surface to the user AND forward to observability (not swallowed):
                                // a failed deletion is operationally important to see in Crashlytics.
                                ObservabilityHub.recordNonFatal(error)
                                isDeleting = false
                                deleteError = "No se pudo eliminar la cuenta. Inténtalo de nuevo."
                            }
                        }
                    },
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = AppColors.Error,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Eliminar definitivamente", color = AppColors.Error)
                    }
                }
            },
            dismissButton = {
                TextButton(enabled = !isDeleting, onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            },
        )
    }
}
