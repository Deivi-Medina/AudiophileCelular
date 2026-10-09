package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BgPrimary

/**
 * Giro continuo del vinilo, en grados (0..360 en bucle). Se suma al ángulo de
 * apertura para que el disco nunca dé un salto al empezar a girar.
 */
@Composable
fun rememberVinylSpin(turnMillis: Int = 3600): Float {
    val transition = rememberInfiniteTransition(label = "vinylSpin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = turnMillis, easing = LinearEasing)
        ),
        label = "vinylSpinAngle"
    )
    return angle
}

/**
 * Vinilo dibujado con Compose a partir de la portada de la canción: no usa
 * ningún recurso externo.
 *
 * - El disco es negro con surcos concéntricos y una luz que gira con él.
 * - La etiqueta central es la portada (la predeterminada de la app cuando la
 *   canción no trae portada: lo resuelve [StableCoverArt]).
 * - [morph] va de 0 a 1: en 0 el disco está pequeño y apenas visible (la
 *   portada cuadrada manda), en 1 el vinilo está completo.
 * - Los brillos exteriores no giran: son luz fija sobre el disco.
 */
@Composable
fun VinylRecord(
    coverUrl: String?,
    rotationDegrees: Float,
    modifier: Modifier = Modifier,
    morph: Float = 1f,
    labelFraction: Float = 0.38f,
    discScaleAtStart: Float = 0.72f,
    discScaleAtEnd: Float = 0.86f
) {
    val progress = morph.coerceIn(0f, 1f)
    val discScale = discScaleAtStart + (discScaleAtEnd - discScaleAtStart) * progress

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Capa que gira: disco + surcos + etiqueta con la portada.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = rotationDegrees
                    scaleX = discScale
                    scaleY = discScale
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().clip(CircleShape)) {
                val radius = size.minDimension / 2f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF25252E), Color(0xFF111117), Color(0xFF07070B)),
                        center = Offset(size.width * 0.38f, size.height * 0.34f),
                        radius = radius * 1.45f
                    ),
                    radius = radius
                )
                // Surcos
                val grooves = 16
                for (i in 1..grooves) {
                    val fraction = 0.34f + 0.62f * (i.toFloat() / (grooves + 1))
                    drawCircle(
                        color = Color(0x16FFFFFF),
                        radius = radius * fraction,
                        style = Stroke(width = 1.1f)
                    )
                }
                // Franja de luz que gira con el disco: hace visible el giro.
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0x00FFFFFF),
                            Color(0x00FFFFFF),
                            Color(0x1FFFFFFF),
                            Color(0x00FFFFFF)
                        )
                    ),
                    radius = radius * 0.99f,
                    style = Stroke(width = radius * 0.45f)
                )
                drawCircle(color = Color(0x33FFFFFF), radius = radius - 1f, style = Stroke(width = 1.2f))
            }

            // Etiqueta central: la portada de la canción.
            Box(
                modifier = Modifier
                    .fillMaxSize(labelFraction)
                    .clip(CircleShape)
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
            ) {
                StableCoverArt(
                    coverUrl = coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
                // Agujero del eje central.
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(BgPrimary)
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                )
            }
        }

        // Luz fija sobre el vinilo (no gira): da volumen al disco.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = discScale; scaleY = discScale }
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0x00FFFFFF), Color(0x14FFFFFF), Color(0x00FFFFFF)),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    )
                )
        )
    }
}
