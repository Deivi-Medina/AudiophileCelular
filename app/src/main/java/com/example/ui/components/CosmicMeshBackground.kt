package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AccentGlow
import com.example.ui.theme.BgPrimary

@Composable
fun CosmicMeshBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // Fondo base profundo
        drawRect(color = BgPrimary)

        val w = size.width
        val h = size.height

        // Gradiente radial 1: at 15% 15% (rgba(26, 10, 46, 0.9))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xE61A0A2E), Color.Transparent),
                center = Offset(w * 0.15f, h * 0.15f),
                radius = w * 0.65f * pulseScale
            ),
            center = Offset(w * 0.15f, h * 0.15f),
            radius = w * 0.65f * pulseScale
        )

        // Gradiente radial 2: at 85% 85% (rgba(46, 8, 84, 0.5))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x802E0854), Color.Transparent),
                center = Offset(w * 0.85f, h * 0.85f),
                radius = w * 0.75f
            ),
            center = Offset(w * 0.85f, h * 0.85f),
            radius = w * 0.75f
        )

        // Gradiente radial 3: at 50% 50% (accent-glow)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(AccentGlow, Color.Transparent),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = w * 0.85f * pulseScale
            ),
            center = Offset(w * 0.5f, h * 0.5f),
            radius = w * 0.85f * pulseScale
        )
    }
}
