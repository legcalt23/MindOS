package com.mindos.app.features.social.presentation.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mindos.app.features.social.domain.model.UserProfile
import com.mindos.app.features.social.presentation.state.SocialGraphUiState

@Composable
fun SocialGraphScreen(
    state: SocialGraphUiState,
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
        ProfileListSection(title = "Followers", profiles = state.followers)
        ProfileListSection(title = "Following", profiles = state.following)
    }
}

@Composable
private fun ProfileListSection(
    title: String,
    profiles: List<UserProfile>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        profiles.forEach { profile ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = profile.displayName.value)
                Text(text = profile.handle.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (profiles.isEmpty()) {
            Text(text = "Nothing here yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

