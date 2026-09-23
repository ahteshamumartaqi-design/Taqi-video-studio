package com.example.videoeditor.processing

import android.content.Context
import android.net.Uri
import androidx.media3.common.Effect
import androidx.media3.common.Effects
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.SpeedChangeEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.TransformationRequest
import com.example.videoeditor.editor.EditorUiState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File

/**
 * Turns an EditorUiState into a Media3 Composition and runs the export.
 * This is the single place that combines: trim, mute, speed, color effects,
 * text/image overlays, and background music into one output file.
 */
@UnstableApi
class VideoProcessor(private val context: Context) {

    sealed class ExportEvent {
        data class Progress(val fraction: Float) : ExportEvent()
        data class Completed(val outputUri: Uri) : ExportEvent()
        data class Failed(val message: String) : ExportEvent()
    }

    fun export(state: EditorUiState, outputFile: File): Flow<ExportEvent> = callbackFlow {
        val sourceUri = state.sourceUri
            ?: run { trySend(ExportEvent.Failed("No source video selected")); close(); return@callbackFlow }

        if (state.reverse) {
            trySend(ExportEvent.Failed("Reverse export isn't wired up yet — see ReverseVideoProcessor.kt"))
            close()
            return@callbackFlow
        }

        val mediaItem = MediaItem.Builder()
            .setUri(sourceUri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(state.trimStartMs)
                    .setEndPositionMs(
                        if (state.trimEndMs > state.trimStartMs) state.trimEndMs
                        else state.sourceDurationMs
                    )
                    .build()
            )
            .build()

        val videoEffects = mutableListOf<Effect>()
        videoEffects += EffectsLibrary.buildEffects(state.colorEffect)
        if (state.playbackSpeed != 1f) {
            videoEffects += SpeedChangeEffect(state.playbackSpeed)
        }

        val overlayBitmaps = mutableListOf<BitmapOverlay>()
        state.textOverlays.forEach { overlayBitmaps += OverlayEffects.buildTextOverlay(context, it) }
        // Image overlays require their bitmaps decoded first — see EditorViewModel for where
        // that decode happens before this export call; passed in via state in a fuller build.
        OverlayEffects.buildCombinedEffect(overlayBitmaps)?.let { videoEffects += it }

        var editedItem = EditedMediaItem.Builder(mediaItem)
            .setRemoveAudio(state.muteOriginalAudio)
            .setEffects(Effects(emptyList(), videoEffects))
            .build()

        if (!state.muteOriginalAudio && state.music != null) {
            editedItem = AudioMixer.withOriginalVolume(editedItem, state.music.originalVolume)
        }

        val videoSequence = EditedMediaItemSequence(listOf(editedItem))
        val sequences = mutableListOf(videoSequence)

        val effectiveDurationMs = ((state.trimEndMs.takeIf { it > state.trimStartMs }
            ?: state.sourceDurationMs) - state.trimStartMs)

        state.music?.let { music ->
            AudioMixer.buildMusicSequence(music, effectiveDurationMs)?.let { sequences += it }
        }

        val composition = Composition.Builder(sequences).build()

        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, result: androidx.media3.transformer.ExportResult) {
                    trySend(ExportEvent.Completed(Uri.fromFile(outputFile)))
                    close()
                }
                override fun onError(
                    composition: Composition,
                    result: androidx.media3.transformer.ExportResult,
                    exception: androidx.media3.transformer.ExportException
                ) {
                    trySend(ExportEvent.Failed(exception.message ?: "Export failed"))
                    close()
                }
            })
            .build()

        transformer.start(composition, outputFile.absolutePath)

        // Poll progress periodically; Transformer doesn't push progress via the listener.
        val progressHolder = androidx.media3.transformer.ProgressHolder()
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val pollRunnable = object : Runnable {
            override fun run() {
                val state = transformer.getProgress(progressHolder)
                if (state != Transformer.PROGRESS_STATE_NOT_STARTED) {
                    trySend(ExportEvent.Progress(progressHolder.progress / 100f))
                }
                if (state != Transformer.PROGRESS_STATE_UNAVAILABLE) {
                    handler.postDelayed(this, 300)
                }
            }
        }
        handler.post(pollRunnable)

        awaitClose {
            handler.removeCallbacks(pollRunnable)
            transformer.cancel()
        }
    }
}
