package com.example.videoeditor.processing

import androidx.media3.common.Effect
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.Saturation
import androidx.media3.effect.RgbMatrix
import android.opengl.Matrix
import com.example.videoeditor.editor.ColorEffect

/**
 * Turns a ColorEffect enum choice into the concrete Media3 GlEffect chain applied
 * during export/preview. Add new looks here — each is just a small matrix or filter.
 */
object EffectsLibrary {

    fun buildEffects(effect: ColorEffect): List<Effect> = when (effect) {
        ColorEffect.NONE -> emptyList()

        ColorEffect.GRAYSCALE -> listOf(RgbFilter.createGrayscaleFilter())

        ColorEffect.SEPIA -> listOf(sepiaMatrix())

        ColorEffect.WARM -> listOf(
            Saturation(0.15f),
            warmthMatrix(0.12f)
        )

        ColorEffect.COOL -> listOf(
            Saturation(0.05f),
            warmthMatrix(-0.12f)
        )

        ColorEffect.HIGH_CONTRAST -> listOf(
            Contrast(0.35f),
            Saturation(0.2f)
        )

        // True vignette needs a custom shader (radial darkening); left as an extension
        // point — see comment below. For now it falls back to a mild contrast boost.
        ColorEffect.VIGNETTE -> listOf(Contrast(0.15f))
    }

    private fun sepiaMatrix(): RgbMatrix {
        val m = FloatArray(16)
        Matrix.setIdentityM(m, 0)
        // Standard sepia transform coefficients, row-major 4x4 applied to RGBA.
        m[0] = 0.393f; m[1] = 0.349f; m[2] = 0.272f
        m[4] = 0.769f; m[5] = 0.686f; m[6] = 0.534f
        m[8] = 0.189f; m[9] = 0.168f; m[10] = 0.131f
        return RgbMatrix { _, _ -> m }
    }

    private fun warmthMatrix(amount: Float): RgbMatrix {
        val m = FloatArray(16)
        Matrix.setIdentityM(m, 0)
        m[0] = 1f + amount        // boost/cut red
        m[10] = 1f - amount       // cut/boost blue
        return RgbMatrix { _, _ -> m }
    }

    // --- Extension point -----------------------------------------------------------
    // To add a true vignette or a LUT-based filmic look, implement androidx.media3.effect.GlEffect
    // / GlShaderProgram directly. Media3's RgbMatrix/Contrast/Saturation classes above cover most
    // "filter" style looks without writing any GLSL yourself.
}
