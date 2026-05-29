package com.example.mindos.features.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mindos.core.security.SafeInput
import com.example.mindos.core.session.AuthenticatedUser
import com.example.mindos.core.session.SessionRepository
import com.example.mindos.core.session.toAuthFailureDiagnostic
import com.example.mindos.core.session.toUserMessage
import com.example.mindos.design.AppColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private const val MAX_ATTEMPTS_BEFORE_LOCKOUT = 5
private const val LOCKOUT_DURATION_MS = 30_000L

@Composable
fun AuthScreen(
    sessionRepository: SessionRepository,
    onAuthenticated: (AuthenticatedUser) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var isCreatingAccount by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showPasswordReset by remember { mutableStateOf(false) }

    var failedAttempts by remember { mutableIntStateOf(0) }
    var lockoutUntil by remember { mutableLongStateOf(0L) }

    fun isLockedOut(): Boolean {
        if (failedAttempts < MAX_ATTEMPTS_BEFORE_LOCKOUT) return false
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        if (now >= lockoutUntil) {
            failedAttempts = 0
            return false
        }
        return true
    }

    fun submit(createAccount: Boolean) {
        if (isSubmitting) return
        if (isLockedOut()) {
            errorMessage = "Demasiados intentos. Espera unos segundos antes de reintentar."
            return
        }
        scope.launch {
            isSubmitting = true
            errorMessage = null
            successMessage = null
            runCatching {
                if (createAccount) {
                    sessionRepository.createUserWithEmailAndPassword(
                        email = email,
                        password = password,
                        displayName = displayName,
                    )
                } else {
                    sessionRepository.signInWithEmailAndPassword(
                        email = email,
                        password = password,
                    )
                }
            }.fold(
                onSuccess = { user ->
                    failedAttempts = 0
                    onAuthenticated(user)
                },
                onFailure = { error ->
                    failedAttempts++
                    if (failedAttempts >= MAX_ATTEMPTS_BEFORE_LOCKOUT) {
                        lockoutUntil = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() + LOCKOUT_DURATION_MS
                    }
                    val diagnostic = error.toAuthFailureDiagnostic()
                    errorMessage = diagnostic.toUserMessage(email.trim())
                },
            )
            isSubmitting = false
        }
    }

    fun submitPasswordReset() {
        if (isSubmitting) return
        if (isLockedOut()) {
            errorMessage = "Demasiados intentos. Espera unos segundos antes de reintentar."
            return
        }
        scope.launch {
            isSubmitting = true
            errorMessage = null
            successMessage = null
            try {
                sessionRepository.sendPasswordResetEmail(email)
                successMessage = "Si el email existe, recibiras un enlace para restablecer tu contrasena."
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                successMessage = "Si el email existe, recibiras un enlace para restablecer tu contrasena."
            }
            isSubmitting = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "MindOS",
            color = AppColors.NeuGold,
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (showPasswordReset) "Restablecer contrasena" else "Acceso seguro con Firebase",
            color = AppColors.NeuGold.copy(alpha = 0.82f),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(28.dp))

        if (isCreatingAccount && !showPasswordReset) {
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = SafeInput.normalizeSingleLineHumanText(it, 80) },
                label = { Text("Nombre visible") },
                singleLine = true,
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
        }
        OutlinedTextField(
            value = email,
            onValueChange = { email = SafeInput.normalizeSingleLineHumanText(it, 254) },
            label = { Text("Email") },
            singleLine = true,
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!showPasswordReset) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it.filterNot { char -> char.code in 0..31 || char.code == 127 }.take(128) },
                label = { Text("Contraseña") },
                singleLine = true,
                enabled = !isSubmitting,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(18.dp))

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
        }

        successMessage?.let {
            Text(
                text = it,
                color = AppColors.NeuGreen,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
        }

        if (showPasswordReset) {
            Button(
                onClick = { submitPasswordReset() },
                enabled = !isSubmitting && email.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Enviar enlace de restablecimiento")
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Volver al inicio de sesion",
                color = AppColors.NeuGreen,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable {
                    showPasswordReset = false
                    errorMessage = null
                    successMessage = null
                },
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = { submit(createAccount = isCreatingAccount) },
                    enabled = !isSubmitting,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (isCreatingAccount) "Crear cuenta" else "Entrar")
                }
                OutlinedButton(
                    onClick = {
                        errorMessage = null
                        successMessage = null
                        isCreatingAccount = !isCreatingAccount
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (isCreatingAccount) "Ya tengo cuenta" else "Nueva cuenta")
                }
            }
            if (!isCreatingAccount) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "He olvidado mi contrasena",
                    color = AppColors.NeuGreen,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable {
                        showPasswordReset = true
                        errorMessage = null
                        successMessage = null
                    },
                )
            }
        }

        if (isSubmitting) {
            Spacer(Modifier.height(18.dp))
            CircularProgressIndicator(color = AppColors.NeuGold)
        }
    }
}
