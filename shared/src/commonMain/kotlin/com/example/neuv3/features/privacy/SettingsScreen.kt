package com.example.neuv3.features.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    privacyState: MutableState<Boolean>,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020504))
            .statusBarsPadding()
            .padding(22.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "CERRAR",
                color = Color(0xFFF4FFF9),
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
                color = Color(0xFFF4FFF9),
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 35.sp, fontWeight = FontWeight.Black),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if (privacyState.value) "PRIVACIDAD: ACTIVA" else "PRIVACIDAD: INACTIVA",
                color = Color(0xFF13ECA4),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.045f))
                    .clickable { privacyState.value = !privacyState.value }
                    .padding(18.dp),
            )
        }
    }
}
