package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DownloadState
import com.example.ui.theme.CosmicBorder
import com.example.ui.theme.CosmicCard
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GlowNeon
import com.example.ui.theme.OfflineGreen
import com.example.ui.theme.PurpleNeon
import com.example.ui.theme.WarningAmber

@Composable
fun CosmicDownloadButton(
    state: DownloadState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cosmic_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_angle"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .testTag("cosmic_download_button")
            .clip(CircleShape)
            .background(CosmicCard)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = PurpleNeon),
                onClick = onClick
            )
    ) {
        when (state) {
            is DownloadState.Downloading -> {
                val progress = state.progress.coerceIn(0f, 1f)
                val sweepAngle = progress * 360f

                // Resplandor difuminado (Glow) usando Modifier.blur y #B5179E
                Canvas(
                    modifier = Modifier
                        .size(44.dp)
                        .blur(5.dp)
                ) {
                    drawArc(
                        color = GlowNeon.copy(alpha = glowAlpha),
                        startAngle = -90f + spinAngle * 0.2f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Aro de progreso radiante con Canvas utilizando #9D4EDD
                Canvas(
                    modifier = Modifier.size(42.dp)
                ) {
                    // Pista de fondo sutil
                    drawCircle(
                        color = CosmicBorder,
                        radius = size.minDimension / 2f,
                        style = Stroke(width = 2.5.dp.toPx())
                    )

                    // Arco de progreso con gradiente radiante
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(PurpleNeon, GlowNeon, PurpleNeon)
                        ),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Indicador de porcentaje en el centro
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            is DownloadState.Queued -> {
                // Indicador de espera en cola pulsante
                Canvas(modifier = Modifier.size(42.dp)) {
                    drawCircle(
                        color = WarningAmber.copy(alpha = glowAlpha),
                        radius = size.minDimension / 2f,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
                Icon(
                    imageVector = Icons.Default.HourglassTop,
                    contentDescription = "En cola de descarga",
                    tint = WarningAmber,
                    modifier = Modifier.size(20.dp)
                )
            }

            is DownloadState.Completed -> {
                // Resplandor de éxito verde #10B981
                Canvas(modifier = Modifier.size(42.dp)) {
                    drawCircle(
                        color = OfflineGreen.copy(alpha = 0.35f),
                        radius = size.minDimension / 2f,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Descarga completada",
                    tint = OfflineGreen,
                    modifier = Modifier.size(22.dp)
                )
            }

            is DownloadState.Error -> {
                Canvas(modifier = Modifier.size(42.dp)) {
                    drawCircle(
                        color = ErrorRed.copy(alpha = 0.5f),
                        radius = size.minDimension / 2f,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Error, reintentar descarga",
                    tint = ErrorRed,
                    modifier = Modifier.size(20.dp)
                )
            }

            is DownloadState.Idle -> {
                // Borde circular sutil para reposo
                Canvas(modifier = Modifier.size(42.dp)) {
                    drawCircle(
                        color = CosmicBorder,
                        radius = size.minDimension / 2f,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = "Descargar canción",
                    tint = PurpleNeon,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
