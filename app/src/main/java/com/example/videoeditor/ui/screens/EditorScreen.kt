package com.example.videoeditor.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.videoeditor.editor.ColorEffect
import com.example.videoeditor.editor.EditorUiState
import com.example.videoeditor.editor.MusicTrackSpec
import com.example.videoeditor.editor.TextOverlaySpec
import com.example.videoeditor.editor.VideoEditorViewModel

@UnstableApi
@Composable
fun EditorScreen(viewModel: VideoEditorViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val player = remember { ExoPlayer.Builder(context).build() }
    LaunchedEffect(state.sourceUri) {
        state.sourceUri?.let {
            player.setMediaItem(androidx.media3.common.MediaItem.fromUri(it))
            player.prepare()
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    val musicPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.setMusic(MusicTrackSpec(musicUri = it)) }
    }

    var showAddTextDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { PlayerView(it).apply { this.player = player } },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Trim
            Text("Trim", style = MaterialTheme.typography.labelLarge)
            RangeSlider(
                value = state.trimStartMs.toFloat()..state.trimEndMs.coerceAtLeast(state.trimStartMs + 1).toFloat(),
                onValueChange = { range ->
                    viewModel.setTrim(range.start.toLong(), range.endInclusive.toLong())
                },
                valueRange = 0f..state.sourceDurationMs.coerceAtLeast(1).toFloat()
            )

            Spacer(Modifier.height(8.dp))

            // Audio
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Mute original audio", modifier = Modifier.weight(1f))
                Switch(checked = state.muteOriginalAudio, onCheckedChange = { viewModel.setMuteOriginal(it) })
            }
            OutlinedButton(onClick = { musicPicker.launch("audio/*") }, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.music == null) "Add music" else "Music added — tap to change")
            }

            Spacer(Modifier.height(8.dp))

            // Speed / reverse
            Text("Speed: ${"%.2f".format(state.playbackSpeed)}x", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = state.playbackSpeed,
                onValueChange = { viewModel.setSpeed(it) },
                valueRange = 0.25f..4f
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Reverse video", modifier = Modifier.weight(1f))
                Switch(checked = state.reverse, onCheckedChange = { viewModel.setReverse(it) })
            }
            if (state.reverse) {
                Text(
                    "Reverse export isn't implemented yet — see ReverseVideoProcessor.kt for the two build-out paths.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(8.dp))

            // Effects
            Text("Effects", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ColorEffect.entries) { effect ->
                    FilterChip(
                        selected = state.colorEffect == effect,
                        onClick = { viewModel.setColorEffect(effect) },
                        label = { Text(effect.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Text overlay
            OutlinedButton(onClick = { showAddTextDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Add text overlay (${state.textOverlays.size})")
            }

            Spacer(Modifier.height(16.dp))

            // Export
            if (state.isExporting) {
                LinearProgressIndicator(
                    progress = { state.exportProgress },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Exporting... ${(state.exportProgress * 100).toInt()}%")
            } else {
                Button(onClick = { viewModel.export() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Export video")
                }
            }

            state.exportedUri?.let {
                Text("Saved to Movies/VideoEditor", style = MaterialTheme.typography.bodySmall)
            }
            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (showAddTextDialog) {
        AddTextOverlayDialog(
            durationMs = state.sourceDurationMs,
            onDismiss = { showAddTextDialog = false },
            onAdd = { spec ->
                viewModel.addTextOverlay(spec)
                showAddTextDialog = false
            }
        )
    }
}

@Composable
private fun AddTextOverlayDialog(durationMs: Long, onDismiss: () -> Unit, onAdd: (TextOverlaySpec) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add text") },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Text") })
        },
        confirmButton = {
            TextButton(onClick = {
                if (text.isNotBlank()) {
                    onAdd(TextOverlaySpec(text = text, startMs = 0, endMs = durationMs))
                }
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
