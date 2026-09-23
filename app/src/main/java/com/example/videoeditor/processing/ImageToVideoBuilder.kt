package com.example.videoeditor.processing

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.EditedMediaItem

/**
 * Image-to-video: each image becomes a short clip in the timeline. Media3's Transformer
 * treats a still image MediaItem as a video source when you give it a duration via
 * EditedMediaItem.Builder.setDurationUs(), so no manual frame encoding is needed.
 *
 * Ken Burns (slow pan/zoom) is added with a Scale/Crop ramp: Media3's `ScaleAndRotateTransformation`
 * only supports a single static transform per clip today, so a true animated ramp needs a custom
 * GlShaderProgram that interpolates scale over presentation time. A minimal starting point for that
 * is sketched in KenBurnsEffect.kt — wire it into `extraEffects` below once implemented.
 */
@UnstableApi
object ImageToVideoBuilder {

    data class ImageClip(val uri: Uri, val durationMs: Long = 3000)

    fun buildClip(clip: ImageClip, extraEffects: List<androidx.media3.common.Effect> = emptyList()): EditedMediaItem {
        val mediaItem = MediaItem.Builder()
            .setUri(clip.uri)
            .build()

        return EditedMediaItem.Builder(mediaItem)
            .setDurationUs(clip.durationMs * 1000)
            .setFrameRate(30)
            .setEffects(
                androidx.media3.common.Effects(
                    /* audioProcessors= */ emptyList(),
                    /* videoEffects= */ extraEffects
                )
            )
            .build()
    }

    /** Builds a full slideshow: one EditedMediaItem per image, concatenated by the caller
     * into an EditedMediaItemSequence (see VideoProcessor.buildSlideshowComposition). */
    fun buildSlideshowClips(images: List<ImageClip>): List<EditedMediaItem> =
        images.map { buildClip(it) }
}
