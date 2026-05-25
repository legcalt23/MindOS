package com.example.neuv3.features.social.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.neuv3.features.social.domain.model.AudienceRule
import com.example.neuv3.features.social.domain.model.ProfileVisibility
import com.example.neuv3.features.social.presentation.state.PrivacySettingsUiState
import com.example.neuv3.features.social.presentation.state.ProfileActionState

@Composable
fun PrivacySettingsScreen(
    state: PrivacySettingsUiState,
    onProfileVisibilitySelected: (ProfileVisibility) -> Unit,
    onFollowersVisibilitySelected: (AudienceRule) -> Unit,
    onFollowingVisibilitySelected: (AudienceRule) -> Unit,
    onMessagePermissionSelected: (AudienceRule) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = state.settings
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(text = "Privacy", style = MaterialTheme.typography.headlineSmall)
        state.errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
        if (settings != null) {
            EnumSelector(
                title = "Profile",
                selected = settings.profileVisibility,
                values = ProfileVisibility.entries,
                onSelected = onProfileVisibilitySelected,
            )
            EnumSelector(
                title = "Followers",
                selected = settings.followersVisibility,
                values = AudienceRule.entries,
                onSelected = onFollowersVisibilitySelected,
            )
            EnumSelector(
                title = "Following",
                selected = settings.followingVisibility,
                values = AudienceRule.entries,
                onSelected = onFollowingVisibilitySelected,
            )
            EnumSelector(
                title = "Messages",
                selected = settings.messagePermission,
                values = AudienceRule.entries,
                onSelected = onMessagePermissionSelected,
            )
            Button(onClick = onSave) {
                Text("Save")
            }
        }
        when (val action = state.actionState) {
            ProfileActionState.Idle, ProfileActionState.Running -> Unit
            is ProfileActionState.Succeeded -> Text(action.message)
            is ProfileActionState.Failed -> Text(action.message, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun <T : Enum<T>> EnumSelector(
    title: String,
    selected: T,
    values: List<T>,
    onSelected: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            values.forEach { value ->
                if (value == selected) {
                    Button(onClick = { onSelected(value) }) {
                        Text(value.name)
                    }
                } else {
                    OutlinedButton(onClick = { onSelected(value) }) {
                        Text(value.name)
                    }
                }
            }
        }
    }
}
