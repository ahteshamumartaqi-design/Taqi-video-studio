package com.example.videoeditor.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onVideoPicked: (android.net.Uri) -> Unit,
    onImageToVideo: () -> Unit,
    onTextToVideo: () -> Unit,
    onSettings: () -> Unit = {},
) {
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(onVideoPicked)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("TAQI VIDEO STUDIO", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Personal-use editor — trim, mute, music, effects, overlays & more",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(32.dp))

        HomeActionCard(
            icon = Icons.Default.VideoLibrary,
            title = "Edit a video",
            subtitle = "Trim, mute, add music, effects, overlays, reverse",
            onClick = { videoPicker.launch("video/*") }
        )
        Spacer(Modifier.height(12.dp))
        HomeActionCard(
            icon = Icons.Default.Collections,
            title = "Image to video",
            subtitle = "Turn photos into a slideshow clip",
            onClick = onImageToVideo
        )
        Spacer(Modifier.height(12.dp))
        HomeActionCard(
            icon = Icons.Default.AutoAwesome,
            title = "Text to video (AI)",
            subtitle = "Needs an external API key — see README",
            onClick = onTextToVideo
        )
        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onSettings) { Text("AI generation settings") }
    }
}

@Composable
private fun HomeActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
