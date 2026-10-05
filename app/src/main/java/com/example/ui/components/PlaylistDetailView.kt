package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistSongEntity
import com.example.data.network.YouTubeAudioEngine
import com.example.ui.theme.Accent
import com.example.ui.theme.AccentDim
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgPrimary
import com.example.ui.theme.BgSecondary
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderStrong
import com.example.ui.theme.HeartPink
import com.example.ui.theme.OfflineGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailView(
    playlist: PlaylistEntity,
    songs: List<PlaylistSongEntity>,
    currentPlayingSongId: String? = null,
    isPlaying: Boolean = false,
    onBackClick: () -> Unit,
    onPlaySong: (PlaylistSongEntity, Int) -> Unit,
    onPlayAll: () -> Unit,
    onPlayShuffle: () -> Unit,
    onMoveSong: (fromIndex: Int, toIndex: Int) -> Unit,
    onRemoveSong: (PlaylistSongEntity) -> Unit,
    onEditPlaylist: () -> Unit,
    onDeletePlaylist: () -> Unit,
    onAddSongsClick: (() -> Unit)? = null,
    onPlayNext: ((PlaylistSongEntity) -> Unit)? = null,
    onAddToQueue: ((PlaylistSongEntity) -> Unit)? = null,
    onAddToOtherPlaylist: ((PlaylistSongEntity) -> Unit)? = null,
    onDownloadSong: ((PlaylistSongEntity) -> Unit)? = null,
    onOpenReviews: ((PlaylistSongEntity) -> Unit)? = null,
    onGoToArtist: ((String) -> Unit)? = null,
    onClearHistory: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isReorderMode by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var selectedSongForMenu by remember { mutableStateOf<PlaylistSongEntity?>(null) }

    // Calcular duración total estimada de la playlist
    val totalDurationText = remember(songs) {
        val totalMs = songs.sumOf { YouTubeAudioEngine.parseDurationToMillis(it.durationText) }
        val totalMins = totalMs / 1000 / 60
        val totalSecs = (totalMs / 1000) % 60
        if (totalMins >= 60) {
            val hours = totalMins / 60
            val remMins = totalMins % 60
            "${hours} h ${remMins} min"
        } else {
            "${totalMins} min ${totalSecs} s"
        }
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Vaciar Historial", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("¿Deseas eliminar todo tu historial de reproducción reciente?", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearHistoryDialog = false
                        onClearHistory?.invoke()
                    }
                ) {
                    Text("Vaciar", color = Color(0xFFFF453A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = BgCard
        )
    }

    // Modal Bottom Sheet de opciones pulido estilo Spotify / Apple Music
    if (selectedSongForMenu != null) {
        val song = selectedSongForMenu!!
        ModalBottomSheet(
            onDismissRequest = { selectedSongForMenu = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = BgSecondary,
            scrimColor = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp)
            ) {
                // Indicador de arrastre (Handle Bar)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(TextMuted.copy(alpha = 0.4f))
                    )
                }

                // Cabecera de la pista seleccionada
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BgCard)
                            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val coverUrl = song.coverUrl
                        if (!coverUrl.isNullOrEmpty()) {
                            val context = LocalContext.current
                            val modelData: Any = remember(coverUrl) {
                                if (coverUrl.startsWith("file://")) File(coverUrl.removePrefix("file://"))
                                else if (!coverUrl.startsWith("http") && !coverUrl.startsWith("content")) File(coverUrl)
                                else coverUrl
                            }
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(modelData).crossfade(true).build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = Accent, modifier = Modifier.size(24.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = song.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = song.artist, color = TextSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                HorizontalDivider(color = GlassBorderStrong)
                Spacer(modifier = Modifier.height(10.dp))

                // Opciones del menú estilizadas en tarjetas
                data class SheetMenuOption(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val tint: Color, val action: () -> Unit)
                val menuOptions = listOf(
                    SheetMenuOption("Reproducir ahora", Icons.Default.PlayArrow, Accent) {
                        val idx = songs.indexOf(song)
                        selectedSongForMenu = null
                        onPlaySong(song, if (idx >= 0) idx else 0)
                    },
                    SheetMenuOption("Reproducir a continuación", Icons.AutoMirrored.Filled.QueueMusic, Accent) {
                        selectedSongForMenu = null
                        onPlayNext?.invoke(song)
                    },
                    SheetMenuOption("Añadir a la cola", Icons.AutoMirrored.Filled.QueueMusic, Accent) {
                        selectedSongForMenu = null
                        onAddToQueue?.invoke(song)
                    },
                    SheetMenuOption("Añadir a otra playlist", Icons.Default.Add, Accent) {
                        selectedSongForMenu = null
                        onAddToOtherPlaylist?.invoke(song)
                    },
                    SheetMenuOption(if (song.isLocal) "✓ Canción descargada" else "Descargar canción (320 kbps)", Icons.Default.CloudDownload, if (song.isLocal) OfflineGreen else Accent) {
                        selectedSongForMenu = null
                        onDownloadSong?.invoke(song)
                    },
                    SheetMenuOption("Reseñas y valoración (5★)", Icons.Default.Star, Color(0xFFFFB800)) {
                        selectedSongForMenu = null
                        onOpenReviews?.invoke(song)
                    },
                    SheetMenuOption("Ir al artista (${song.artist})", Icons.Default.Person, Accent) {
                        selectedSongForMenu = null
                        onGoToArtist?.invoke(song.artist)
                    }
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    menuOptions.forEach { option ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BgCard),
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = option.action)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(AccentDim),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = option.icon, contentDescription = null, tint = option.tint, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(text = option.label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = GlassBorderStrong)
                Spacer(modifier = Modifier.height(10.dp))

                // Eliminar de playlist (en rojo)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    border = BorderStroke(1.dp, Color(0xFFFF453A).copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedSongForMenu = null
                            onRemoveSong(song)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF453A).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF453A), modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = if (playlist.id == -1L) "Quitar de favoritos" else "Eliminar de esta playlist",
                            color = Color(0xFFFF453A),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("playlist_detail_view")
    ) {
        // Barra superior con botón Volver y acciones
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BgCard)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = TextPrimary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (playlist.id >= 0 && onAddSongsClick != null) {
                    IconButton(
                        onClick = onAddSongsClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AccentDim)
                            .border(1.dp, Accent, CircleShape)
                            .testTag("playlist_add_songs_top_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Añadir canciones",
                            tint = Accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Solo las playlists creadas por el usuario permiten reordenamiento manual
                if (songs.size > 1 && playlist.id >= 0) {
                    IconButton(
                        onClick = { isReorderMode = !isReorderMode },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isReorderMode) Accent.copy(alpha = 0.2f) else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = if (isReorderMode) Icons.Default.Check else Icons.Default.SwapVert,
                            contentDescription = if (isReorderMode) "Listo" else "Reordenar canciones",
                            tint = if (isReorderMode) Accent else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Botón para vaciar historial reciente
                if (playlist.id == -2L && songs.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearHistoryDialog = true },
                        modifier = Modifier.size(36.dp).testTag("clear_history_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Vaciar historial",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (playlist.id >= 0) {
                    IconButton(
                        onClick = onEditPlaylist,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar playlist",
                            tint = Accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDeletePlaylist,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar playlist",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Cabecera grande con datos de la Playlist
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Carátula de la Playlist
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                when (playlist.id) {
                                    -1L -> Brush.linearGradient(listOf(Color(0xFF8E2DE2), Color(0xFFF000FF), HeartPink))
                                    -2L -> Brush.linearGradient(listOf(Color(0xFF00C9FF), Color(0xFF92FE9D)))
                                    else -> Brush.linearGradient(listOf(Accent, AccentDim))
                                }
                            )
                            .border(1.dp, GlassBorder, RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val firstCover = songs.firstOrNull()?.coverUrl
                        if (!firstCover.isNullOrEmpty()) {
                            val context = LocalContext.current
                            val modelData: Any = remember(firstCover) {
                                if (firstCover.startsWith("file://")) File(firstCover.removePrefix("file://"))
                                else if (!firstCover.startsWith("http") && !firstCover.startsWith("content")) File(firstCover)
                                else firstCover
                            }
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(modelData)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = playlist.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val icon = when (playlist.id) {
                                -1L -> Icons.Default.Favorite
                                -2L -> Icons.Default.History
                                else -> Icons.Default.MusicNote
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = playlist.name,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (playlist.description.isNotBlank()) playlist.description else "Playlist personalizada",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (songs.isNotEmpty()) "${songs.size} canciones • $totalDurationText" else "0 canciones",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (playlist.id >= 0 && onAddSongsClick != null) {
                            Button(
                                onClick = onAddSongsClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentDim,
                                    contentColor = Accent
                                ),
                                border = BorderStroke(1.dp, Accent),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Añadir", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        if (songs.isNotEmpty()) {
                            Button(
                                onClick = onPlayAll,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Accent,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reproducir Todo", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Button(
                                onClick = onPlayShuffle,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BgCard,
                                    contentColor = TextPrimary
                                ),
                                border = BorderStroke(1.dp, GlassBorder),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Shuffle, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Aleatorio", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isReorderMode,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = "Usa las flechas ▲ ▼ para ordenar las canciones de tu playlist",
                            color = Accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            if (songs.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val emptyIcon = when (playlist.id) {
                            -1L -> Icons.Default.Favorite
                            -2L -> Icons.Default.History
                            else -> Icons.Default.MusicNote
                        }
                        val iconTint = if (playlist.id == -1L) HeartPink else Accent
                        val emptyTitle = when (playlist.id) {
                            -1L -> "Aún no tienes canciones favoritas"
                            -2L -> "Aún no has reproducido pistas"
                            else -> "Esta playlist está vacía"
                        }
                        val emptyDesc = when (playlist.id) {
                            -1L -> "Toca el corazón en el reproductor para guardar tus pistas preferidas aquí."
                            -2L -> "Las pistas que escuches en streaming o local aparecerán automáticamente aquí."
                            else -> "Toca el botón '+' para buscar y añadir canciones directamente a esta playlist."
                        }

                        Surface(
                            shape = CircleShape,
                            color = BgCard,
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier.size(60.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = emptyIcon,
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = emptyTitle,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = emptyDesc,
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 24.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        if (playlist.id >= 0 && onAddSongsClick != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onAddSongsClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Añadir canciones ahora", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                    val isCurrent = (currentPlayingSongId == song.songId)
                    var isPressed by remember { mutableStateOf(false) }

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                                onAddToQueue?.invoke(song)
                                true
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromEndToStart = false,
                        backgroundContent = {
                            val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                                OfflineGreen
                            } else {
                                Color.Transparent
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                // Solo visible mientras se desliza: si no, se transparenta tras tarjetas translúcidas (seleccionada/actual).
                                if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Añadir a la cola", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        },
                        content = {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        isPressed -> Accent.copy(alpha = 0.25f)
                                        isCurrent -> Accent.copy(alpha = 0.12f)
                                        else -> BgCard
                                    }
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    when {
                                        isPressed -> Accent
                                        isCurrent -> Accent
                                        else -> GlassBorder
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                isPressed = true
                                                tryAwaitRelease()
                                                isPressed = false
                                            },
                                            onTap = { onPlaySong(song, index) },
                                            onLongPress = {
                                                isPressed = false
                                                selectedSongForMenu = song
                                            }
                                        )
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Número de orden o indicador activo
                                    if (isCurrent) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = "Sonando",
                                            tint = Accent,
                                            modifier = Modifier
                                                .width(24.dp)
                                                .size(18.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "${index + 1}",
                                            color = TextMuted,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(24.dp)
                                        )
                                    }

                                    // Portada de la canción
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(BgSecondary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val coverUrl = song.coverUrl
                                        if (!coverUrl.isNullOrEmpty()) {
                                            val context = LocalContext.current
                                            val modelData: Any = remember(coverUrl) {
                                                if (coverUrl.startsWith("file://")) File(coverUrl.removePrefix("file://"))
                                                else if (!coverUrl.startsWith("http") && !coverUrl.startsWith("content")) File(coverUrl)
                                                else coverUrl
                                            }
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(modelData)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = song.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = Accent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Título y artista
                                    val displayArtist = when {
                                        song.artist.isNotBlank() && !song.artist.equals("YouTube Music", ignoreCase = true) -> song.artist
                                        song.title.contains(" - ") -> song.title.substringBefore(" - ").trim()
                                        song.title.contains(" – ") -> song.title.substringBefore(" – ").trim()
                                        else -> "Artista Audiophile's"
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            color = if (isCurrent) Accent else TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "$displayArtist • ${song.durationText}",
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Controles según modo reordenar o modo normal
                                    if (isReorderMode) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { onMoveSong(index, index - 1) },
                                                enabled = index > 0,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowUp,
                                                    contentDescription = "Mover arriba",
                                                    tint = if (index > 0) Accent else TextMuted.copy(alpha = 0.3f),
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { onMoveSong(index, index + 1) },
                                                enabled = index < songs.lastIndex,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Mover abajo",
                                                    tint = if (index < songs.lastIndex) Accent else TextMuted.copy(alpha = 0.3f),
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        // Botón 3 Puntos estilo Spotify
                                        IconButton(
                                            onClick = { selectedSongForMenu = song },
                                            modifier = Modifier.size(32.dp).testTag("song_row_menu_${song.songId}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "Opciones de canción",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
