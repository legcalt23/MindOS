package com.example.neuv3.features.search.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.neuv3.features.search.domain.UsernameSearchMinQueryLength
import com.example.neuv3.features.search.domain.UsernameSearchUser
import kotlin.math.roundToInt

@Composable
internal fun ZeroUiSearchMode(
    state: UsernameSearchUiState,
    currentUserId: String,
    onQueryChanged: (String) -> Unit,
    onResultSelected: (UsernameSearchUser) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 28.dp),
    ) {
        Text(
            text = "ZERO UI SEARCH",
            color = NeuWhite,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 25.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "Silencio operativo. Mismo motor, menos superficie.",
            color = NeuMist,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp, bottom = 22.dp),
        )

        RadarCommandInput(
            query = state.query,
            onQueryChanged = onQueryChanged,
            helper = inputHelper(state),
            quiet = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(20.dp))

        when {
            !state.canSearch -> ZeroStateMessage(
                "Esperando coordenadas.",
                "$UsernameSearchMinQueryLength caracteres activan la busqueda.",
            )
            state.status is UsernameSearchStatus.Scanning -> ZeroStateMessage("Escaneando.", "Resolviendo identidades compatibles.")
            state.status is UsernameSearchStatus.Empty -> ZeroStateMessage("Sin coincidencias.", "No hay usuarios en esta frecuencia.")
            state.status is UsernameSearchStatus.Failed -> ZeroStateMessage("Busqueda interrumpida.", state.status.message)
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.results, key = { it.userId }) { user ->
                    ZeroUserRow(
                        user = user,
                        selected = state.selectedUserId == user.userId,
                        isCurrentUser = user.userId == currentUserId,
                        onClick = { onResultSelected(user) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ZeroUserRow(
    user: UsernameSearchUser,
    selected: Boolean,
    isCurrentUser: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) NeuGreen.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.035f))
            .border(
                BorderStroke(1.dp, if (selected) NeuGreen.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.08f)),
                RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DetectedAvatar(user = user, modifier = Modifier.size(54.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "@${user.username}",
                color = NeuWhite,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (isCurrentUser) "Tu perfil NEU" else user.displayName,
                color = NeuMist,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = "${(user.matchScore * 100).roundToInt()}",
            color = NeuGreen.copy(alpha = 0.82f),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
        )
    }
}

@Composable
private fun ZeroStateMessage(
    title: String,
    detail: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.035f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.075f)), RoundedCornerShape(8.dp))
            .padding(18.dp),
    ) {
        Text(
            text = title,
            color = NeuWhite,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = detail,
            color = NeuMist,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
