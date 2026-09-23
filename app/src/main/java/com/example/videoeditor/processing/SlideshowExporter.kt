package com.example.videoeditor.processing

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.example.videoeditor.util.MediaStoreUtil
import java.io.File

/** Minimal export path for the image-to-video slideshow builder (bypasses the single-source
 * EditorUiState pipeline in VideoProcessor, since a slideshow has N sources, not one). */
@UnstableApi
object SlideshowExporter {

    fun export(
        context: Context,
        clips: List<ImageToVideoBuilder.ImageClip>,
        onComplete: (android.net.Uri?) -> Unit,
        onError: (String) -> Unit,
    ) {
        val editedItems = ImageToVideoBuilder.buildSlideshowClips(clips)
        val sequence = EditedMediaItemSequence(editedItems)
        val composition = Composition.Builder(listOf(sequence)).build()

        val outputFile = File(context.cacheDir, "slideshow_${System.currentTimeMillis()}.mp4")

        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, result: ExportResult) {
                    val savedUri = MediaStoreUtil.saveVideoToGallery(context, outputFile, outputFile.name)
                    onComplete(savedUri)
                }
                override fun onError(composition: Composition, result: ExportResult, exception: ExportException) {
                    onError(exception.message ?: "Slideshow export failed")
                }
            })
            .build()

        transformer.start(composition, outputFile.absolutePath)
    }
}
