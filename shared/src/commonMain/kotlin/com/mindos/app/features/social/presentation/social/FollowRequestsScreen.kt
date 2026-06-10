package com.mindos.app.features.social.presentation.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mindos.app.features.social.domain.model.FollowRequest
import com.mindos.app.features.social.domain.valueobject.RequestId
import com.mindos.app.features.social.presentation.state.FollowRequestsUiState
import com.mindos.app.features.social.presentation.state.ProfileActionState

@Composable
fun FollowRequestsScreen(
    state: FollowRequestsUiState,
    onAccept: (RequestId) -> Unit,
    onDeny: (RequestId) -> Unit,
    onCancel: (RequestId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.isLoading) CircularProgressIndicator()
        state.errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
        RequestSection(
            title = "Incoming",
            requests = state.incoming,
            actions = { request ->
                Button(onClick = { onAccept(request.id) }) { Text("Accept") }
                OutlinedButton(onClick = { onDeny(request.id) }) { Text("Deny") }
            },
        )
        RequestSection(
            title = "Outgoing",
            requests = state.outgoing,
            actions = { request ->
                OutlinedButton(onClick = { onCancel(request.id) }) { Text("Cancel") }
            },
        )
        when (val action = state.actionState) {
            ProfileActionState.Idle, ProfileActionState.Running -> Unit
            is ProfileActionState.Succeeded -> Text(action.message)
            is ProfileActionState.Failed -> Text(action.message, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun RequestSection(
    title: String,
    requests: List<FollowRequest>,
    actions: @Composable (FollowRequest) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        requests.forEach { request ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "${request.requesterId.value} -> ${request.targetId.value}")
                actions(request)
            }
        }
        if (requests.isEmpty()) {
            Text(text = "No pending requests.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
