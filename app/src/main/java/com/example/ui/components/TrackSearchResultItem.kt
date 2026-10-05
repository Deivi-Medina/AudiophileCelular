package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.AudioQuality
import com.example.data.model.DownloadState
import com.example.data.model.YouTubeTrackResult
import com.example.ui.theme.CosmicBackground
import com.example.ui.theme.CosmicBorder
import com.example.ui.theme.CosmicBorderLight
import com.example.ui.theme.CosmicCard
import com.example.ui.theme.CosmicSurface
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GlowNeon
import com.example.ui.theme.PurpleNeon
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TrackSearchResultItem(
    track: YouTubeTrackResult,
    downloadState: DownloadState,
    selectedQuality: AudioQuality,
    onQualitySelected: (AudioQuality) -> Unit,
    isPreviewPlaying: Boolean,
    onPreviewToggle: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isQualityMenuOpen by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("track_result_${track.videoId}"),
        colors = CardDefaults.cardColors(containerColor = CosmicCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (isPreviewPlaying) PurpleNeon else CosmicBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Portada con botón flotante de previsualización (Play/Pause)
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CosmicSurface),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(track.thumbnailUrl.ifEmpty { null })
                        .crossfade(true)
                        .build(),
                    placeholder = painterResource(id = R.drawable.audiophiles_default_cover),
                    error = painterResource(id = R.drawable.audiophiles_default_cover),
                    fallback = painterResource(id = R.drawable.audiophiles_default_cover),
                    contentDescription = "Portada de ${track.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Gradiente oscuro sutil para legibilidad
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0x99000000))
                            )
                        )
                )

                // Botón flotante para previsualización de 15-30s
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isPreviewPlaying) GlowNeon else Color(0xCC0A0A12))
                        .border(
                            BorderStroke(
                                1.5.dp,
                                if (isPreviewPlaying) Color.White else PurpleNeon
                            ),
                            CircleShape
                        )
                        .clickable(onClick = onPreviewToggle)
                        .testTag("preview_button_${track.videoId}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPreviewPlaying) "Pausar previsualización" else "Escuchar previsualización",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Badge de duración en la esquina inferior
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xCC020204),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                ) {
                    Text(
                        text = track.durationText,
                        color = TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Información de la pista
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = track.channelOrArtist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Selector desplegable/badge de calidad
                    Box {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CosmicSurface,
                            border = BorderStroke(1.dp, CosmicBorderLight),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isQualityMenuOpen = true }
                                .testTag("quality_selector_${track.videoId}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${selectedQuality.bitrate} ${selectedQuality.format}",
                                    color = CyanAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Cambiar calidad",
                                    tint = CyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = isQualityMenuOpen,
                            onDismissRequest = { isQualityMenuOpen = false },
                            modifier = Modifier.background(CosmicSurface)
                        ) {
                            track.disponiblesQualities.forEach { quality ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = quality.label,
                                                color = if (quality == selectedQuality) PurpleNeon else TextPrimary,
                                                fontWeight = if (quality == selectedQuality) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "${quality.format} • ${quality.bitrate}",
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        onQualitySelected(quality)
                                        isQualityMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    // Indicador de preview activo
                    AnimatedVisibility(
                        visible = isPreviewPlaying,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(GlowNeon.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = GlowNeon,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Preview",
                                color = GlowNeon,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Cosmic Download Button a la derecha
            CosmicDownloadButton(
                state = downloadState,
                onClick = onDownloadClick
            )
        }
    }
}
