package com.example.videoeditor.editor

import android.net.Uri

enum class ColorEffect { NONE, GRAYSCALE, SEPIA, WARM, COOL, HIGH_CONTRAST, VIGNETTE }

data class TextOverlaySpec(
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val xFraction: Float = 0.5f,   // 0..1 across the frame
    val yFraction: Float = 0.85f,
    val colorArgb: Int = 0xFFFFFFFF.toInt(),
    val sizeSp: Float = 42f,
)

data class ImageOverlaySpec(
    val imageUri: Uri,
    val startMs: Long,
    val endMs: Long,
    val xFraction: Float = 0.5f,
    val yFraction: Float = 0.5f,
    val scale: Float = 0.4f,
    val removeBackground: Boolean = false, // see BackgroundRemoval.kt for the on-device path
)

data class MusicTrackSpec(
    val musicUri: Uri,
    val originalVolume: Float = 1f,   // volume of the source video's own audio, 0 = same as "mute original"
    val musicVolume: Float = 1f,
    val loopToFillDuration: Boolean = true,
)

/**
 * A single project's full edit state. VideoProcessor.buildEditedMediaItem() reads this
 * and turns it into a Media3 EditedMediaItem / Composition for export.
 */
data class EditorUiState(
    val sourceUri: Uri? = null,
    val sourceDurationMs: Long = 0,

    // Trim
    val trimStartMs: Long = 0,
    val trimEndMs: Long = 0,

    // Audio
    val muteOriginalAudio: Boolean = false,
    val music: MusicTrackSpec? = null,

    // Speed / reverse
    val playbackSpeed: Float = 1f,      // 0.25x .. 4x, applied via Media3 SpeedChangeEffect
    val reverse: Boolean = false,       // see ReverseVideoProcessor.kt — heavier operation

    // Visuals
    val colorEffect: ColorEffect = ColorEffect.NONE,
    val rotationDegrees: Float = 0f,
    val textOverlays: List<TextOverlaySpec> = emptyList(),
    val imageOverlays: List<ImageOverlaySpec> = emptyList(),

    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val exportedUri: Uri? = null,
    val errorMessage: String? = null,
)
