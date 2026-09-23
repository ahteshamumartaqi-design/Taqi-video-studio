package com.example.videoeditor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.videoeditor.ai.AiSettingsStore

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { AiSettingsStore(context) }
    var baseUrl by remember { mutableStateOf(store.baseUrl) }
    var apiKey by remember { mutableStateOf(store.apiKey) }
    var saved by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("AI Generation Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Point this at your own Wan 2.1 server (self-hosted — a cloud GPU box, a home " +
                "PC, or ComfyUI with the Wan nodes). Wan 2.1 is open-weight but needs a real " +
                "GPU to run, so it can't live on the phone itself.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("Server URL (e.g. https://your-server:8000)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API key (optional, if your server requires one)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            store.baseUrl = baseUrl
            store.apiKey = apiKey
            saved = true
        }) { Text("Save") }

        if (saved) {
            Spacer(Modifier.height(12.dp))
            Text("Saved.", style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.weight(1f))
        TextButton(onClick = onBack) { Text("Back") }
    }
}
