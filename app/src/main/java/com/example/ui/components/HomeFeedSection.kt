package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import java.io.File
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.PlaylistEntity
import com.example.data.model.Artist
import com.example.data.model.AudioQuality
import com.example.data.model.DownloadState
import com.example.data.model.LocalSong
import com.example.data.model.YouTubeTrackResult
import com.example.ui.theme.Accent
import com.example.ui.theme.AccentDim
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgInput
import com.example.ui.theme.BgPrimary
import com.example.ui.theme.BgSecondary
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.HeartPink
import com.example.ui.theme.HoverBorder
import com.example.ui.theme.OfflineGreen
import com.example.ui.theme.OfflineGreenBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Calendar

@Composable
fun HomeFeedSection(
    playlists: List<PlaylistEntity>,
    featuredArtists: List<Artist>,
    recommendedTracks: List<YouTubeTrackResult>,
    localSongs: List<LocalSong>,
    selectedQualities: Map<String, AudioQuality>,
    downloadStates: Map<String, DownloadState>,
    currentPreviewTrack: YouTubeTrackResult?,
    isPreviewPlaying: Boolean,
    favoritesCount: Int = 0,
    recentlyPlayedCount: Int = 0,
    onOpenFavorites: () -> Unit = {},
    onOpenRecentlyPlayed: () -> Unit = {},
    onArtistClick: (Artist) -> Unit,
    onFollowToggle: (Artist) -> Unit,
    onPreviewToggle: (YouTubeTrackResult) -> Unit,
    onDownloadClick: (YouTubeTrackResult) -> Unit,
    onAddToPlaylistClick: (YouTubeTrackResult) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onPlaylistClick: (PlaylistEntity) -> Unit,
    onPlayLocalSong: (LocalSong) -> Unit,
    onNavigateToExplore: (String) -> Unit,
    onNavigateToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_feed_list"),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // 1. Cabecera limpia estilo Spotify
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AUDIOPHILES",
                            color = Accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Inicio",
                            color = TextPrimary,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Botón de acceso rápido a Explorar para buscar/descargar
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = BgCard,
                        border = BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onNavigateToExplore("") }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = Accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Explorar",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // 2. Cartas de las Playlists que tienes (Cuadrícula 2 columnas estilo Spotify)
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tus Playlists",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Transparent,
                        modifier = Modifier.clickable { onCreatePlaylistClick() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Crear",
                                tint = Accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Nueva",
                                color = Accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Fila fija de Playlists Inteligentes (Tus Favoritos & Historial Reciente)
                val favEntity = remember(favoritesCount) {
                    PlaylistEntity(
                        id = -1L,
                        name = "Tus Favoritos",
                        description = "$favoritesCount canciones"
                    )
                }
                val recentsEntity = remember(recentlyPlayedCount) {
                    PlaylistEntity(
                        id = -2L,
                        name = "Historial Reciente",
                        description = "$recentlyPlayedCount pistas"
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Fila 1: Smart Playlists
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SpotifyPlaylistCard(
                            playlist = favEntity,
                            modifier = Modifier.weight(1f),
                            onClick = onOpenFavorites
                        )
                        SpotifyPlaylistCard(
                            playlist = recentsEntity,
                            modifier = Modifier.weight(1f),
                            onClick = onOpenRecentlyPlayed
                        )
                    }

                    // Fila 2+: Playlists personalizadas del usuario o atajos directos
                    if (playlists.isEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Tarjeta para crear la primera playlist
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = BgCard),
                                border = BorderStroke(1.dp, GlassBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(58.dp)
                                    .clickable { onCreatePlaylistClick() }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(58.dp)
                                            .background(
                                                Brush.linearGradient(listOf(Accent, AccentDim))
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlaylistAdd,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.padding(end = 8.dp)) {
                                        Text(
                                            text = "Crear Playlist",
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Tus listas aquí",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            // Tarjeta de Música local
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = BgCard),
                                border = BorderStroke(1.dp, GlassBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(58.dp)
                                    .clickable { onNavigateToLibrary() }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(58.dp)
                                            .background(
                                                Brush.linearGradient(listOf(OfflineGreen, BgSecondary))
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Headphones,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.padding(end = 8.dp)) {
                                        Text(
                                            text = "Música local",
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${localSongs.size} canciones",
                                            color = OfflineGreen,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Playlists creadas por el usuario
                        val displayedPlaylists = playlists.take(6)
                        val rows = displayedPlaylists.chunked(2)
                        rows.forEach { rowPlaylists ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowPlaylists.forEach { playlist ->
                                    SpotifyPlaylistCard(
                                        playlist = playlist,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onPlaylistClick(playlist) }
                                    )
                                }
                                if (rowPlaylists.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Cartas de los Artistas (NO FIJO, se desplaza naturalmente con el scroll del Home)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Tus Artistas",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Toca un artista para ver sus canciones y descargar",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(featuredArtists, key = { it.id }) { artist ->
                        SpotifyArtistCard(
                            artist = artist,
                            onFollowToggle = { onFollowToggle(artist) },
                            onClick = { onArtistClick(artist) }
                        )
                    }
                }
            }
        }

        // 4. Canciones Recomendadas según tus gustos
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Recomendadas para ti",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Seleccionadas con base en tus preferencias musicales",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BgCard,
                        border = BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier.clickable { onNavigateToExplore("") }
                    ) {
                        Text(
                            text = "Ver más",
                            color = Accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Lista de canciones recomendadas con controles de reproducción y descarga directa
        items(recommendedTracks, key = { it.videoId }) { track ->
            val quality = selectedQualities[track.videoId] ?: track.disponiblesQualities.first()
            val downloadState = downloadStates[track.videoId] ?: DownloadState.Idle
            val isCurrentPreview = currentPreviewTrack?.videoId == track.videoId && isPreviewPlaying

            SpotifyRecommendedTrackRow(
                track = track,
                quality = quality,
                downloadState = downloadState,
                isPreviewPlaying = isCurrentPreview,
                onPreviewToggle = { onPreviewToggle(track) },
                onDownloadClick = { onDownloadClick(track) },
                onAddToPlaylistClick = { onAddToPlaylistClick(track) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 5. Escuchado recientemente / Canciones guardadas en tu dispositivo (si existen)
        if (localSongs.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Listas para escuchar sin conexión",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Tus canciones descargadas en alta calidad",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(localSongs.take(8), key = { it.id }) { song ->
                            SpotifyOfflineSongCard(
                                song = song,
                                onPlayClick = { onPlayLocalSong(song) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta rectangular compacta de playlist estilo Spotify (2 por fila)
 */
@Composable
fun SpotifyPlaylistCard(
    playlist: PlaylistEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, GlassBorder),
        modifier = modifier
            .height(58.dp)
            .clickable(onClick = onClick)
            .testTag("home_playlist_card_${playlist.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(
                        when (playlist.id) {
                            -1L -> Brush.linearGradient(listOf(Color(0xFF8E2DE2), Color(0xFFF000FF), HeartPink))
                            -2L -> Brush.linearGradient(listOf(Color(0xFF00C9FF), Color(0xFF92FE9D)))
                            else -> Brush.linearGradient(listOf(Accent, AccentDim))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (playlist.id) {
                    -1L -> Icons.Default.Favorite
                    -2L -> Icons.Default.History
                    else -> Icons.Default.MusicNote
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = playlist.name,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (playlist.description.isNotEmpty()) playlist.description else "Playlist personalizada",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Tarjeta de artista estilo Spotify (circular, NO FIJO, con botón Seguir)
 */
@Composable
fun SpotifyArtistCard(
    artist: Artist,
    onFollowToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, if (artist.isFollowing) HoverBorder else GlassBorder),
        modifier = modifier
            .width(124.dp)
            .clickable(onClick = onClick)
            .testTag("spotify_artist_card_${artist.id}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(BgSecondary)
                    .border(
                        BorderStroke(
                            1.5.dp,
                            if (artist.isFollowing) Accent else GlassBorder
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!artist.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(artist.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = artist.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(listOf(Accent, AccentDim))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = artist.name.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = artist.name,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Text(
                text = artist.followersText,
                fontSize = 10.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (artist.isFollowing) AccentDim else BgSecondary,
                border = BorderStroke(1.dp, if (artist.isFollowing) Accent else GlassBorder),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onFollowToggle() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    if (artist.isFollowing) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    Text(
                        text = if (artist.isFollowing) "Siguiendo" else "Seguir",
                        fontSize = 10.sp,
                        color = if (artist.isFollowing) Accent else TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Fila de canción recomendada en el Home estilo Spotify
 */
@Composable
fun SpotifyRecommendedTrackRow(
    track: YouTubeTrackResult,
    quality: AudioQuality,
    downloadState: DownloadState,
    isPreviewPlaying: Boolean,
    onPreviewToggle: () -> Unit,
    onDownloadClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, if (isPreviewPlaying) HoverBorder else GlassBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("recommended_track_${track.videoId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Portada con overlay de reproducción
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BgSecondary)
                    .clickable { onPreviewToggle() }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(track.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Botón Play / Pause superpuesto
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isPreviewPlaying) Color.Black.copy(alpha = 0.45f)
                            else Color.Black.copy(alpha = 0.25f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPreviewPlaying) "Pausar" else "Reproducir preview",
                        tint = if (isPreviewPlaying) Accent else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Información de la canción
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onPreviewToggle() }
            ) {
                Text(
                    text = track.title,
                    color = if (isPreviewPlaying) Accent else TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = track.channelOrArtist,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = " • ${track.durationText}",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = quality.label,
                    color = Accent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Botón Agregar a Playlist
            IconButton(
                onClick = onAddToPlaylistClick,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlaylistAdd,
                    contentDescription = "Añadir a playlist",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Botón de Descarga directa
            when (downloadState) {
                is DownloadState.Downloading -> {
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { downloadState.progress },
                            color = Accent,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                is DownloadState.Completed -> {
                    Surface(
                        shape = CircleShape,
                        color = OfflineGreenBg,
                        border = BorderStroke(1.dp, OfflineGreen),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Descargada",
                                tint = OfflineGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                else -> {
                    IconButton(
                        onClick = onDownloadClick,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Descargar canción",
                            tint = Accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta de música offline en el Home (1 tap para reproducir)
 */
@Composable
fun SpotifyOfflineSongCard(
    song: LocalSong,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, GlassBorder),
        modifier = modifier
            .width(130.dp)
            .clickable(onClick = onPlayClick)
            .testTag("offline_home_card_${song.id}")
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .size(114.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BgSecondary)
            ) {
                val coverModel = remember(song.albumArtUri) {
                    val art = song.albumArtUri
                    when {
                        art.isNullOrEmpty() -> null
                        art.startsWith("file://") -> File(art.removePrefix("file://"))
                        art.startsWith("/") -> File(art)
                        else -> art
                    }
                }
                if (coverModel != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(coverModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(BgSecondary, BgCard))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Pequeño indicador de reproducción
                Surface(
                    shape = CircleShape,
                    color = Accent,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = song.title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = song.artist,
                color = TextSecondary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
