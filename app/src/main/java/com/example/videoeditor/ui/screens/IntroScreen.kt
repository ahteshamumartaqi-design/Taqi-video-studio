package com.example.videoeditor.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videoeditor.ui.theme.AccentPink
import com.example.videoeditor.ui.theme.AccentPurple
import com.example.videoeditor.ui.theme.BgDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A short, professional-looking brand intro: logo mark scales/fades in, a gradient ring
 * spins behind it, then the wordmark and tagline settle in before handing off to Home.
 * Runs once per cold start (~1.8s total) — this sits on top of the OS-level splash screen
 * that shows for the instant before Compose is ready (wired up in MainActivity via
 * androidx.core.splashscreen).
 */
@Composable
fun IntroScreen(onFinished: () -> Unit) {
    val logoScale = remember { Animatable(0.4f) }
    val logoAlpha = remember { Animatable(0f) }
    val ringRotation = remember { Animatable(0f) }
    val wordmarkAlpha = remember { Animatable(0f) }
    val wordmarkOffset = remember { Animatable(24f) }
    val taglineAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Logo pops in with a slight overshoot for a polished feel.
        logoAlpha.animateTo(1f, tween(350, easing = LinearEasing))
        logoScale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 260f))

        // Ring keeps spinning gently behind the logo for the whole intro (fire-and-forget —
        // must be launched as a child coroutine, since animateTo() with infiniteRepeatable
        // never itself completes and would otherwise block the rest of this sequence).
        launch { launchRingSpin(ringRotation) }

        delay(150)
        wordmarkAlpha.animateTo(1f, tween(400))
        wordmarkOffset.animateTo(0f, tween(400, easing = FastOutSlowInEasing))

        delay(100)
        taglineAlpha.animateTo(1f, tween(350))

        delay(650)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                // Rotating gradient ring behind the mark
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer { rotationZ = ringRotation.value; alpha = logoAlpha.value }
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.sweepGradient(listOf(AccentPurple, AccentPink, AccentPurple))
                        )
                )
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(BgDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(48.dp)
                            .graphicsLayer {
                                scaleX = logoScale.value; scaleY = logoScale.value
                                alpha = logoAlpha.value
                            }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "TAQI VIDEO STUDIO",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = wordmarkAlpha.value
                    translationY = wordmarkOffset.value
                }
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Edit anything. Anywhere.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                modifier = Modifier.graphicsLayer { alpha = taglineAlpha.value }
            )
        }
    }
}

private suspend fun launchRingSpin(rotation: Animatable<Float, AnimationVector1D>) {
    rotation.animateTo(
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
}
