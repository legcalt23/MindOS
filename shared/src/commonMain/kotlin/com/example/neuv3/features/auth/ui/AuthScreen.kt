package com.example.neuv3.features.auth.ui

import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.neuv3.core.security.SafeInput
import com.example.neuv3.core.session.AuthenticatedUser
import com.example.neuv3.core.session.SessionRepository
import com.example.neuv3.core.session.toAuthFailureDiagnostic
import com.example.neuv3.core.session.toUserMessage
import kotlinx.coroutines.launch

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

    fun submit(createAccount: Boolean) {
        if (isSubmitting) return
        scope.launch {
            isSubmitting = true
            errorMessage = null
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
                onSuccess = onAuthenticated,
                onFailure = { error ->
                    val diagnostic = error.toAuthFailureDiagnostic()
                    errorMessage = diagnostic.toUserMessage(email.trim())
                },
            )
            isSubmitting = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "NEU",
            color = Color(0xFFE7E0CF),
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Acceso seguro con Firebase",
            color = Color(0xFFE7E0CF).copy(alpha = 0.82f),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(28.dp))

        if (isCreatingAccount) {
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
        Spacer(Modifier.height(18.dp))

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
        }

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
                    isCreatingAccount = !isCreatingAccount
                },
                enabled = !isSubmitting,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (isCreatingAccount) "Ya tengo cuenta" else "Nueva cuenta")
            }
        }

        if (isSubmitting) {
            Spacer(Modifier.height(18.dp))
            CircularProgressIndicator(color = Color(0xFFE7E0CF))
        }
    }
}
