package com.example.videoeditor.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.videoeditor.ai.AiSettingsStore
import com.example.videoeditor.ai.WanVideoClient
import com.example.videoeditor.util.MediaStoreUtil
import kotlinx.coroutines.launch

@Composable
fun TextToVideoScreen(onGenerated: (Uri) -> Unit, onOpenSettings: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { AiSettingsStore(context) }
    val client = remember { WanVideoClient(settings) }
    val scope = rememberCoroutineScope()

    var prompt by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var statusText by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Text to Video — Wan 2.1", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            if (settings.isConfigured) "Using server: ${settings.baseUrl}"
            else "No server configured yet — set your self-hosted Wan 2.1 endpoint in Settings.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onOpenSettings) { Text("Open AI settings") }

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Describe the video you want") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            enabled = !isGenerating
        )
        Spacer(Modifier.height(16.dp))

        Button(
            enabled = !isGenerating && prompt.isNotBlank() && settings.isConfigured,
            onClick = {
                isGenerating = true
                progress = 0f
                statusText = "Submitting to your Wan 2.1 server…"
                scope.launch {
                    client.generate(
                        prompt = prompt,
                        outputDir = context.cacheDir,
                    ) { event ->
                        when (event) {
                            is WanVideoClient.GenerationEvent.Progress -> {
                                progress = event.fraction
                                statusText = "Generating… ${(event.fraction * 100).toInt()}%"
                            }
                            is WanVideoClient.GenerationEvent.Completed -> {
                                val saved = MediaStoreUtil.saveVideoToGallery(
                                    context, event.localFile, event.localFile.name
                                )
                                isGenerating = false
                                statusText = "Done."
                                (saved ?: Uri.fromFile(event.localFile)).let(onGenerated)
                            }
                            is WanVideoClient.GenerationEvent.Failed -> {
                                isGenerating = false
                                statusText = "Failed: ${event.message}"
                            }
                        }
                    }
                }
            }
        ) {
            Text("Generate")
        }

        if (isGenerating) {
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        }
        statusText?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.weight(1f))
        TextButton(onClick = onBack) { Text("Back") }
    }
}
