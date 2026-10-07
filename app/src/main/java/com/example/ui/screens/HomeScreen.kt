package com.example.ui.screens

import com.example.data.model.SongRef
import androidx.compose.material.icons.filled.Star
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.FavoriteSongEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistSongEntity
import com.example.data.local.RecentSongEntity
import com.example.data.local.SongReviewEntity
import com.example.data.model.AudioQuality
import com.example.data.model.DownloadState
import com.example.data.model.LocalSong
import com.example.data.model.YouTubeTrackResult
import com.example.ui.components.AddSongsToPlaylistDialog
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.AppBrandHeader
import com.example.ui.components.CosmicFullPlayerSheet
import com.example.ui.components.CosmicMeshBackground
import com.example.ui.components.CosmicMiniPlayer
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.EditPlaylistDialog
import com.example.ui.components.GlassmorphicDownloadToast
import com.example.ui.components.HomeFeedSection
import com.example.ui.components.LocalSongRow
import com.example.ui.components.PlaylistDetailView
import com.example.ui.components.SongReviewDialog
import com.example.ui.components.TrackSearchResultItem
import com.example.ui.theme.Accent
import com.example.ui.theme.AccentDim
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgInput
import com.example.ui.theme.BgPrimary
import com.example.ui.theme.BgSecondary
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.HoverBorder
import com.example.ui.theme.OfflineGreen
import com.example.ui.theme.OfflineGreenBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SearchAndDownloadViewModel

enum class NavigationTab {
    HOME,
    EXPLORE,
    LIBRARY,
    EQUALIZER,
    PROFILE
}

enum class LibrarySubTab {
    SONGS,
    PLAYLISTS
}

