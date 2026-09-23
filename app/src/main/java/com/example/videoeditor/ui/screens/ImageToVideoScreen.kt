package com.example.videoeditor.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.example.videoeditor.processing.ImageToVideoBuilder

@UnstableApi
@Composable
fun ImageToVideoScreen(onBuilt: (List<ImageToVideoBuilder.ImageClip>) -> Unit) {
    var images by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var perImageSeconds by remember { mutableStateOf(3f) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        images = uris
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Image to Video", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Pick photos to turn into a slideshow clip. Each image becomes " +
                "${"%.1f".format(perImageSeconds)}s of video.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(16.dp))

        Button(onClick = { picker.launch("image/*") }) {
            Text(if (images.isEmpty()) "Choose images" else "${images.size} images selected — change")
        }

        Spacer(Modifier.height(16.dp))
        Text("Seconds per image: ${"%.1f".format(perImageSeconds)}")
        Slider(value = perImageSeconds, onValueChange = { perImageSeconds = it }, valueRange = 1f..8f)

        Spacer(Modifier.height(16.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(images) { uri -> Text(uri.lastPathSegment ?: uri.toString(), Modifier.padding(vertical = 4.dp)) }
        }

        Button(
            enabled = images.isNotEmpty(),
            onClick = {
                onBuilt(images.map { ImageToVideoBuilder.ImageClip(it, (perImageSeconds * 1000).toLong()) })
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Build slideshow clip")
        }
    }
}
