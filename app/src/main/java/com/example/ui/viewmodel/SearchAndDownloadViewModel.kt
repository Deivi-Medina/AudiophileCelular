package com.example.ui.viewmodel

import com.example.data.local.ListenLaterEntity
import com.example.data.local.PinnedFavoriteEntity
import com.example.data.model.SongRef
import android.app.Application
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.audio.AudioEffectEngine
import com.example.data.audio.AudioSynthesizer
import com.example.data.audio.AudiophilesMediaSessionManager
import com.example.data.audio.EqualizerState
import com.example.data.local.FavoriteSongEntity
import com.example.data.local.LocalMusicScanner
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistSongEntity
import com.example.data.local.RecentSongEntity
import com.example.data.local.SongReviewEntity
import com.example.data.local.UserProfile
import com.example.data.model.AudioQuality
import com.example.data.model.DownloadState
import com.example.data.model.LocalSong
import com.example.data.model.PlaybackQueue
import com.example.data.model.PlaybackQueueItem
import com.example.data.model.RepeatMode
import com.example.data.model.YouTubeTrackResult
import com.example.data.model.toQueueItem
import com.example.data.network.YouTubeAudioEngine
import com.example.data.repository.MusicRepository
import com.example.ui.components.ToastData
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchAndDownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private val audioEffectEngine = AudioEffectEngine()
    private val TAG = "SearchAndDownloadVM"

    // Media Session y Controles del Sistema Android
    private val mediaSessionManager = AudiophilesMediaSessionManager(
        context = application,
        onPlayAction = {
            if (_currentPreviewTrack.value != null) resumePreview()
            else if (_currentLocalSong.value != null) resumeLocalPlayback()
        },
        onPauseAction = {
            if (_currentPreviewTrack.value != null) pausePreview()
            else if (_currentLocalSong.value != null) pauseLocalPlayback()
        },
        onNextAction = { playNext() },
        onPrevAction = { playPrevious() },
        onSeekAction = { posMs ->
            val duration = previewMediaPlayer?.duration ?: localMediaPlayer?.duration ?: 1
            if (duration > 0) {
                seekTo(posMs.toFloat() / duration)
            }
        }
    )

    // Búsqueda
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<YouTubeTrackResult>>(emptyList())
    val searchResults: StateFlow<List<YouTubeTrackResult>> = _searchResults.asStateFlow()

    // Búsqueda aislada para diálogo de añadir canciones
    private val _dialogSearchQuery = MutableStateFlow("")
    val dialogSearchQuery: StateFlow<String> = _dialogSearchQuery.asStateFlow()

    private val _dialogSearchResults = MutableStateFlow<List<YouTubeTrackResult>>(emptyList())
    val dialogSearchResults: StateFlow<List<YouTubeTrackResult>> = _dialogSearchResults.asStateFlow()

    private val _isDialogSearching = MutableStateFlow(false)
    val isDialogSearching: StateFlow<Boolean> = _isDialogSearching.asStateFlow()

    private var dialogSearchJob: Job? = null

    private val _recommendedTracks = MutableStateFlow<List<YouTubeTrackResult>>(emptyList())
    val recommendedTracks: StateFlow<List<YouTubeTrackResult>> = _recommendedTracks.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _selectedQualities = MutableStateFlow<Map<String, AudioQuality>>(emptyMap())
    val selectedQualities: StateFlow<Map<String, AudioQuality>> = _selectedQualities.asStateFlow()

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    private val _toastData = MutableStateFlow(ToastData(title = "", quality = "", visible = false))
    val toastData: StateFlow<ToastData> = _toastData.asStateFlow()

    private val _localSongs = MutableStateFlow<List<LocalSong>>(emptyList())
    val localSongs: StateFlow<List<LocalSong>> = _localSongs.asStateFlow()

    private val _isScanningLocal = MutableStateFlow(false)
    val isScanningLocal: StateFlow<Boolean> = _isScanningLocal.asStateFlow()

    private val _featuredArtists = MutableStateFlow<List<com.example.data.model.Artist>>(emptyList())
    val featuredArtists: StateFlow<List<com.example.data.model.Artist>> = _featuredArtists.asStateFlow()

    private val _isFullPlayerVisible = MutableStateFlow(false)
    val isFullPlayerVisible: StateFlow<Boolean> = _isFullPlayerVisible.asStateFlow()

    private val _isMiniPlayerDismissed = MutableStateFlow(false)
    val isMiniPlayerDismissed: StateFlow<Boolean> = _isMiniPlayerDismissed.asStateFlow()

    // Reproductor de Previsualización
    private var previewMediaPlayer: MediaPlayer? = null
    private var previewProgressJob: Job? = null

    private val _currentPreviewTrack = MutableStateFlow<YouTubeTrackResult?>(null)
    val currentPreviewTrack: StateFlow<YouTubeTrackResult?> = _currentPreviewTrack.asStateFlow()

    private val _isPreviewPlaying = MutableStateFlow(false)
    val isPreviewPlaying: StateFlow<Boolean> = _isPreviewPlaying.asStateFlow()

    private val _previewProgress = MutableStateFlow(0f)
    val previewProgress: StateFlow<Float> = _previewProgress.asStateFlow()

    // Reproductor Local
    private var localMediaPlayer: MediaPlayer? = null
    private var localProgressJob: Job? = null

    private val _currentLocalSong = MutableStateFlow<LocalSong?>(null)
    val currentLocalSong: StateFlow<LocalSong?> = _currentLocalSong.asStateFlow()

    private val _isLocalPlaying = MutableStateFlow(false)
    val isLocalPlaying: StateFlow<Boolean> = _isLocalPlaying.asStateFlow()

    private val _localProgress = MutableStateFlow(0f)
    val localProgress: StateFlow<Float> = _localProgress.asStateFlow()

    // Ecualizador
    val equalizerState: StateFlow<EqualizerState> = audioEffectEngine.state

    // Playlists
    val playlists: StateFlow<List<PlaylistEntity>> = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Perfil
    val userProfile: StateFlow<UserProfile> = repository.getUserProfileManager().profile

    // ─── COLA DE REPRODUCCIÓN ─────────────────────────────────
    private val _playbackQueue = MutableStateFlow(PlaybackQueue.EMPTY)
    val playbackQueue: StateFlow<PlaybackQueue> = _playbackQueue.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()
    val isShuffleActive: StateFlow<Boolean> = shuffleEnabled

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    val favorites: StateFlow<List<FavoriteSongEntity>> = repository.getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoritesCount: StateFlow<Int> = repository.getFavoritesCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentlyPlayed: StateFlow<List<RecentSongEntity>> = repository.getRecentlyPlayed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayedCount: StateFlow<Int> = repository.getRecentlyPlayedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _activePlaylist = MutableStateFlow<PlaylistEntity?>(null)
    val activePlaylist: StateFlow<PlaylistEntity?> = _activePlaylist.asStateFlow()

    val isCurrentTrackFavorite: StateFlow<Boolean> = combine(
        currentPreviewTrack,
        currentLocalSong,
        favorites
    ) { preview, local, favList ->
        val currentSongId = preview?.videoId ?: local?.id?.toString()
        if (currentSongId != null) {
            favList.any { it.songId == currentSongId }
        } else {
            false
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var searchJob: Job? = null

    init {
        _featuredArtists.value = repository.getFeaturedArtists()
        loadRecommendedTracks()
        performSearch("")
        refreshLocalMusic()
        listenToDatabaseChanges()
    }

    private fun loadRecommendedTracks() {
        viewModelScope.launch {
            try {
                val recs = repository.searchTracks("")
                _recommendedTracks.value = recs
                val currentMap = _selectedQualities.value.toMutableMap()
                recs.forEach { track ->
                    if (!currentMap.containsKey(track.videoId)) {
                        currentMap[track.videoId] = track.disponiblesQualities.first()
                    }
                }
                _selectedQualities.value = currentMap
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando canciones recomendadas: ${e.message}", e)
            }
        }
    }

    private fun listenToDatabaseChanges() {
        viewModelScope.launch {
            repository.getDownloadedSongs().collectLatest { entities ->
                val updatedStates = _downloadStates.value.toMutableMap()
                entities.forEach { entity ->
                    if (updatedStates[entity.videoId] !is DownloadState.Downloading) {
                        updatedStates[entity.videoId] = DownloadState.Completed
                    }
                }
                _downloadStates.value = updatedStates
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(250)
            performSearch(newQuery)
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            try {
                val results = repository.searchTracks(query)
                _searchResults.value = results

                val currentMap = _selectedQualities.value.toMutableMap()
                results.forEach { track ->
                    if (!currentMap.containsKey(track.videoId)) {
                        currentMap[track.videoId] = track.disponiblesQualities.first()
                    }
                }
                _selectedQualities.value = currentMap
            } catch (e: Exception) {
                Log.e(TAG, "Error realizando búsqueda: ${e.message}", e)
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun onDialogSearchQueryChanged(newQuery: String) {
        _dialogSearchQuery.value = newQuery
        dialogSearchJob?.cancel()
        dialogSearchJob = viewModelScope.launch {
            delay(250)
            performDialogSearch(newQuery)
        }
    }

    private fun performDialogSearch(query: String) {
        viewModelScope.launch {
            _isDialogSearching.value = true
            try {
                val results = repository.searchTracks(query)
                _dialogSearchResults.value = results
            } catch (e: Exception) {
                Log.e(TAG, "Error realizando búsqueda aislada para diálogo: ${e.message}", e)
            } finally {
                _isDialogSearching.value = false
            }
        }
    }

    fun setQualityForTrack(videoId: String, quality: AudioQuality) {
        _selectedQualities.update { it + (videoId to quality) }
    }

    fun togglePreview(track: YouTubeTrackResult) {
        _isMiniPlayerDismissed.value = false
        // Reproducción suelta → limpiar cola
        _playbackQueue.value = PlaybackQueue.EMPTY
        stopLocalPlayback()

        val isCurrent = _currentPreviewTrack.value?.let { current ->
            current.videoId == track.videoId || current.title.equals(track.title, ignoreCase = true)
        } ?: false

        if (isCurrent && _isPreviewPlaying.value) {
            pausePreview()
            return
        }

        if (isCurrent && !_isPreviewPlaying.value) {
            resumePreview()
            return
        }

        startPreview(track)
    }

    private fun startPreview(track: YouTubeTrackResult) {
        releasePreviewPlayer()
        _currentPreviewTrack.value = track
        _isPreviewPlaying.value = true
        _previewProgress.value = 0f

        viewModelScope.launch {
            repository.recordPlayed(
                RecentSongEntity(
                    songId = track.videoId,
                    title = track.title,
                    artist = track.channelOrArtist,
                    coverUrl = track.thumbnailUrl,
                    audioUrl = track.previewAudioUrl,
                    durationText = track.durationText,
                    isLocal = false
                )
            )
        }

        mediaSessionManager.requestAudioFocus()
        val realPreviewDur = if (track.durationMs > 0) track.durationMs else 210000L
        mediaSessionManager.updateMetadata(
            title = track.title,
            artist = track.channelOrArtist,
            durationMs = realPreviewDur,
            coverUrl = track.thumbnailUrl
        )

        val previewUrl = track.previewAudioUrl
        if (!previewUrl.isNullOrEmpty() && (previewUrl.startsWith("http://") || previewUrl.startsWith("https://"))) {
            playStreamUrl(previewUrl)
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                val (itunesUrl, itunesMs, itunesDur) = YouTubeAudioEngine.resolveRealAudioStream(track.channelOrArtist, track.title)
                val resolvedUrl = if (!itunesUrl.isNullOrEmpty()) {
                    itunesUrl
                } else {
                    YouTubeAudioEngine.resolveDownloadUrl(track.videoId, "320")
                }
                withContext(Dispatchers.Main) {
                    if (_currentPreviewTrack.value?.videoId == track.videoId) {
                        if (!resolvedUrl.isNullOrEmpty()) {
                            if (itunesMs > 0) {
                                _currentPreviewTrack.update { it?.copy(durationMs = itunesMs, durationText = itunesDur) }
                            }
                            playStreamUrl(resolvedUrl)
                        } else {
                            Log.w(TAG, "No se pudo obtener stream para preview de ${track.title}")
                            _isPreviewPlaying.value = false
                        }
                    }
                }
            }
        }
    }

    private fun playStreamUrl(streamUrl: String) {
        try {
            releasePreviewPlayer()
            val mp = MediaPlayer()
            previewMediaPlayer = mp
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            mp.setDataSource(streamUrl)
            mp.setOnPreparedListener { player ->
                player.start()
                _isPreviewPlaying.value = true
                audioEffectEngine.attachSession(player.audioSessionId)
                updateMasterVolume()
                mediaSessionManager.updatePlaybackState(playing = true, positionMs = 0)
                startPreviewProgressTracking(player)
            }
            mp.setOnCompletionListener {
                _isPreviewPlaying.value = false
                previewProgressJob?.cancel()
                mediaSessionManager.updatePlaybackState(playing = false, positionMs = 0)
                handleTrackFinished()
            }
            mp.setOnErrorListener { _, what, extra ->
                Log.w(TAG, "Error en stream preview MediaPlayer ($what, $extra)")
                _isPreviewPlaying.value = false
                previewProgressJob?.cancel()
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            Log.e(TAG, "Error configurando stream preview: ${e.message}")
            _isPreviewPlaying.value = false
        }
    }

    private fun startPreviewProgressTracking(mp: MediaPlayer) {
        previewProgressJob?.cancel()
        previewProgressJob = viewModelScope.launch {
            while (isActive && _isPreviewPlaying.value) {
                try {
                    val duration = mp.duration.coerceAtLeast(1)
                    val current = mp.currentPosition
                    if (duration > 0 && current >= 0) {
                        _previewProgress.value = (current.toFloat() / duration).coerceIn(0f, 1f)
                        mediaSessionManager.updatePlaybackState(playing = true, positionMs = current.toLong())
                    }
                } catch (_: Exception) {}
                delay(150)
            }
        }
    }

    fun pausePreview() {
        try {
            if (previewMediaPlayer?.isPlaying == true) {
                previewMediaPlayer?.pause()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausando preview: ${e.message}")
        }
        _isPreviewPlaying.value = false
        previewProgressJob?.cancel()
        val pos = try { previewMediaPlayer?.currentPosition ?: 0 } catch (_: Exception) { 0 }
        mediaSessionManager.updatePlaybackState(playing = false, positionMs = pos.toLong())
    }

    fun resumePreview() {
        val currentTrack = _currentPreviewTrack.value ?: return
        val mp = previewMediaPlayer

        if (_previewProgress.value >= 0.95f) {
            startPreview(currentTrack)
            return
        }

        if (mp != null) {
            try {
                mp.start()
                _isPreviewPlaying.value = true
                mediaSessionManager.requestAudioFocus()

                try {
                    audioEffectEngine.attachSession(mp.audioSessionId)
                    updateMasterVolume()
                } catch (_: Exception) {}

                mediaSessionManager.updatePlaybackState(playing = true, positionMs = mp.currentPosition.toLong())
                startPreviewProgressTracking(mp)
                return
            } catch (e: Exception) {
                Log.e(TAG, "Error reanudando preview con mp.start(): ${e.message}")
            }
        }
        startPreview(currentTrack)
    }

    fun stopPreview() {
        releasePreviewPlayer()
        _currentPreviewTrack.value = null
        _isPreviewPlaying.value = false
        _previewProgress.value = 0f
        mediaSessionManager.dismissNotification()
    }

    private fun releasePreviewPlayer() {
        previewProgressJob?.cancel()
        previewProgressJob = null
        try {
            previewMediaPlayer?.stop()
            previewMediaPlayer?.release()
        } catch (_: Exception) {}
        previewMediaPlayer = null
        mediaSessionManager.abandonAudioFocus()
    }

    fun startDownload(track: YouTubeTrackResult) {
        val currentState = _downloadStates.value[track.videoId]
        if (currentState is DownloadState.Downloading) return

        val quality = _selectedQualities.value[track.videoId] ?: track.disponiblesQualities.first()

        _downloadStates.update { it + (track.videoId to DownloadState.Downloading(0.05f)) }

        viewModelScope.launch {
            try {
                val savedSong = repository.downloadTrackDirectly(track, quality) { progress ->
                    _downloadStates.update { it + (track.videoId to DownloadState.Downloading(progress)) }
                }

                _localSongs.update { currentList ->
                    val withoutDupes = currentList.filter { it.title != savedSong.title }
                    listOf(savedSong) + withoutDupes
                }

                _downloadStates.update { it + (track.videoId to DownloadState.Completed) }

                _toastData.value = ToastData(
                    title = track.title,
                    quality = "${quality.bitrate} ${quality.format}",
                    artist = track.channelOrArtist,
                    visible = true
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error en descarga directa: ${e.message}", e)
                _downloadStates.update { it + (track.videoId to DownloadState.Completed) }
                refreshLocalMusic()
            }
        }
    }

    fun dismissToast() {
        _toastData.update { it.copy(visible = false) }
    }

    fun refreshLocalMusic() {
        viewModelScope.launch {
            _isScanningLocal.value = true
            try {
                val songs = repository.getLocalMusicList()
                _localSongs.value = songs
            } catch (e: Exception) {
                Log.e(TAG, "Error refrescando música local: ${e.message}", e)
            } finally {
                _isScanningLocal.value = false
            }
        }
    }

    fun playLocalSong(song: LocalSong) {
        _isMiniPlayerDismissed.value = false
        // Reproducción suelta → limpiar cola
        _playbackQueue.value = PlaybackQueue.EMPTY
        stopPreview()

        if (_currentLocalSong.value?.id == song.id && _isLocalPlaying.value) {
            pauseLocalPlayback()
            return
        }

        if (_currentLocalSong.value?.id == song.id && !_isLocalPlaying.value) {
            resumeLocalPlayback()
            return
        }

        startLocalPlayback(song, seekPositionMs = 0)
    }

    private fun startLocalPlayback(song: LocalSong, seekPositionMs: Long = 0) {
        releaseLocalPlayer()
        _currentLocalSong.value = song
        _isLocalPlaying.value = true
        _localProgress.value = if (song.durationMs > 0) (seekPositionMs.toFloat() / song.durationMs).coerceIn(0f, 1f) else 0f

        viewModelScope.launch {
            repository.recordPlayed(
                RecentSongEntity(
                    songId = song.id.toString(),
                    title = song.title,
                    artist = song.artist,
                    coverUrl = song.albumArtUri,
                    audioUrl = song.path,
                    durationText = "3:30",
                    isLocal = true
                )
            )
        }

        mediaSessionManager.requestAudioFocus()
        mediaSessionManager.updateMetadata(
            title = song.title,
            artist = song.artist,
            durationMs = song.durationMs,
            coverUrl = song.albumArtUri
        )

        try {
            localMediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                var configured = false
                val path = song.path
                if (path.startsWith("content://")) {
                    try {
                        setDataSource(getApplication(), Uri.parse(path))
                        configured = true
                    } catch (e: Exception) {
                        Log.w(TAG, "Uri setDataSource fallo: ${e.message}")
                    }
                }
                if (!configured && path.isNotEmpty()) {
                    val cleanPath = if (path.startsWith("file://")) path.removePrefix("file://") else path
                    val file = File(cleanPath)
                    if (file.exists() && file.length() > 0) {
                        try {
                            setDataSource(file.absolutePath)
                            configured = true
                        } catch (e: Exception) {
                            Log.w(TAG, "File absolutePath setDataSource fallo: ${e.message}")
                        }
                    }
                }
                if (!configured) {
                    val synth = getOrCreateSynthWavFile()
                    setDataSource(synth.absolutePath)
                }

                setOnPreparedListener { mp ->
                    if (seekPositionMs > 0 && seekPositionMs < mp.duration) {
                        mp.seekTo(seekPositionMs.toInt())
                    }
                    val actualDur = mp.duration.toLong()
                    if (actualDur > 0 && _currentLocalSong.value?.durationMs != actualDur) {
                        _currentLocalSong.update { it?.copy(durationMs = actualDur) }
                    }
                    mp.start()
                    _isLocalPlaying.value = true
                    audioEffectEngine.attachSession(mp.audioSessionId)
                    updateMasterVolume()
                    mediaSessionManager.updatePlaybackState(playing = true, positionMs = seekPositionMs)
                    startLocalProgressTracking(mp)
                }

                setOnCompletionListener {
                    _isLocalPlaying.value = false
                    localProgressJob?.cancel()
                    mediaSessionManager.updatePlaybackState(playing = false, positionMs = 0)
                    handleTrackFinished()
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Error en local MediaPlayer ($what, $extra), recuperando con sintetizador")
                    playWithSynthesizerFallback(song)
                    true
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando reproducción local: ${e.message}", e)
            playWithSynthesizerFallback(song)
        }
    }

    private fun playWithSynthesizerFallback(song: LocalSong) {
        releaseLocalPlayer()
        try {
            val synth = getOrCreateSynthWavFile()
            localMediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(synth.absolutePath)
                setOnPreparedListener { mp ->
                    mp.start()
                    _isLocalPlaying.value = true
                    audioEffectEngine.attachSession(mp.audioSessionId)
                    updateMasterVolume()
                    mediaSessionManager.updatePlaybackState(playing = true, positionMs = 0)
                    startLocalProgressTracking(mp)
                }
                setOnCompletionListener {
                    _isLocalPlaying.value = false
                    localProgressJob?.cancel()
                    mediaSessionManager.updatePlaybackState(playing = false, positionMs = 0)
                    handleTrackFinished()
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallo total en fallback sintetizador: ${e.message}")
        }
    }

    private fun startLocalProgressTracking(mp: MediaPlayer) {
        localProgressJob?.cancel()
        localProgressJob = viewModelScope.launch {
            while (isActive && _isLocalPlaying.value) {
                try {
                    val duration = mp.duration.coerceAtLeast(1)
                    val current = mp.currentPosition
                    if (duration > 0 && current >= 0) {
                        _localProgress.value = (current.toFloat() / duration).coerceIn(0f, 1f)
                        mediaSessionManager.updatePlaybackState(playing = true, positionMs = current.toLong())
                    }
                } catch (_: Exception) {}
                delay(150)
            }
        }
    }

    fun pauseLocalPlayback() {
        try {
            if (localMediaPlayer?.isPlaying == true) {
                localMediaPlayer?.pause()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausando local player: ${e.message}")
        }
        _isLocalPlaying.value = false
        localProgressJob?.cancel()

        val pos = try { localMediaPlayer?.currentPosition ?: 0 } catch (_: Exception) { 0 }
        val dur = try { localMediaPlayer?.duration ?: 1 } catch (_: Exception) { 1 }
        if (dur > 0) {
            _localProgress.value = (pos.toFloat() / dur).coerceIn(0f, 1f)
        }

        mediaSessionManager.updatePlaybackState(playing = false, positionMs = pos.toLong())
    }

    fun resumeLocalPlayback() {
        val currentSong = _currentLocalSong.value ?: return
        val mp = localMediaPlayer

        if (_localProgress.value >= 0.95f) {
            startLocalPlayback(currentSong, seekPositionMs = 0)
            return
        }

        val targetMs = if (currentSong.durationMs > 0) {
            (_localProgress.value * currentSong.durationMs).toLong().coerceIn(0L, currentSong.durationMs)
        } else {
            0L
        }

        if (mp != null) {
            try {
                mp.start()
                _isLocalPlaying.value = true
                mediaSessionManager.requestAudioFocus()

                try {
                    audioEffectEngine.attachSession(mp.audioSessionId)
                    updateMasterVolume()
                } catch (_: Exception) {}

                mediaSessionManager.updatePlaybackState(playing = true, positionMs = mp.currentPosition.toLong())
                startLocalProgressTracking(mp)
                return
            } catch (e: Exception) {
                Log.e(TAG, "Error reanudando con mp.start(): ${e.message}. Recreando MediaPlayer...")
            }
        }

        Log.d(TAG, "Recreando MediaPlayer para reanudar en ${targetMs}ms")
        startLocalPlayback(currentSong, seekPositionMs = targetMs)
    }

    fun stopLocalPlayback() {
        releaseLocalPlayer()
        _currentLocalSong.value = null
        _isLocalPlaying.value = false
        _localProgress.value = 0f
        mediaSessionManager.dismissNotification()
    }

    private fun releaseLocalPlayer() {
        localProgressJob?.cancel()
        localProgressJob = null
        try {
            localMediaPlayer?.stop()
            localMediaPlayer?.release()
        } catch (_: Exception) {}
        localMediaPlayer = null
        mediaSessionManager.abandonAudioFocus()
    }

    fun deleteLocalSong(song: LocalSong) {
        viewModelScope.launch {
            if (_currentLocalSong.value?.id == song.id || _currentLocalSong.value?.title == song.title) {
                stopLocalPlayback()
            }
            LocalMusicScanner.markSongAsDeleted(getApplication(), song.path, song.title)
            repository.deleteLocalSong(song)
            _localSongs.update { current ->
                current.filter { it.id != song.id && it.title != song.title && it.path != song.path }
            }
            _downloadStates.update { it - (song.id.toString()) }
            refreshLocalMusic()
        }
    }

    fun toggleFollowArtist(artist: com.example.data.model.Artist) {
        _featuredArtists.update { list ->
            list.map { if (it.id == artist.id) it.copy(isFollowing = !it.isFollowing) else it }
        }
    }

    fun openFullPlayer() { _isFullPlayerVisible.value = true }
    fun closeFullPlayer() { _isFullPlayerVisible.value = false }
    fun dismissMiniPlayer() { _isMiniPlayerDismissed.value = true }
    fun restoreMiniPlayer() { _isMiniPlayerDismissed.value = false }

    fun seekTo(progress: Float) {
        val clamped = progress.coerceIn(0f, 1f)
        if (_currentPreviewTrack.value != null) {
            _previewProgress.value = clamped
            previewMediaPlayer?.let {
                val targetMs = (clamped * it.duration).toInt()
                it.seekTo(targetMs)
                mediaSessionManager.updatePlaybackState(playing = _isPreviewPlaying.value, positionMs = targetMs.toLong())
            }
        } else if (_currentLocalSong.value != null) {
            _localProgress.value = clamped
            localMediaPlayer?.let {
                val targetMs = (clamped * it.duration).toInt()
                it.seekTo(targetMs)
                mediaSessionManager.updatePlaybackState(playing = _isLocalPlaying.value, positionMs = targetMs.toLong())
            }
        }
    }

    fun playNext(forceSkip: Boolean = false) {
        val queue = _playbackQueue.value
        if (queue.isActive) {
            handleQueueAdvance(forward = true, forceSkip = forceSkip)
            return
        }
        // Fallback sin cola
        if (_currentPreviewTrack.value != null) {
            val results = _searchResults.value
            if (results.isNotEmpty()) {
                val i = results.indexOfFirst { it.videoId == _currentPreviewTrack.value?.videoId }
                val next = if (i in 0 until results.lastIndex) i + 1 else 0
                togglePreview(results[next])
            }
        } else if (_currentLocalSong.value != null) {
            val songs = _localSongs.value
            if (songs.isNotEmpty()) {
                val i = songs.indexOfFirst { it.id == _currentLocalSong.value?.id }
                val next = if (i in 0 until songs.lastIndex) i + 1 else 0
                playLocalSong(songs[next])
            }
        }
    }

    fun playPrevious() {
        val queue = _playbackQueue.value
        if (queue.isActive) {
            handleQueueAdvance(forward = false, forceSkip = true)
            return
        }
        if (_currentPreviewTrack.value != null) {
            val results = _searchResults.value
            if (results.isNotEmpty()) {
                val i = results.indexOfFirst { it.videoId == _currentPreviewTrack.value?.videoId }
                val prev = if (i > 0) i - 1 else results.lastIndex
                togglePreview(results[prev])
            }
        } else if (_currentLocalSong.value != null) {
            val songs = _localSongs.value
            if (songs.isNotEmpty()) {
                val i = songs.indexOfFirst { it.id == _currentLocalSong.value?.id }
                val prev = if (i > 0) i - 1 else songs.lastIndex
                playLocalSong(songs[prev])
            }
        }
    }

    private fun handleQueueAdvance(forward: Boolean, forceSkip: Boolean) {
        val q = _playbackQueue.value
        if (q.items.isEmpty()) return

        // Repetir una sola pista (solo si no es skip manual)
        if (!forceSkip && _repeatMode.value == RepeatMode.ONE) {
            playQueueItemAt(q.currentIndex)
            return
        }

        val next = if (forward) q.currentIndex + 1 else q.currentIndex - 1

        when {
            next in q.items.indices -> playQueueItemAt(next)
            forward && _repeatMode.value == RepeatMode.ALL -> playQueueItemAt(0)
            !forward && _repeatMode.value == RepeatMode.ALL -> playQueueItemAt(q.items.lastIndex)
            else -> {
                // Borde de la lista sin loop: no se salta a otra canción ni se
                // pierde la actual, queda pausada en 0:00 lista para reanudar.
                pauseAtTrackStart()
            }
        }
    }

    /**
     * Fin natural de una pista (el audio llegó al final; no es un salto manual).
     *
     * Con loop activo se repite la misma canción o se da la vuelta a la lista,
     * y con canciones pendientes se pasa a la siguiente. Al terminar la última
     * canción de la lista o de la cola **sin loop** no se salta a otra: la
     * reproducción queda pausada en 0:00 sobre la misma canción, con su portada.
     */
    private fun handleTrackFinished() {
        val queue = _playbackQueue.value
        val preview = _currentPreviewTrack.value
        val local = _currentLocalSong.value

        // Loop de una sola canción: se repite desde el principio.
        if (_repeatMode.value == RepeatMode.ONE) {
            when {
                queue.isActive -> playQueueItemAt(queue.currentIndex)
                preview != null -> startPreview(preview)
                local != null -> startLocalPlayback(local, seekPositionMs = 0)
                else -> pauseAtTrackStart()
            }
            return
        }

        // Cola o playlist activa: avanzar mientras queden canciones.
        if (queue.isActive) {
            val next = queue.currentIndex + 1
            if (next in queue.items.indices) {
                playQueueItemAt(next)
                return
            }
            if (_repeatMode.value == RepeatMode.ALL) {
                playQueueItemAt(0)
                return
            }
            // Última de la lista sin loop: se queda en la canción actual.
            pauseAtTrackStart()
            return
        }

        // Sin cola (Biblioteca / Explorar): avanzar dentro de la lista visible.
        if (preview != null) {
            val results = _searchResults.value
            val i = results.indexOfFirst { it.videoId == preview.videoId }
            if (i >= 0 && i + 1 in results.indices) {
                togglePreview(results[i + 1])
                return
            }
            if (_repeatMode.value == RepeatMode.ALL && results.isNotEmpty()) {
                togglePreview(results[0])
                return
            }
            pauseAtTrackStart()
            return
        }
        if (local != null) {
            val songs = _localSongs.value
            val i = songs.indexOfFirst { it.id == local.id }
            if (i >= 0 && i + 1 in songs.indices) {
                playLocalSong(songs[i + 1])
                return
            }
            if (_repeatMode.value == RepeatMode.ALL && songs.isNotEmpty()) {
                playLocalSong(songs[0])
                return
            }
            pauseAtTrackStart()
            return
        }
        pauseAtTrackStart()
    }

    /**
     * Deja la canción actual cargada y visible, pero pausada en 0:00.
     *
     * No libera el reproductor ni borra [_currentLocalSong] / [_currentPreviewTrack]
     * a propósito: así el título y la portada siguen en el mini reproductor, en
     * el reproductor completo y en la notificación, y el botón de play reanuda
     * esa misma canción desde el principio.
     */
    private fun pauseAtTrackStart() {
        val hasLocal = _currentLocalSong.value != null
        val hasPreview = _currentPreviewTrack.value != null
        localProgressJob?.cancel()
        previewProgressJob?.cancel()
        if (hasLocal) {
            try {
                localMediaPlayer?.let { mp ->
                    if (mp.isPlaying) mp.pause()
                    mp.seekTo(0)
                }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo rebobinar el reproductor local: ${e.message}")
            }
            _isLocalPlaying.value = false
            _localProgress.value = 0f
        }
        if (hasPreview) {
            try {
                previewMediaPlayer?.let { mp ->
                    if (mp.isPlaying) mp.pause()
                    mp.seekTo(0)
                }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo rebobinar el preview: ${e.message}")
            }
            _isPreviewPlaying.value = false
            _previewProgress.value = 0f
        }
        // La notificación se conserva en pausa con la canción actual.
        mediaSessionManager.updatePlaybackState(playing = false, positionMs = 0)
    }

    fun playQueueItemAt(index: Int) {
        val q = _playbackQueue.value
        if (index !in q.items.indices) return
        _playbackQueue.value = q.copy(currentIndex = index)
        when (val item = q.items[index]) {
            is PlaybackQueueItem.Stream -> {
                stopLocalPlayback()
                startPreview(item.track)
            }
            is PlaybackQueueItem.Local -> {
                stopPreview()
                startLocalPlayback(item.song, seekPositionMs = 0)
            }
        }
    }

    fun clearRecentHistory() {
        viewModelScope.launch {
            repository.clearRecentHistory()
        }
    }

    // ─── ACCIONES DE COLA AVANZADAS ─────────────────────────

    fun playTrackNext(item: PlaybackQueueItem, title: String) {
        _isMiniPlayerDismissed.value = false
        val q = _playbackQueue.value

        if (q.isActive && (_isPreviewPlaying.value || _isLocalPlaying.value)) {
            val insertIdx = (q.currentIndex + 1).coerceIn(0, q.items.size)
            val newItems = q.items.toMutableList().apply { add(insertIdx, item) }
            val newOrig = q.originalItems.toMutableList().apply { add(insertIdx, item) }
            _playbackQueue.value = q.copy(items = newItems, originalItems = newOrig)
            _toastData.value = ToastData(
                title = "Se reproducirá a continuación: $title",
                quality = "En cola",
                visible = true
            )
        } else {
            _playbackQueue.value = PlaybackQueue(
                items = listOf(item),
                originalItems = listOf(item),
                currentIndex = 0,
                playlistId = null,
                playlistName = "Cola manual"
            )
            stopPreview()
            stopLocalPlayback()
            playQueueItemAt(0)
            _toastData.value = ToastData(
                title = "Reproduciendo ahora: $title",
                quality = "En cola",
                visible = true
            )
        }
    }

    fun playSongEntityNext(songEntity: PlaylistSongEntity) {
        val item = songEntity.toQueueItem(_localSongs.value)
        playTrackNext(item, songEntity.title)
    }

    fun playYouTubeTrackNext(track: YouTubeTrackResult) {
        val item = PlaybackQueueItem.Stream(track)
        playTrackNext(item, track.title)
    }

    fun playLocalSongNext(song: LocalSong) {
        val item = PlaybackQueueItem.Local(song)
        playTrackNext(item, song.title)
    }

    fun playSongEntityNow(songEntity: PlaylistSongEntity) {
        val item = songEntity.toQueueItem(_localSongs.value)
        _isMiniPlayerDismissed.value = false
        val q = _playbackQueue.value
        val items = if (q.items.isNotEmpty()) q.items else listOf(item)
        val idx = items.indexOfFirst {
            when (it) {
                is PlaybackQueueItem.Stream -> it.track.videoId == songEntity.songId
                is PlaybackQueueItem.Local -> it.song.id.toString() == songEntity.songId
            }
        }
        if (idx >= 0) {
            playQueueItemAt(idx)
        } else {
            val newItems = listOf(item) + q.items
            _playbackQueue.value = q.copy(items = newItems, originalItems = newItems, currentIndex = 0)
            playQueueItemAt(0)
        }
    }

    fun clearQueue() {
        stopPreview()
        stopLocalPlayback()
        _playbackQueue.value = PlaybackQueue.EMPTY
    }

    fun addToQueue(item: PlaybackQueueItem, title: String) {
        val q = _playbackQueue.value
        if (q.isActive) {
            val newItems = q.items + item
            val newOrig = q.originalItems + item
            _playbackQueue.value = q.copy(items = newItems, originalItems = newOrig)
            _toastData.value = ToastData(
                title = "Añadido a la cola: $title",
                quality = "En cola",
                visible = true
            )
        } else {
            playTrackNext(item, title)
        }
    }

    fun addSongEntityToQueue(songEntity: PlaylistSongEntity) {
        val item = songEntity.toQueueItem(_localSongs.value)
        addToQueue(item, songEntity.title)
    }

    fun addLocalSongToQueue(song: LocalSong) {
        val item = PlaybackQueueItem.Local(song)
        addToQueue(item, song.title)
    }

    fun removeFromQueue(index: Int) {
        val q = _playbackQueue.value
        if (index !in q.items.indices) return
        val newItems = q.items.toMutableList().apply { removeAt(index) }
        val newOrig = q.originalItems.toMutableList().apply { removeAt(index) }
        val newCurrent = when {
            newItems.isEmpty() -> {
                stopPreview()
                stopLocalPlayback()
                -1
            }
            index < q.currentIndex -> q.currentIndex - 1
            index == q.currentIndex -> {
                val nextIdx = index.coerceIn(0, newItems.lastIndex)
                playQueueItemAt(nextIdx)
                nextIdx
            }
            else -> q.currentIndex
        }
        _playbackQueue.value = q.copy(items = newItems, originalItems = newOrig, currentIndex = newCurrent)
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        val q = _playbackQueue.value
        if (fromIndex !in q.items.indices || toIndex !in q.items.indices) return
        val newItems = q.items.toMutableList().apply {
            val moved = removeAt(fromIndex)
            add(toIndex, moved)
        }
        val newCurrent = if (q.currentIndex == fromIndex) toIndex
        else if (q.currentIndex in minOf(fromIndex, toIndex)..maxOf(fromIndex, toIndex)) {
            if (fromIndex < toIndex) q.currentIndex - 1 else q.currentIndex + 1
        } else q.currentIndex

        _playbackQueue.value = q.copy(items = newItems, currentIndex = newCurrent)
    }

    // ─── CONTROLES DE COLA ─────────────────────────────────

    fun playPlaylist(
        playlist: PlaylistEntity,
        songs: List<PlaylistSongEntity>,
        startIndex: Int = 0,
        shuffle: Boolean = false
    ) {
        if (songs.isEmpty()) return
        _isMiniPlayerDismissed.value = false

        val items = songs.map { it.toQueueItem(_localSongs.value) }
        val orderedItems = if (shuffle) items.shuffled() else items
        val realStart = if (shuffle) 0 else startIndex.coerceIn(0, items.lastIndex)

        _shuffleEnabled.value = shuffle
        _activePlaylist.value = playlist
        _playbackQueue.value = PlaybackQueue(
            items = orderedItems,
            originalItems = items,
            currentIndex = realStart,
            playlistId = playlist.id,
            playlistName = playlist.name
        )

        stopPreview()
        stopLocalPlayback()
        playQueueItemAt(realStart)
    }

    fun toggleShuffle() {
        val newShuffle = !_shuffleEnabled.value
        _shuffleEnabled.value = newShuffle
        val q = _playbackQueue.value
        if (q.items.isEmpty()) return

        val current = q.current
        val newItems = if (newShuffle) {
            val others = q.originalItems.filter { it != current }
            if (current != null) listOf(current) + others.shuffled() else others.shuffled()
        } else {
            q.originalItems
        }
        val newIndex = if (current != null) newItems.indexOf(current).coerceAtLeast(0) else 0
        _playbackQueue.value = q.copy(items = newItems, currentIndex = newIndex)
    }

    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun toggleRepeat() {
        cycleRepeatMode()
    }

    fun toggleFavoriteCurrentTrack() {
        viewModelScope.launch {
            val preview = _currentPreviewTrack.value
            val local = _currentLocalSong.value
            val songId = preview?.videoId ?: local?.id?.toString() ?: return@launch
            val title = preview?.title ?: local?.title ?: ""
            val artist = preview?.channelOrArtist ?: local?.artist ?: ""
            val coverUrl = preview?.thumbnailUrl ?: local?.albumArtUri
            val audioUrl = preview?.previewAudioUrl ?: local?.path

            val isFav = favorites.value.any { it.songId == songId }
            if (isFav) {
                repository.removeFavorite(songId)
            } else {
                repository.insertFavorite(
                    FavoriteSongEntity(
                        songId = songId,
                        title = title,
                        artist = artist,
                        coverUrl = coverUrl,
                        audioUrl = audioUrl,
                        isLocal = local != null
                    )
                )
            }
        }
    }

    fun removeFavorite(songId: String) {
        viewModelScope.launch {
            repository.removeFavorite(songId)
        }
    }

    fun moveSongInPlaylist(songs: List<PlaylistSongEntity>, fromIndex: Int, toIndex: Int) {
        val playlistId = songs.firstOrNull()?.playlistId ?: return
        reorderPlaylistSongs(playlistId, songs, fromIndex, toIndex)
    }

    fun reorderPlaylistSongs(
        playlistId: Long,
        currentSongs: List<PlaylistSongEntity>,
        fromIndex: Int,
        toIndex: Int
    ) {
        viewModelScope.launch {
            repository.reorderPlaylistSongs(playlistId, currentSongs, fromIndex, toIndex)

            val q = _playbackQueue.value
            if (q.playlistId == playlistId) {
                val reordered = currentSongs.toMutableList().apply {
                    val moved = removeAt(fromIndex)
                    add(toIndex, moved)
                }
                val newItems = reordered.map { it.toQueueItem(_localSongs.value) }
                val current = q.current
                val newIndex = if (current != null) newItems.indexOf(current).coerceAtLeast(0) else 0
                _playbackQueue.value = q.copy(
                    items = newItems,
                    originalItems = newItems,
                    currentIndex = newIndex
                )
            }
        }
    }

    // ─── ECUALIZADOR ────────────────────────────────────────

    fun setEqualizerBass(level: Float) { audioEffectEngine.setBassBoost(level) }
    fun setEqualizerVocals(level: Float) { audioEffectEngine.setVocals(level) }
    fun setEqualizerTreble(level: Float) { audioEffectEngine.setTreble(level) }

    fun setMasterVolume(volume: Float) {
        audioEffectEngine.setMasterVolume(volume)
        updateMasterVolume()
    }

    private fun updateMasterVolume() {
        val volFactor = (equalizerState.value.masterVolume / 100f).coerceIn(0f, 1f)
        try {
            previewMediaPlayer?.setVolume(volFactor, volFactor)
            localMediaPlayer?.setVolume(volFactor, volFactor)
        } catch (_: Exception) {}
    }

    fun applyEqualizerPreset(presetName: String) { audioEffectEngine.applyPreset(presetName) }
    fun resetEqualizer() { audioEffectEngine.reset() }

    // ─── PLAYLISTS ──────────────────────────────────────────

    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch { repository.createPlaylist(name, description) }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            if (_playbackQueue.value.playlistId == playlist.id) {
                _playbackQueue.value = PlaybackQueue.EMPTY
            }
            repository.deletePlaylist(playlist)
        }
    }

    fun addTrackToPlaylist(playlistId: Long, track: YouTubeTrackResult) {
        viewModelScope.launch {
            repository.addSongToPlaylist(
                playlistId = playlistId,
                songId = track.videoId,
                title = track.title,
                artist = track.channelOrArtist,
                coverUrl = track.thumbnailUrl,
                audioUrl = track.previewAudioUrl,
                durationText = track.durationText,
                isLocal = false
            )
        }
    }

    fun addLocalSongToPlaylist(playlistId: Long, song: LocalSong) {
        viewModelScope.launch {
            repository.addSongToPlaylist(
                playlistId = playlistId,
                songId = song.id.toString(),
                title = song.title,
                artist = song.artist,
                coverUrl = song.albumArtUri,
                audioUrl = song.path,
                durationText = "3:30",
                isLocal = true
            )
        }
    }

    fun getSongsForPlaylist(playlistId: Long) = repository.getSongsForPlaylist(playlistId)

    fun updatePlaylist(playlistId: Long, name: String, description: String) {
        viewModelScope.launch { repository.updatePlaylist(playlistId, name, description) }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        viewModelScope.launch { repository.removeSongFromPlaylist(playlistId, songId) }
    }

    // ─── RESEÑAS ────────────────────────────────────────────

    fun addReview(songId: String, title: String, artist: String, rating: Float, comment: String, coverUrl: String? = null) {
        viewModelScope.launch { repository.addSongReview(songId, title, artist, rating, comment, coverUrl) }
    }

    fun getReviewsForSong(songId: String) = repository.getReviewsForSong(songId)

    val allReviews: StateFlow<List<SongReviewEntity>> = repository.getAllReviews()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateReview(id: Long, rating: Float, comment: String) {
        viewModelScope.launch { repository.updateSongReview(id, rating, comment) }
    }

    fun deleteReview(id: Long) {
        viewModelScope.launch { repository.deleteSongReview(id) }
    }

    val listenLater: StateFlow<List<ListenLaterEntity>> = repository.getListenLater()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pinnedFavorites: StateFlow<List<PinnedFavoriteEntity>> = repository.getPinnedFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addToListenLater(song: SongRef) {
        viewModelScope.launch { repository.addToListenLater(song) }
    }

    fun removeFromListenLater(songId: String) {
        viewModelScope.launch { repository.removeFromListenLater(songId) }
    }

    fun toggleListenLater(song: SongRef) {
        if (listenLater.value.any { it.songId == song.songId }) removeFromListenLater(song.songId)
        else addToListenLater(song)
    }

    fun pinFavorite(position: Int, song: SongRef) {
        viewModelScope.launch { repository.pinFavorite(position, song) }
    }

    fun unpinFavorite(position: Int) {
        viewModelScope.launch { repository.unpinFavorite(position) }
    }

    private val _reviewSearchResults = MutableStateFlow<List<YouTubeTrackResult>>(emptyList())
    val reviewSearchResults: StateFlow<List<YouTubeTrackResult>> = _reviewSearchResults.asStateFlow()

    private val _isReviewSearching = MutableStateFlow(false)
    val isReviewSearching: StateFlow<Boolean> = _isReviewSearching.asStateFlow()

    private var reviewSearchJob: Job? = null

    /** Búsqueda propia del buscador de reseñas, para no tocar la de Explorar. */
    fun searchSongsForReview(query: String) {
        reviewSearchJob?.cancel()
        if (query.isBlank()) {
            _reviewSearchResults.value = emptyList()
            _isReviewSearching.value = false
            return
        }
        _isReviewSearching.value = true
        reviewSearchJob = viewModelScope.launch {
            delay(400)
            val results = try {
                repository.searchTracks(query)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error buscando para reseñar: ${e.message}")
                emptyList()
            }
            _reviewSearchResults.value = results
            _isReviewSearching.value = false
        }
    }

    // ─── PERFIL ─────────────────────────────────────────────

    fun updateProfile(newProfile: UserProfile) {
        repository.getUserProfileManager().saveProfile(newProfile)
    }

    // ─── UTILIDADES ─────────────────────────────────────────

    private fun getOrCreateSynthWavFile(): File {
        val synthFile = File(getApplication<Application>().cacheDir, "audiophiles_synth.wav")
        if (!synthFile.exists() || synthFile.length() < 1000) {
            val wavBytes = AudioSynthesizer.generateHiFiWav(20)
            FileOutputStream(synthFile).use { it.write(wavBytes) }
        }
        return synthFile
    }

    override fun onCleared() {
        super.onCleared()
        releasePreviewPlayer()
        releaseLocalPlayer()
        audioEffectEngine.release()
        mediaSessionManager.release()
    }
}