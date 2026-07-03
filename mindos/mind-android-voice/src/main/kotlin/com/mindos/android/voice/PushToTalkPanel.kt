package com.mindos.android.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AndroidPushToTalkPanel(
    state: PushToTalkUiState,
    permissionGranted: Boolean,
    onMicrophonePressed: (permissionGranted: Boolean) -> Unit,
    onRequestPermission: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit = {},
    onRejectConfirmation: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(state.text)
            when (state) {
                PushToTalkUiState.DisabledByFlag -> Unit
                is PushToTalkUiState.PermissionRequired ->
                    Button(onClick = onRequestPermission) { Text("Permitir microfono") }
                PushToTalkUiState.Listening,
                PushToTalkUiState.Processing,
                -> OutlinedButton(onClick = onCancel) { Text("Cancelar") }
                is PushToTalkUiState.NeedsConfirmation -> Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = onConfirm) { Text("Confirmar") }
                    OutlinedButton(onClick = onRejectConfirmation) { Text("Cancelar") }
                }
                is PushToTalkUiState.Degraded,
                is PushToTalkUiState.Executed,
                is PushToTalkUiState.Failed,
                PushToTalkUiState.Idle,
                is PushToTalkUiState.Rejected,
                -> Button(onClick = { onMicrophonePressed(permissionGranted) }) { Text("Hablar") }
            }
        }
    }
}
