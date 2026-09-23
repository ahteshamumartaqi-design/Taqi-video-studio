package com.example.videoeditor.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.media3.common.Effect
import androidx.media3.common.OverlaySettings
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect as Media3OverlayEffect
import androidx.media3.effect.StaticOverlaySettings
import com.example.videoeditor.editor.ImageOverlaySpec
import com.example.videoeditor.editor.TextOverlaySpec
import com.google.common.collect.ImmutableList

/**
 * Renders text/sticker overlays as bitmaps composited over the video frame.
 * Media3's OverlayEffect draws one or more BitmapOverlays every frame; we build
 * a text bitmap once per overlay spec and position it with StaticOverlaySettings.
 *
 * Note: Media3's overlay API positions overlays for the whole clip rather than
 * natively time-ranged per overlay. For "appear only between startMs..endMs" behavior,
 * TimestampOverlaySettings / per-frame visibility hooks are used — see buildTimedOverlay().
 */
@UnstableApi
object OverlayEffects {

    fun buildTextOverlay(context: Context, spec: TextOverlaySpec): BitmapOverlay {
        val bitmap = renderTextBitmap(spec)
        val settings = StaticOverlaySettings.Builder()
            .setOverlayFrameAnchor(0f, 0f)
            .setBackgroundFrameAnchor(spec.xFraction * 2 - 1, 1 - spec.yFraction * 2)
            .build()
        return object : BitmapOverlay() {
            override fun getBitmap(presentationTimeUs: Long): Bitmap = bitmap
            override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
                // Hide the overlay outside its time window by shrinking it to invisible.
                val ms = presentationTimeUs / 1000
                return if (ms in spec.startMs..spec.endMs) settings
                else StaticOverlaySettings.Builder().setScale(0f, 0f).build()
            }
        }
    }

    fun buildImageOverlay(context: Context, spec: ImageOverlaySpec, sourceBitmap: Bitmap): BitmapOverlay {
        val settings = StaticOverlaySettings.Builder()
            .setScale(spec.scale, spec.scale)
            .setBackgroundFrameAnchor(spec.xFraction * 2 - 1, 1 - spec.yFraction * 2)
            .build()
        return object : BitmapOverlay() {
            override fun getBitmap(presentationTimeUs: Long): Bitmap = sourceBitmap
            override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
                val ms = presentationTimeUs / 1000
                return if (ms in spec.startMs..spec.endMs) settings
                else StaticOverlaySettings.Builder().setScale(0f, 0f).build()
            }
        }
    }

    /** Combines every overlay for the project into one Effect to append to the pipeline. */
    fun buildCombinedEffect(overlays: List<BitmapOverlay>): Effect? {
        if (overlays.isEmpty()) return null
        return Media3OverlayEffect(ImmutableList.copyOf(overlays))
    }

    private fun renderTextBitmap(spec: TextOverlaySpec): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = spec.colorArgb
            textSize = spec.sizeSp * 3f // rough sp->px at export resolution; tune per output size
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            setShadowLayer(6f, 0f, 2f, Color.BLACK)
        }
        val width = (paint.measureText(spec.text) + 40).toInt().coerceAtLeast(1)
        val height = (paint.textSize * 1.6f).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawText(spec.text, 20f, height - paint.descent(), paint)
        return bitmap
    }

    // --- Extension point: background-removed image overlays --------------------------
    // For "overlay with no background" (e.g. cutting a person out of a photo before
    // compositing), run the source bitmap through an on-device segmentation model
    // (Google ML Kit Selfie Segmentation or MediaPipe Image Segmenter) to get an alpha
    // mask, then punch that mask into the bitmap's alpha channel before calling
    // buildImageOverlay(). ML Kit dependency:
    //   implementation("com.google.mlkit:segmentation-selfie:16.0.0-beta6")
    // This is left as a follow-up because it pulls in a ~10-20MB model.
}
