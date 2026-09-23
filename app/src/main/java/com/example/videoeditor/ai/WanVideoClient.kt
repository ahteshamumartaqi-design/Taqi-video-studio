package com.example.videoeditor.ai

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Talks to a self-hosted Wan 2.1 inference server. There is no single official Wan 2.1 REST
 * API — you either front the official inference scripts
 * (https://github.com/Wan-Video/Wan2.1) with a small FastAPI/Flask wrapper, or use ComfyUI's
 * API with the community Wan nodes. This client assumes the common "submit job, poll status"
 * shape most people wrap around it:
 *
 *   POST {baseUrl}/generate         -> { "job_id": "..." }
 *   GET  {baseUrl}/status/{job_id}  -> { "status": "queued|running|done|error",
 *                                        "progress": 0.0..1.0,
 *                                        "video_url": "https://.../out.mp4"  (when done) }
 *
 * If your server's contract differs (e.g. ComfyUI's native /prompt + /history endpoints),
 * adjust buildRequestBody() / parseStatus() to match — the polling loop and download step
 * stay the same.
 */
class WanVideoClient(private val settings: AiSettingsStore) {

    sealed class GenerationEvent {
        data class Progress(val fraction: Float) : GenerationEvent()
        data class Completed(val localFile: File) : GenerationEvent()
        data class Failed(val message: String) : GenerationEvent()
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * @param prompt text description
     * @param initImage optional source image for image-to-video (Wan 2.1's I2V-14B models);
     *   null means text-to-video (T2V-1.3B / T2V-14B).
     */
    suspend fun generate(
        prompt: String,
        initImage: Bitmap? = null,
        durationSeconds: Int = 4,
        outputDir: File,
        onEvent: suspend (GenerationEvent) -> Unit,
    ) = withContext(Dispatchers.IO) {
        if (!settings.isConfigured) {
            onEvent(GenerationEvent.Failed("No Wan 2.1 server configured — set it in Settings first."))
            return@withContext
        }

        try {
            val jobId = submitJob(prompt, initImage, durationSeconds)
            pollUntilDone(jobId, outputDir, onEvent)
        } catch (e: Exception) {
            onEvent(GenerationEvent.Failed(e.message ?: "Generation request failed"))
        }
    }

    private fun submitJob(prompt: String, initImage: Bitmap?, durationSeconds: Int): String {
        val body = JSONObject().apply {
            put("prompt", prompt)
            put("duration_seconds", durationSeconds)
            if (initImage != null) put("init_image_b64", bitmapToBase64(initImage))
        }

        val request = Request.Builder()
            .url("${settings.baseUrl}/generate")
            .apply { if (settings.apiKey.isNotBlank()) addHeader("Authorization", "Bearer ${settings.apiKey}") }
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw RuntimeException("Server returned ${response.code}")
            val json = JSONObject(response.body?.string() ?: "{}")
            return json.getString("job_id")
        }
    }

    private suspend fun pollUntilDone(jobId: String, outputDir: File, onEvent: suspend (GenerationEvent) -> Unit) {
        while (true) {
            val request = Request.Builder()
                .url("${settings.baseUrl}/status/$jobId")
                .apply { if (settings.apiKey.isNotBlank()) addHeader("Authorization", "Bearer ${settings.apiKey}") }
                .build()

            val json = http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw RuntimeException("Server returned ${response.code}")
                JSONObject(response.body?.string() ?: "{}")
            }

            when (json.optString("status")) {
                "done" -> {
                    val videoUrl = json.getString("video_url")
                    val file = downloadVideo(videoUrl, outputDir)
                    onEvent(GenerationEvent.Completed(file))
                    return
                }
                "error" -> {
                    onEvent(GenerationEvent.Failed(json.optString("message", "Generation failed on the server")))
                    return
                }
                else -> {
                    onEvent(GenerationEvent.Progress(json.optDouble("progress", 0.0).toFloat()))
                    delay(2000)
                }
            }
        }
    }

    private fun downloadVideo(url: String, outputDir: File): File {
        val request = Request.Builder().url(url).build()
        val outFile = File(outputDir, "wan_${System.currentTimeMillis()}.mp4")
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw RuntimeException("Download failed: ${response.code}")
            FileOutputStream(outFile).use { out ->
                response.body?.byteStream()?.copyTo(out)
            }
        }
        return outFile
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }
}