@Composable
fun HomeScreen(
    viewModel: SearchAndDownloadViewModel,
    onRequestStoragePermission: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(NavigationTab.HOME) }
    var librarySubTab by remember { mutableStateOf(LibrarySubTab.SONGS) }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var showOnlyOffline by remember { mutableStateOf(false) }

    // Diálogos de Playlists, Reseñas y Borrado de canciones
    var isCreatePlaylistDialogVisible by remember { mutableStateOf(false) }
    var selectedPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
    var editingPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
    var playlistForAddSongs by remember { mutableStateOf<PlaylistEntity?>(null) }
    var trackForPlaylist by remember { mutableStateOf<YouTubeTrackResult?>(null) }
    var localSongForPlaylist by remember { mutableStateOf<LocalSong?>(null) }

    var trackForReview by remember { mutableStateOf<YouTubeTrackResult?>(null) }
    var localSongForReview by remember { mutableStateOf<LocalSong?>(null) }

    var songToDelete by remember { mutableStateOf<LocalSong?>(null) }

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val dialogSearchQuery by viewModel.dialogSearchQuery.collectAsState()
    val dialogSearchResults by viewModel.dialogSearchResults.collectAsState()
    val isDialogSearching by viewModel.isDialogSearching.collectAsState()
    val playbackQueue by viewModel.playbackQueue.collectAsState()
    val recommendedTracks by viewModel.recommendedTracks.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val selectedQualities by viewModel.selectedQualities.collectAsState()
    val downloadStates by viewModel.downloadStates.collectAsState()
    val toastData by viewModel.toastData.collectAsState()

    val featuredArtists by viewModel.featuredArtists.collectAsState()
    val localSongs by viewModel.localSongs.collectAsState()
    val isScanningLocal by viewModel.isScanningLocal.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val listenLater by viewModel.listenLater.collectAsState()

    val currentPreviewTrack by viewModel.currentPreviewTrack.collectAsState()
    val isPreviewPlaying by viewModel.isPreviewPlaying.collectAsState()
    val previewProgress by viewModel.previewProgress.collectAsState()

    val currentLocalSong by viewModel.currentLocalSong.collectAsState()
    val isLocalPlaying by viewModel.isLocalPlaying.collectAsState()
    val localProgress by viewModel.localProgress.collectAsState()

    val isFullPlayerVisible by viewModel.isFullPlayerVisible.collectAsState()
    val isMiniPlayerDismissed by viewModel.isMiniPlayerDismissed.collectAsState()
    val isShuffleActive by viewModel.isShuffleActive.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val activePlaylist by viewModel.activePlaylist.collectAsState()

    val favorites by viewModel.favorites.collectAsState()
    val favoritesCount by viewModel.favoritesCount.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val recentlyPlayedCount by viewModel.recentlyPlayedCount.collectAsState()
    val isCurrentTrackFavorite by viewModel.isCurrentTrackFavorite.collectAsState()

    val categories = listOf("Todos", "Rock", "Electronic", "Hi-Res 320k", "Vinilos", "Clásica")

    val activeDownloadsCount = downloadStates.values.count {
        it is DownloadState.Downloading || it is DownloadState.Queued
    }

    val dismissAllDialogs = {
        if (isFullPlayerVisible) viewModel.closeFullPlayer()
        playlistForAddSongs = null
        selectedPlaylist = null
        editingPlaylist = null
        trackForPlaylist = null
        localSongForPlaylist = null
        trackForReview = null
        localSongForReview = null
        isCreatePlaylistDialogVisible = false
        songToDelete = null
    }

    // Navegación intuitiva con el botón Atrás del sistema (Android BackHandler)
    BackHandler(
        enabled = isFullPlayerVisible ||
                playlistForAddSongs != null ||
                editingPlaylist != null ||
                selectedPlaylist != null ||
                songToDelete != null ||
                trackForPlaylist != null ||
                localSongForPlaylist != null ||
                trackForReview != null ||
                localSongForReview != null ||
                isCreatePlaylistDialogVisible ||
                searchQuery.isNotEmpty() ||
                selectedTab != NavigationTab.HOME
    ) {
        when {
            isFullPlayerVisible -> viewModel.closeFullPlayer()
            playlistForAddSongs != null -> playlistForAddSongs = null
            editingPlaylist != null -> editingPlaylist = null
            selectedPlaylist != null -> selectedPlaylist = null
            songToDelete != null -> songToDelete = null
            trackForPlaylist != null || localSongForPlaylist != null -> {
                trackForPlaylist = null
                localSongForPlaylist = null
            }
            trackForReview != null || localSongForReview != null -> {
                trackForReview = null
                localSongForReview = null
            }
            isCreatePlaylistDialogVisible -> isCreatePlaylistDialogVisible = false
            searchQuery.isNotEmpty() -> viewModel.onSearchQueryChanged("")
            selectedTab != NavigationTab.HOME -> selectedTab = NavigationTab.HOME
        }
    }

    Scaffold(
        containerColor = BgPrimary,
        bottomBar = {
            Column(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (isFullPlayerVisible) viewModel.closeFullPlayer()
                }
            ) {
                // Mini reproductor: NUNCA se muestra si el reproductor completo está expandido
                // o si el usuario le dio al botón de minimizar. SIN botón de cerrar por requerimiento.
                if (!isFullPlayerVisible && !isMiniPlayerDismissed) {
                    if (currentPreviewTrack != null) {
                        CosmicMiniPlayer(
                            visible = true,
                            title = currentPreviewTrack?.title ?: "",
                            artist = currentPreviewTrack?.channelOrArtist ?: "",
                            coverUrl = currentPreviewTrack?.thumbnailUrl,
                            isPlaying = isPreviewPlaying,
                            progress = previewProgress,
                            isStreamPreview = true,
                            onPlayPauseToggle = {
                                if (isPreviewPlaying) viewModel.pausePreview() else viewModel.resumePreview()
                            },
                            onExpand = { viewModel.openFullPlayer() },
                            onMinimize = { viewModel.dismissMiniPlayer() }
                        )
                    } else if (currentLocalSong != null) {
                        CosmicMiniPlayer(
                            visible = true,
                            title = currentLocalSong?.title ?: "",
                            artist = currentLocalSong?.artist ?: "",
                            coverUrl = currentLocalSong?.albumArtUri,
                            isPlaying = isLocalPlaying,
                            progress = localProgress,
                            isStreamPreview = false,
                            onPlayPauseToggle = {
                                if (isLocalPlaying) viewModel.pauseLocalPlayback() else viewModel.resumeLocalPlayback()
                            },
                            onExpand = { viewModel.openFullPlayer() },
                            onMinimize = { viewModel.dismissMiniPlayer() }
                        )
                    }
                }

                // Barra de navegación móvil minimalista: Oculta cuando el reproductor maximizado está abierto
                if (!isFullPlayerVisible) {
                    NavigationBar(
                        containerColor = BgSecondary,
                        contentColor = TextPrimary,
                        tonalElevation = 8.dp,
                        modifier = Modifier.border(1.dp, GlassBorder)
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == NavigationTab.HOME,
                            onClick = {
                                dismissAllDialogs()
                                selectedTab = NavigationTab.HOME
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Inicio"
                                )
                            },
                            label = { Text("Inicio", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Accent,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = Accent
                            )
                        )

                        NavigationBarItem(
                            selected = selectedTab == NavigationTab.EXPLORE,
                            onClick = {
                                dismissAllDialogs()
                                selectedTab = NavigationTab.EXPLORE
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Explorar"
                                )
                            },
                            label = { Text("Explorar", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Accent,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = Accent
                            )
                        )

                        NavigationBarItem(
                            selected = selectedTab == NavigationTab.LIBRARY,
                            onClick = {
                                dismissAllDialogs()
                                selectedTab = NavigationTab.LIBRARY
                                viewModel.refreshLocalMusic()
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.LibraryMusic,
                                    contentDescription = "Biblioteca"
                                )
                            },
                            label = { Text("Biblioteca", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Accent,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = Accent
                            )
                        )

                        NavigationBarItem(
                            selected = selectedTab == NavigationTab.EQUALIZER,
                            onClick = {
                                dismissAllDialogs()
                                selectedTab = NavigationTab.EQUALIZER
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Equalizer,
                                    contentDescription = "Ecualizador"
                                )
                            },
                            label = { Text("Ecualizador", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Accent,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = Accent
                            )
                        )

                        NavigationBarItem(
                            selected = selectedTab == NavigationTab.PROFILE,
                            onClick = {
                                dismissAllDialogs()
                                selectedTab = NavigationTab.PROFILE
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Perfil"
                                )
                            },
                            label = { Text("Perfil", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Accent,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = Accent
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CosmicMeshBackground()

            Column(modifier = Modifier.fillMaxSize()) {
                if (selectedPlaylist != null) {
                    val currentPl = selectedPlaylist!!
                    val roomPlaylistSongs by viewModel.getSongsForPlaylist(currentPl.id).collectAsState(initial = emptyList())
                    val playlistSongs = remember(currentPl.id, roomPlaylistSongs, favorites, recentlyPlayed) {
                        when (currentPl.id) {
                            -1L -> favorites.mapIndexed { idx, it ->
                                PlaylistSongEntity(
                                    id = it.addedAt,
                                    playlistId = -1L,
                                    songId = it.songId,
                                    title = it.title,
                                    artist = it.artist,
                                    coverUrl = it.coverUrl,
                                    audioUrl = it.audioUrl,
                                    durationText = it.durationText,
                                    isLocal = it.isLocal,
                                    orderIndex = idx
                                )
                            }
                            -2L -> recentlyPlayed.mapIndexed { idx, it ->
                                PlaylistSongEntity(
                                    id = it.playedAt,
                                    playlistId = -2L,
                                    songId = it.songId,
                                    title = it.title,
                                    artist = it.artist,
                                    coverUrl = it.coverUrl,
                                    audioUrl = it.audioUrl,
                                    durationText = it.durationText,
                                    isLocal = it.isLocal,
                                    orderIndex = idx
                                )
                            }
                            else -> roomPlaylistSongs
                        }
                    }

                    PlaylistDetailView(
                        playlist = currentPl,
                        songs = playlistSongs,
                        currentPlayingSongId = currentPreviewTrack?.videoId ?: currentLocalSong?.id?.toString(),
                        isPlaying = isPreviewPlaying || isLocalPlaying,
                        onBackClick = { selectedPlaylist = null },
                        onPlaySong = { songEntity, index ->
                            viewModel.playPlaylist(currentPl, playlistSongs, startIndex = index, shuffle = false)
                        },
                        onPlayAll = {
                            viewModel.playPlaylist(currentPl, playlistSongs, startIndex = 0, shuffle = false)
                        },
                        onPlayShuffle = {
                            viewModel.playPlaylist(currentPl, playlistSongs, startIndex = 0, shuffle = true)
                        },
                        onMoveSong = { fromIndex, toIndex ->
                            if (currentPl.id >= 0) {
                                viewModel.moveSongInPlaylist(playlistSongs, fromIndex, toIndex)
                            }
                        },
                        onRemoveSong = { songEntity ->
                            if (currentPl.id == -1L) {
                                viewModel.removeFavorite(songEntity.songId)
                            } else if (currentPl.id >= 0) {
                                viewModel.removeSongFromPlaylist(currentPl.id, songEntity.songId)
                            }
                        },
                        onEditPlaylist = {
                            if (currentPl.id >= 0) editingPlaylist = currentPl
                        },
                        onDeletePlaylist = {
                            if (currentPl.id >= 0) viewModel.deletePlaylist(currentPl)
                            selectedPlaylist = null
                        },
                        onAddSongsClick = {
                            if (currentPl.id >= 0) playlistForAddSongs = currentPl
                        },
                        onPlayNext = { songEntity ->
                            viewModel.playSongEntityNext(songEntity)
                        },
                        onAddToQueue = { songEntity ->
                            viewModel.addSongEntityToQueue(songEntity)
                        },
                        onAddToOtherPlaylist = { songEntity ->
                            val local = localSongs.find { it.id.toString() == songEntity.songId }
                            if (local != null) {
                                localSongForPlaylist = local
                            } else {
                                trackForPlaylist = YouTubeTrackResult(
                                    videoId = songEntity.songId,
                                    title = songEntity.title,
                                    channelOrArtist = songEntity.artist,
                                    durationText = songEntity.durationText,
                                    durationMs = 210_000L,
                                    thumbnailUrl = songEntity.coverUrl ?: "",
                                    previewAudioUrl = songEntity.audioUrl
                                )
                            }
                        },
                        onDownloadSong = { songEntity ->
                            val track = YouTubeTrackResult(
                                videoId = songEntity.songId,
                                title = songEntity.title,
                                channelOrArtist = songEntity.artist,
                                durationText = songEntity.durationText,
                                durationMs = 210_000L,
                                thumbnailUrl = songEntity.coverUrl ?: "",
                                previewAudioUrl = songEntity.audioUrl
                            )
                            viewModel.startDownload(track)
                        },
                        onOpenReviews = { songEntity ->
                            val track = YouTubeTrackResult(
                                videoId = songEntity.songId,
                                title = songEntity.title,
                                channelOrArtist = songEntity.artist,
                                durationText = songEntity.durationText,
                                durationMs = 210_000L,
                                thumbnailUrl = songEntity.coverUrl ?: "",
                                previewAudioUrl = songEntity.audioUrl
                            )
                            trackForReview = track
                        },
                        onGoToArtist = { artistName ->
                            selectedTab = NavigationTab.EXPLORE
                            viewModel.onSearchQueryChanged(artistName)
                        },
                        onClearHistory = {
                            viewModel.clearRecentHistory()
                        }
                    )
                } else {
                    when (selectedTab) {
                        NavigationTab.HOME -> {
                            HomeFeedSection(
                                playlists = playlists,
                                featuredArtists = featuredArtists,
                                recommendedTracks = recommendedTracks,
                                localSongs = localSongs,
                                selectedQualities = selectedQualities,
                                downloadStates = downloadStates,
                                currentPreviewTrack = currentPreviewTrack,
                                isPreviewPlaying = isPreviewPlaying,
                                onArtistClick = { artist ->
                                    selectedTab = NavigationTab.EXPLORE
                                    viewModel.onSearchQueryChanged(artist.name)
                                },
                                onFollowToggle = { artist ->
                                    viewModel.toggleFollowArtist(artist)
                                },
                                onPreviewToggle = { track ->
                                    viewModel.togglePreview(track)
                                },
                                onDownloadClick = { track ->
                                    viewModel.startDownload(track)
                                },
                                onAddToPlaylistClick = { track ->
                                    trackForPlaylist = track
                                },
                                onCreatePlaylistClick = {
                                    isCreatePlaylistDialogVisible = true
                                },
                                onPlaylistClick = { playlist ->
                                    selectedPlaylist = playlist
                                },
                                onPlayLocalSong = { song ->
                                    viewModel.playLocalSong(song)
                                },
                                onNavigateToExplore = { query ->
                                    selectedTab = NavigationTab.EXPLORE
                                    if (query.isNotEmpty()) {
                                        viewModel.onSearchQueryChanged(query)
                                    }
                                },
                                onNavigateToLibrary = {
                                    selectedTab = NavigationTab.LIBRARY
                                    librarySubTab = LibrarySubTab.SONGS
                                }
                            )
                        }

                    NavigationTab.EXPLORE -> {
                        // CABECERA APARTADA: Cabecera con estadísticas en Explorar y Descargar
                        AppBrandHeader(activeDownloadsCount = activeDownloadsCount)

                        // Barra de búsqueda APARTADA de la cabecera
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .height(50.dp)
                                .testTag("search_text_field"),
                            placeholder = {
                                Text(
                                    "Buscar canciones, artistas en YouTube...",
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Buscar",
                                    tint = Accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.onSearchQueryChanged("") },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Limpiar búsqueda",
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

                        // Categorías de exploración en chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEachIndexed { index, category ->
                                val isSelected = selectedCategoryIndex == index
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) Accent else BgCard,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) Accent else GlassBorder
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable {
                                            selectedCategoryIndex = index
                                            val queryToSet = if (index == 0) "" else category
                                            viewModel.onSearchQueryChanged(queryToSet)
                                        }
                                ) {
                                    Text(
                                        text = category,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // Lista de canciones recomendadas o resultados de YouTube Music
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
                                    CircularProgressIndicator(
                                        color = Accent,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Conectando con biblioteca YouTube...",
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            } else if (searchResults.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Sin resultados para \"$searchQuery\"",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Intenta buscar por artista como Queen, Daft Punk, Canserbero, Pink Floyd...",
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    item {
                                        Text(
                                            text = if (searchQuery.isEmpty()) "RECOMENDACIONES BASADAS EN TUS GUSTOS" else "RESULTADOS EN YOUTUBE MUSIC",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextMuted,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }

                                    items(searchResults, key = { it.videoId }) { track ->
                                        val quality = selectedQualities[track.videoId] ?: track.disponiblesQualities.first()
                                        val downloadState = downloadStates[track.videoId] ?: DownloadState.Idle
                                        val isCurrentPreview = currentPreviewTrack?.videoId == track.videoId && isPreviewPlaying

                                        TrackSearchResultItem(
                                            track = track,
                                            selectedQuality = quality,
                                            downloadState = downloadState,
                                            isPreviewPlaying = isCurrentPreview,
                                            isInListenLater = listenLater.any { it.songId == track.videoId },
                                            onToggleListenLater = {
                                                viewModel.toggleListenLater(
                                                    SongRef(
                                                        songId = track.videoId,
                                                        title = track.title,
                                                        artist = track.channelOrArtist,
                                                        coverUrl = track.thumbnailUrl
                                                    )
                                                )
                                            },
                                            onQualitySelected = { q ->
                                                viewModel.setQualityForTrack(track.videoId, q)
                                            },
                                            onDownloadClick = {
                                                viewModel.startDownload(track)
                                            },
                                            onPreviewToggle = {
                                                viewModel.togglePreview(track)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    NavigationTab.LIBRARY -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            // Cabecera propia de la biblioteca
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tu Biblioteca",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                )
                            }

                            // Selector de Sub-Tab: Canciones vs Playlists
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BgSecondary)
                                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                    .padding(4.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (librarySubTab == LibrarySubTab.SONGS) Accent else Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { librarySubTab = LibrarySubTab.SONGS }
                                ) {
                                    Text(
                                        text = "Canciones (${localSongs.size})",
                                        color = if (librarySubTab == LibrarySubTab.SONGS) Color.White else TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (librarySubTab == LibrarySubTab.PLAYLISTS) Accent else Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { librarySubTab = LibrarySubTab.PLAYLISTS }
                                ) {
                                    Text(
                                        text = "Playlists (${playlists.size})",
                                        color = if (librarySubTab == LibrarySubTab.PLAYLISTS) Color.White else TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (librarySubTab == LibrarySubTab.SONGS) {
                                // Barra de acciones: Filtro OFFLINE y Recargar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (showOnlyOffline) OfflineGreenBg else BgCard,
                                        border = BorderStroke(1.dp, if (showOnlyOffline) OfflineGreen else GlassBorder),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .clickable { showOnlyOffline = !showOnlyOffline }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudDownload,
                                                contentDescription = null,
                                                tint = if (showOnlyOffline) OfflineGreen else TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Solo Descargas Audiophile's",
                                                color = if (showOnlyOffline) OfflineGreen else TextSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.refreshLocalMusic()
                                            onRequestStoragePermission()
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Escanear música local",
                                            tint = Accent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                val filteredSongs = if (showOnlyOffline) {
                                    localSongs.filter { it.isDownloadedFromAudiophiles }
                                } else {
                                    localSongs
                                }

                                if (filteredSongs.isEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .padding(24.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = if (showOnlyOffline) "No hay descargas guardadas" else "Biblioteca vacía",
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = if (showOnlyOffline)
                                                "Descarga canciones en formato 320k desde la pestaña Explorar."
                                            else
                                                "Pulsa el botón de escanear o descarga canciones desde Explorar.",
                                            color = TextSecondary,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(filteredSongs, key = { it.id }) { song ->
                                            val isCurrentSongPlaying = currentLocalSong?.id == song.id && isLocalPlaying
                                            LocalSongRow(
                                                song = song,
                                                isPlaying = isCurrentSongPlaying,
                                                onClick = {
                                                    viewModel.playLocalSong(song)
                                                },
                                                onDeleteClick = {
                                                    songToDelete = song
                                                },
                                                onPlayNext = {
                                                    viewModel.playLocalSongNext(song)
                                                },
                                                onAddToQueue = {
                                                    viewModel.addLocalSongToQueue(song)
                                                },
                                                onAddToPlaylist = {
                                                    localSongForPlaylist = song
                                                },
                                                onOpenReviews = {
                                                    localSongForReview = song
                                                },
                                                onGoToArtist = { artistName ->
                                                    selectedTab = NavigationTab.EXPLORE
                                                    viewModel.onSearchQueryChanged(artistName)
                                                }
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Vista de Playlists Propias
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "TUS LISTAS DE REPRODUCCIÓN",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMuted,
                                        letterSpacing = 1.sp
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = AccentDim,
                                        border = BorderStroke(1.dp, Accent),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { isCreatePlaylistDialogVisible = true }
                                            .testTag("create_playlist_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Accent, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Crear Playlist",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (playlists.isEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .padding(24.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Aún no has creado Playlists",
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Crea tus propias listas para organizar tus pistas de YouTube Music y audios locales.",
                                            color = TextSecondary,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(playlists, key = { it.id }) { playlist ->
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = BgSecondary),
                                                border = BorderStroke(1.dp, GlassBorder),
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedPlaylist = playlist }
                                                    .testTag("playlist_card_${playlist.id}")
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(14.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(46.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(AccentDim)
                                                            .border(1.dp, HoverBorder, RoundedCornerShape(10.dp)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                                            contentDescription = null,
                                                            tint = Accent,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(14.dp))

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = playlist.name,
                                                            color = TextPrimary,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 15.sp
                                                        )
                                                        Text(
                                                            text = if (playlist.description.isNotBlank()) playlist.description else "Toca para ver canciones",
                                                            color = TextSecondary,
                                                            fontSize = 12.sp
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = { editingPlaylist = playlist },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "Editar playlist",
                                                            tint = Accent,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(4.dp))

                                                    IconButton(
                                                        onClick = { viewModel.deletePlaylist(playlist) },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Eliminar playlist",
                                                            tint = TextMuted,
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

                    NavigationTab.EQUALIZER -> {
                        EqualizerScreen(
                            viewModel = viewModel,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    NavigationTab.PROFILE -> {
                        ProfileScreen(
                            viewModel = viewModel,
                            downloadedCount = localSongs.count { it.isDownloadedFromAudiophiles },
                            playlistsCount = playlists.size,
                            onOpenSongReviews = { ref ->
                                trackForReview = YouTubeTrackResult(
                                    videoId = ref.songId,
                                    title = ref.title,
                                    channelOrArtist = ref.artist,
                                    durationText = "",
                                    thumbnailUrl = ref.coverUrl ?: ""
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

            // Toast Glassmorphic flotante
            GlassmorphicDownloadToast(
                toastData = toastData,
                onDismiss = { viewModel.dismissToast() },
                onViewInLibrary = {
                    viewModel.dismissToast()
                    selectedTab = NavigationTab.LIBRARY
                    showOnlyOffline = true
                },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Reproductor completo expandido modal
            val fullPlayerTrackTitle = currentPreviewTrack?.title ?: currentLocalSong?.title ?: ""
            val fullPlayerTrackArtist = currentPreviewTrack?.channelOrArtist ?: currentLocalSong?.artist ?: ""
            val fullPlayerCover = currentPreviewTrack?.thumbnailUrl ?: currentLocalSong?.albumArtUri
            val fullPlayerIsPlaying = if (currentPreviewTrack != null) isPreviewPlaying else isLocalPlaying
            val fullPlayerProgress = if (currentPreviewTrack != null) previewProgress else localProgress
            val fullPlayerDuration = currentPreviewTrack?.durationText ?: currentLocalSong?.let { song ->
                val s = song.durationMs / 1000
                "%d:%02d".format(s / 60, s % 60)
            } ?: "0:00"
            val fullPlayerDurationMs = currentPreviewTrack?.durationMs ?: currentLocalSong?.durationMs ?: 0L

            CosmicFullPlayerSheet(
                visible = isFullPlayerVisible,
                title = fullPlayerTrackTitle,
                artist = fullPlayerTrackArtist,
                songId = currentPreviewTrack?.videoId ?: currentLocalSong?.id?.toString(),
                coverUrl = fullPlayerCover,
                isPlaying = fullPlayerIsPlaying,
                progress = fullPlayerProgress,
                durationText = fullPlayerDuration,
                durationMs = fullPlayerDurationMs,
                isStreamPreview = currentPreviewTrack != null,
                playbackQueue = playbackQueue,
                onPlayPauseToggle = {
                    if (currentPreviewTrack != null) {
                        if (isPreviewPlaying) viewModel.pausePreview() else viewModel.resumePreview()
                    } else if (currentLocalSong != null) {
                        if (isLocalPlaying) viewModel.pauseLocalPlayback() else viewModel.resumeLocalPlayback()
                    }
                },
                onSeek = { viewModel.seekTo(it) },
                onClose = { viewModel.closeFullPlayer() },
                onNext = { viewModel.playNext() },
                onPrevious = { viewModel.playPrevious() },
                isShuffleActive = isShuffleActive,
                repeatMode = repeatMode,
                playlistContextName = activePlaylist?.name,
                isFavorite = isCurrentTrackFavorite,
                onToggleFavorite = { viewModel.toggleFavoriteCurrentTrack() },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onToggleRepeat = { viewModel.toggleRepeat() },
                onAddToPlaylist = {
                    if (currentPreviewTrack != null) {
                        trackForPlaylist = currentPreviewTrack
                    } else if (currentLocalSong != null) {
                        localSongForPlaylist = currentLocalSong
                    }
                },
                onOpenReviews = {
                    if (currentPreviewTrack != null) {
                        trackForReview = currentPreviewTrack
                    } else if (currentLocalSong != null) {
                        localSongForReview = currentLocalSong
                    }
                },
                onGoToArtist = { artistName ->
                    viewModel.closeFullPlayer()
                    selectedTab = NavigationTab.EXPLORE
                    viewModel.onSearchQueryChanged(artistName)
                },
                onDownloadTrack = {
                    currentPreviewTrack?.let { viewModel.startDownload(it) }
                },
                onClearQueue = {
                    viewModel.clearQueue()
                },
                onPlayQueueIndex = { idx ->
                    viewModel.playQueueItemAt(idx)
                },
                onReorderQueue = { from, to ->
                    viewModel.reorderQueue(from, to)
                },
                onRemoveQueueIndex = { idx ->
                    viewModel.removeFromQueue(idx)
                }
            )

            // Diálogo Crear Playlist
            CreatePlaylistDialog(
                visible = isCreatePlaylistDialogVisible,
                onDismiss = { isCreatePlaylistDialogVisible = false },
                onCreate = { name, desc ->
                    viewModel.createPlaylist(name, desc)
                }
            )

            // Diálogo Editar Playlist
            EditPlaylistDialog(
                visible = editingPlaylist != null,
                playlist = editingPlaylist,
                onDismiss = { editingPlaylist = null },
                onSave = { newName, newDesc ->
                    editingPlaylist?.let { pl ->
                        viewModel.updatePlaylist(pl.id, newName, newDesc)
                        if (selectedPlaylist?.id == pl.id) {
                            selectedPlaylist = pl.copy(name = newName, description = newDesc)
                        }
                    }
                    editingPlaylist = null
                }
            )

            // Diálogo Añadir a Playlist
            val songToAddToPlaylistTitle = trackForPlaylist?.title ?: localSongForPlaylist?.title ?: ""
            AddToPlaylistDialog(
                visible = trackForPlaylist != null || localSongForPlaylist != null,
                playlists = playlists,
                songTitle = songToAddToPlaylistTitle,
                onDismiss = {
                    trackForPlaylist = null
                    localSongForPlaylist = null
                },
                onSelectPlaylist = { p ->
                    trackForPlaylist?.let { viewModel.addTrackToPlaylist(p.id, it) }
                    localSongForPlaylist?.let { viewModel.addLocalSongToPlaylist(p.id, it) }
                    trackForPlaylist = null
                    localSongForPlaylist = null
                },
                onOpenCreateNew = {
                    isCreatePlaylistDialogVisible = true
                }
            )

            // Diálogo de Búsqueda para Añadir Canciones (desde dentro de la playlist)
            if (playlistForAddSongs != null) {
                val currentPl = playlistForAddSongs!!
                val roomSongs by viewModel.getSongsForPlaylist(currentPl.id).collectAsState(initial = emptyList())
                val existingIds = remember(roomSongs) { roomSongs.map { it.songId }.toSet() }

                AddSongsToPlaylistDialog(
                    visible = true,
                    playlistName = currentPl.name,
                    searchQuery = dialogSearchQuery,
                    onSearchQueryChanged = { viewModel.onDialogSearchQueryChanged(it) },
                    searchResults = dialogSearchResults,
                    isSearching = isDialogSearching,
                    localSongs = localSongs,
                    existingSongIds = existingIds,
                    onDismiss = {
                        playlistForAddSongs = null
                        viewModel.onDialogSearchQueryChanged("")
                    },
                    onAddYouTubeTrack = { track ->
                        viewModel.addTrackToPlaylist(currentPl.id, track)
                    },
                    onAddLocalSong = { localSong ->
                        viewModel.addLocalSongToPlaylist(currentPl.id, localSong)
                    }
                )
            }

            // Diálogo de Reseñas de 5 Estrellas
            val reviewTitle = trackForReview?.title ?: localSongForReview?.title ?: ""
            val reviewArtist = trackForReview?.channelOrArtist ?: localSongForReview?.artist ?: ""
            val reviewSongId = trackForReview?.videoId ?: localSongForReview?.id.toString()
            val reviewCover = trackForReview?.thumbnailUrl?.takeIf { it.isNotBlank() } ?: localSongForReview?.albumArtUri
            val activeSongReviews by remember(reviewSongId) {
                viewModel.getReviewsForSong(reviewSongId)
            }.collectAsState(initial = emptyList())

            SongReviewDialog(
                visible = trackForReview != null || localSongForReview != null,
                songTitle = reviewTitle,
                artist = reviewArtist,
                existingReviews = activeSongReviews,
                isInListenLater = listenLater.any { it.songId == reviewSongId },
                onToggleListenLater = {
                    viewModel.toggleListenLater(SongRef(reviewSongId, reviewTitle, reviewArtist, reviewCover))
                },
                onDismiss = {
                    trackForReview = null
                    localSongForReview = null
                },
                onSubmitReview = { rating, comment ->
                    viewModel.addReview(reviewSongId, reviewTitle, reviewArtist, rating, comment, reviewCover)
                },
                onUpdateReview = { id, rating, comment -> viewModel.updateReview(id, rating, comment) },
                onDeleteReview = { id -> viewModel.deleteReview(id) }
            )

            // Diálogo de confirmación para borrar canción del celular
            if (songToDelete != null) {
                AlertDialog(
                    onDismissRequest = { songToDelete = null },
                    title = {
                        Text(
                            text = "Borrar canción del celular",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    },
                    text = {
                        Text(
                            text = "¿Deseas eliminar permanentemente \"${songToDelete?.title}\" de tu dispositivo y de tu biblioteca?",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                songToDelete?.let { viewModel.deleteLocalSong(it) }
                                songToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                        ) {
                            Text("Borrar del celular", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { songToDelete = null }) {
                            Text("Cancelar", color = TextSecondary)
                        }
                    },
                    containerColor = BgCard,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}
