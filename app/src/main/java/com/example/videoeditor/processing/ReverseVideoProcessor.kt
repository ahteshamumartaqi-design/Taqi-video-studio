package com.example.videoeditor.processing

/**
 * REVERSE VIDEO — known limitation of the Media3-only approach
 * ---------------------------------------------------------------
 * Media3 Transformer's export pipeline decodes and processes frames strictly forward
 * (it's a streaming decode -> effect -> encode pipeline), so there's no built-in
 * "play backwards" export path the way there is for speed/trim/mute.
 *
 * Two ways to actually get reverse video, in order of effort:
 *
 * 1) FFmpeg (simplest, but reintroduces the dependency you chose to avoid):
 *      add implementation("com.arthenica:ffmpeg-kit-full:6.0-2") [note: ffmpeg-kit is
 *      no longer actively published on Maven Central under that group as of 2025 —
 *      check github.com/arthenica/ffmpeg-kit or a community fork for a current artifact
 *      before depending on it]
 *      then run: -i input.mp4 -vf reverse -af areverse output.mp4
 *
 * 2) Manual frame-buffer reversal with MediaCodec (no extra dependency, more code):
 *      - Decode the whole clip to a sequence of frames (or a temp raw file) using
 *        MediaExtractor + MediaCodec in decode mode.
 *      - Re-encode those frames in reverse order with MediaCodec in encode mode.
 *      - For audio, decode to PCM, reverse the sample buffer, re-encode.
 *      This only works for short clips held in memory/disk buffer — long clips need
 *      chunked processing.
 *
 * This file is a placeholder so the rest of the app (UI, project state) can wire up a
 * "Reverse" toggle now; call out to whichever implementation you pick from
 * VideoProcessor.export() when state.reverse == true.
 */
object ReverseVideoProcessor {
    class NotImplementedYet : Exception(
        "Reverse video needs either FFmpeg or a manual MediaCodec frame-reversal pass — see comments in this file."
    )

    suspend fun reverse(inputPath: String, outputPath: String) {
        throw NotImplementedYet()
    }
}
