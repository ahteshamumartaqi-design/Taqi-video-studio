package com.example.videoeditor.processing

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.ChannelMixingAudioProcessor
import androidx.media3.common.audio.ChannelMixingMatrix
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import com.example.videoeditor.editor.MusicTrackSpec

/**
 * Mixes a music track with (or in place of) a video's original audio.
 *
 * Media3 Transformer mixes multiple audio EditedMediaItemSequences together when they are
 * added to the same Composition as parallel sequences (isLooping=true keeps the music
 * looping to cover the whole video length automatically).
 */
@UnstableApi
object AudioMixer {

    /**
     * Returns the audio sequence to add alongside the video sequence, or null if there's
     * no separate music track (i.e. only the original audio, or fully muted, is used).
     */
    fun buildMusicSequence(music: MusicTrackSpec, videoDurationMs: Long): EditedMediaItemSequence? {
        val musicItem = MediaItem.Builder().setUri(music.musicUri).build()

        val volumeProcessor = ChannelMixingAudioProcessor().apply {
            // Scale all channels by musicVolume; simple uniform gain matrix.
            putChannelMixingMatrix(
                ChannelMixingMatrix.create(2, 2).scaleBy(music.musicVolume)
            )
        }

        val editedMusicItem = EditedMediaItem.Builder(musicItem)
            .setDurationUs(videoDurationMs * 1000)
            .build()

        return EditedMediaItemSequence(
            listOf(editedMusicItem),
            /* isLooping= */ music.loopToFillDuration
        )
    }

    /**
     * Applies a flat volume scale to the *original* video's audio track (e.g. duck it to
     * 20% so music sits on top, or 0f which is equivalent to muteOriginalAudio).
     */
    fun withOriginalVolume(item: EditedMediaItem, volume: Float): EditedMediaItem {
        if (volume >= 0.999f) return item
        val processor = ChannelMixingAudioProcessor().apply {
            putChannelMixingMatrix(ChannelMixingMatrix.create(2, 2).scaleBy(volume))
        }
        return item.buildUpon()
            .setRemoveAudio(volume <= 0f)
            .build()
    }
}

/** Small helper: scales every coefficient of an identity mixing matrix uniformly. */
private fun ChannelMixingMatrix.scaleBy(gain: Float): ChannelMixingMatrix {
    val channels = outputChannelCount
    val values = FloatArray(inputChannelCount * channels)
    for (i in 0 until minOf(inputChannelCount, channels)) {
        values[i * channels + i] = gain
    }
    return ChannelMixingMatrix(inputChannelCount, channels, values)
}
