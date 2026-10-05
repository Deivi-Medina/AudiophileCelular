package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.LocalSong
import com.example.data.model.YouTubeTrackResult
import com.example.ui.theme.Accent
import com.example.ui.theme.AccentDim
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgInput
import com.example.ui.theme.BgPrimary
import com.example.ui.theme.BgSecondary
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.OfflineGreen
import com.example.ui.theme.OfflineGreenBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File
import kotlinx.coroutines.delay

enum class AddSongsTab {
    ALL,
    DOWNLOADS,
    YOUTUBE
}

data class DialogSongItem(
    val songId: String,
    val title: String,
    val artist: String,
    val coverUrl: String?,
    val durationText: String,
    val isLocal: Boolean,
    val youtubeTrack: YouTubeTrackResult? = null,
    val localSong: LocalSong? = null
)

@Composable
fun AddSongsToPlaylistDialog(
    visible: Boolean,
    playlistName: String,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    searchResults: List<YouTubeTrackResult>,
    isSearching: Boolean,
    localSongs: List<LocalSong>,
    existingSongIds: Set<String>,
    onDismiss: () -> Unit,
    onAddYouTubeTrack: (YouTubeTrackResult) -> Unit,
    onAddLocalSong: (LocalSong) -> Unit
) {
    if (!visible) return

    var selectedTab by remember { mutableStateOf(AddSongsTab.ALL) }
    var addedCount by remember { mutableIntStateOf(0) }
    val recentlyAddedMap = remember { mutableStateMapOf<String, Boolean>() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .testTag("add_songs_to_playlist_dialog"),
            color = BgPrimary
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Cabecera superior
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Añadir a playlist",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = playlistName,
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentDim,
                                contentColor = Accent
                            ),
                            border = BorderStroke(1.dp, Accent),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text("Listo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Campo de búsqueda
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("dialog_search_field"),
                        placeholder = {
                            Text(
                                "Buscar canciones en local o YouTube...",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Accent,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = BgInput,
                            unfocusedContainerColor = BgCard,
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = Accent
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Pestañas (Tabs): Todos | Descargas | YouTube
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (selectedTab == AddSongsTab.ALL) Accent else BgCard,
                            border = BorderStroke(1.dp, if (selectedTab == AddSongsTab.ALL) Accent else GlassBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { selectedTab = AddSongsTab.ALL }
                        ) {
                            Text(
                                text = "Todos",
                                color = if (selectedTab == AddSongsTab.ALL) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (selectedTab == AddSongsTab.DOWNLOADS) OfflineGreenBg else BgCard,
                            border = BorderStroke(1.dp, if (selectedTab == AddSongsTab.DOWNLOADS) OfflineGreen else GlassBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { selectedTab = AddSongsTab.DOWNLOADS }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = if (selectedTab == AddSongsTab.DOWNLOADS) OfflineGreen else TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Descargas (${localSongs.size})",
                                    color = if (selectedTab == AddSongsTab.DOWNLOADS) OfflineGreen else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (selectedTab == AddSongsTab.YOUTUBE) Accent else BgCard,
                            border = BorderStroke(1.dp, if (selectedTab == AddSongsTab.YOUTUBE) Accent else GlassBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { selectedTab = AddSongsTab.YOUTUBE }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = if (selectedTab == AddSongsTab.YOUTUBE) Color.White else TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "YouTube Music",
                                    color = if (selectedTab == AddSongsTab.YOUTUBE) Color.White else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Preparar lista de ítems según filtro y pestaña activa
                    val itemsToShow = remember(selectedTab, searchQuery, searchResults, localSongs) {
                        val filteredLocals = if (searchQuery.isBlank()) localSongs else {
                            localSongs.filter {
                                it.title.contains(searchQuery, ignoreCase = true) ||
                                it.artist.contains(searchQuery, ignoreCase = true)
                            }
                        }.map { song ->
                            DialogSongItem(
                                songId = song.id.toString(),
                                title = song.title,
                                artist = song.artist,
                                coverUrl = song.albumArtUri,
                                durationText = "3:30",
                                isLocal = true,
                                localSong = song
                            )
                        }

                        val mappedYouTube = searchResults.map { track ->
                            DialogSongItem(
                                songId = track.videoId,
                                title = track.title,
                                artist = track.channelOrArtist,
                                coverUrl = track.thumbnailUrl,
                                durationText = track.durationText,
                                isLocal = false,
                                youtubeTrack = track
                            )
                        }

                        when (selectedTab) {
                            AddSongsTab.ALL -> filteredLocals + mappedYouTube.filter { yt ->
                                filteredLocals.none { loc -> loc.title.equals(yt.title, ignoreCase = true) }
                            }
                            AddSongsTab.DOWNLOADS -> filteredLocals
                            AddSongsTab.YOUTUBE -> mappedYouTube
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (isSearching) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = Accent, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Buscando pistas...", color = TextSecondary, fontSize = 12.sp)
                            }
                        } else if (itemsToShow.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (searchQuery.isBlank()) "Escribe para buscar canciones" else "Sin resultados",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Busca por título o artista en tus descargas locales o en YouTube Music.",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 70.dp)
                            ) {
                                items(itemsToShow, key = { it.songId + "_" + it.isLocal }) { item ->
                                    val isAlreadyInPlaylist = existingSongIds.contains(item.songId)
                                    val isJustAdded = recentlyAddedMap[item.songId] == true

                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isAlreadyInPlaylist || isJustAdded) BgSecondary else BgCard
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isJustAdded) OfflineGreen else if (isAlreadyInPlaylist) GlassBorder else GlassBorder
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = !isAlreadyInPlaylist && !isJustAdded) {
                                                if (item.isLocal && item.localSong != null) {
                                                    onAddLocalSong(item.localSong)
                                                } else if (item.youtubeTrack != null) {
                                                    onAddYouTubeTrack(item.youtubeTrack)
                                                }
                                                recentlyAddedMap[item.songId] = true
                                                addedCount++
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Ícono distintivo de tipo (Verde para local, Morado para YouTube)
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (item.isLocal) OfflineGreenBg else AccentDim),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                val coverUrl = item.coverUrl
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
                                                        contentDescription = null,
                                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = if (item.isLocal) Icons.Default.CloudDownload else Icons.Default.MusicNote,
                                                        contentDescription = null,
                                                        tint = if (item.isLocal) OfflineGreen else Accent,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = item.title,
                                                        color = if (isAlreadyInPlaylist) TextMuted else TextPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    if (item.isLocal) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = OfflineGreenBg
                                                        ) {
                                                            Text(
                                                                text = "OFFLINE",
                                                                color = OfflineGreen,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "${item.artist} • ${item.durationText}",
                                                    color = TextSecondary,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            // Indicador o botón de acción
                                            if (isAlreadyInPlaylist) {
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = BgSecondary
                                                ) {
                                                    Text(
                                                        text = "En playlist",
                                                        color = TextMuted,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            } else if (isJustAdded) {
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = OfflineGreenBg,
                                                    border = BorderStroke(1.dp, OfflineGreen)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = OfflineGreen,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "Añadida ✓",
                                                            color = OfflineGreen,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            } else {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = AccentDim,
                                                    border = BorderStroke(1.dp, Accent),
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Add,
                                                            contentDescription = "Añadir",
                                                            tint = Accent,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Botón flotante inferior cuando hay al menos 1 añadida
                AnimatedVisibility(
                    visible = addedCount > 0,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(28.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                        modifier = Modifier.height(46.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Hecho ($addedCount)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
