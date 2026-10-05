package com.example.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.data.local.AudiophilesDatabase
import com.example.data.local.DownloadedSongEntity
import com.example.data.local.LocalMusicScanner
import com.example.data.local.FavoriteSongEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistSongEntity
import com.example.data.local.RecentSongEntity
import com.example.data.local.SongReviewEntity
import com.example.data.local.UserProfileManager
import com.example.data.model.Artist
import com.example.data.model.AudioQuality
import com.example.data.model.LocalSong
import com.example.data.model.YouTubeTrackResult
import com.example.data.network.YouTubeAudioEngine
import com.example.data.worker.DownloadSongWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import android.media.MediaMetadataRetriever
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

class MusicRepository(private val context: Context) {

    private val database = AudiophilesDatabase.getInstance(context)
    private val dao = database.downloadedSongDao()
    private val playlistDao = database.playlistDao()
    private val reviewDao = database.songReviewDao()
    private val favoriteAndRecentDao = database.favoriteAndRecentDao()
    private val userProfileManager = UserProfileManager(context)
    private val workManager = WorkManager.getInstance(context)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // Curated catalog based on the legendary artists from your Audiophille-s seed database
    private val baseRecommendedTracks = listOf(
        YouTubeTrackResult(
            videoId = "fJ9rUzIMcZQ",
            title = "Bohemian Rhapsody",
            channelOrArtist = "Queen",
            durationText = "5:55",
            durationMs = 355155L,
            thumbnailUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/8b/0a/ea/8b0aea60-6f4a-195b-5958-cdf459c2333b/602527644271.jpg/600x600bb.jpg",
            previewAudioUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview221/v4/17/fc/1e/17fc1eba-946d-84a9-710b-a0e88ea64209/mzaf_3049006317693088799.plus.aac.p.m4a",
            disponiblesQualities = AudioQuality.DEFAULT_QUALITIES
        ),
        YouTubeTrackResult(
            videoId = "5NV6Rdv1a3I",
            title = "Get Lucky",
            channelOrArtist = "Daft Punk",
            durationText = "6:09",
            durationMs = 369629L,
            thumbnailUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/e8/43/5f/e8435ffa-b6b9-b171-40ab-4ff3959ab661/886443919266.jpg/600x600bb.jpg",
            previewAudioUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview211/v4/57/a5/85/57a585aa-f1bc-7619-881b-f8a04a5541d5/mzaf_6906185026678279401.plus.aac.p.m4a",
            disponiblesQualities = AudioQuality.DEFAULT_QUALITIES
        ),
        YouTubeTrackResult(
            videoId = "JwYX52BP2Sk",
            title = "Time",
            channelOrArtist = "Pink Floyd",
            durationText = "7:02",
            durationMs = 422853L,
            thumbnailUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/3e/76/b0/3e76b0e3-762b-2286-a019-8afb19cee541/886445635829.jpg/600x600bb.jpg",
            previewAudioUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview221/v4/a2/73/36/a27336b7-07c8-207a-74fd-9926910f9903/mzaf_10065953245083721782.plus.aac.p.m4a",
            disponiblesQualities = AudioQuality.DEFAULT_QUALITIES
        ),
        YouTubeTrackResult(
            videoId = "hTWKbfoikeg",
            title = "Smells Like Teen Spirit",
            channelOrArtist = "Nirvana",
            durationText = "5:01",
            durationMs = 301096L,
            thumbnailUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/95/fd/b9/95fdb9b2-6d2b-92a6-97f2-51c1a6d77f1a/00602527874609.rgb.jpg/600x600bb.jpg",
            previewAudioUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview221/v4/65/49/4e/65494e02-b6d3-26d8-6b0c-9bd98dcf4d5d/mzaf_7665413316386155700.plus.aac.p.m4a",
            disponiblesQualities = AudioQuality.DEFAULT_QUALITIES
        ),
        YouTubeTrackResult(
            videoId = "HyHNuVaZJ-k",
            title = "Feel Good Inc.",
            channelOrArtist = "Gorillaz",
            durationText = "3:41",
            durationMs = 221173L,
            thumbnailUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/1c/0f/81/1c0f818a-e458-dd84-6f1b-ccbdf5fe14d6/825646291045.jpg/600x600bb.jpg",
            previewAudioUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview221/v4/9a/a7/90/9aa790e3-651e-9674-26ac-14aba4d3b8d1/mzaf_10454527198707970464.plus.aac.p.m4a",
            disponiblesQualities = AudioQuality.DEFAULT_QUALITIES
        ),
        YouTubeTrackResult(
            videoId = "dwucZ5Kpuww",
            title = "Pensando en Ti",
            channelOrArtist = "Canserbero",
            durationText = "4:05",
            durationMs = 245000L,
            thumbnailUrl = "https://i.ytimg.com/vi/dwucZ5Kpuww/hqdefault.jpg",
            previewAudioUrl = "",
            disponiblesQualities = AudioQuality.DEFAULT_QUALITIES
        ),
        YouTubeTrackResult(
            videoId = "kXYiU_JCYtU",
            title = "Numb",
            channelOrArtist = "Linkin Park",
            durationText = "3:07",
            durationMs = 187508L,
            thumbnailUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/13/44/05/134405bd-9e27-a678-8953-b5f724201f95/093624948988.jpg/600x600bb.jpg",
            previewAudioUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview211/v4/1f/9d/28/1f9d2810-2ac3-efbd-ef02-623691ff9cd2/mzaf_17560018091772095942.plus.aac.p.m4a",
            disponiblesQualities = AudioQuality.DEFAULT_QUALITIES
        ),
        YouTubeTrackResult(
            videoId = "Zi_XLOBDo_Y",
            title = "Billie Jean",
            channelOrArtist = "Michael Jackson",
            durationText = "4:53",
            durationMs = 293802L,
            thumbnailUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/32/4f/fd/324ffda2-9e51-8f6a-0c2d-c6fd2b41ac55/074643811224.jpg/600x600bb.jpg",
            previewAudioUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview211/v4/dc/bc/8a/dcbc8a3e-4ce1-c00d-cc02-eda2212053c7/mzaf_8347559338388601510.plus.aac.p.m4a",
            disponiblesQualities = AudioQuality.DEFAULT_QUALITIES
        ),
        YouTubeTrackResult(
            videoId = "X2WH8mHJnhM",
            title = "Karma Police",
            channelOrArtist = "Radiohead",
            durationText = "4:24",
            durationMs = 264067L,
            thumbnailUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/07/60/ba/0760ba0f-148c-b18f-d0ff-169ee96f3af5/634904078164.png/600x600bb.jpg",
            previewAudioUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview211/v4/46/21/35/46213520-da4a-1806-0c59-5ca6ad008b4e/mzaf_5277404092043261430.plus.aac.p.m4a",
            disponiblesQualities = AudioQuality.DEFAULT_QUALITIES
        ),
        YouTubeTrackResult(
            videoId = "tAGnKpE4NCI",
            title = "Enter Sandman",
            channelOrArtist = "Metallica",
            durationText = "5:31",
            durationMs = 331560L,
            thumbnailUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/2e/94/95/2e9495d7-dfe3-ddc8-87ef-6ef797a60218/850007452056.png/600x600bb.jpg",
            previewAudioUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview221/v4/53/8a/fc/538afc1c-b9cd-24ab-b401-2fa8c81abc46/mzaf_3160131754964321707.plus.aac.p.m4a",
            disponiblesQualities = AudioQuality.DEFAULT_QUALITIES
        )
    )

    // Imágenes auténticas y representativas de los artistas desde CDN de alta velocidad
    fun getFeaturedArtists(): List<Artist> = listOf(
        Artist(
            id = "art_queen",
            name = "Queen",
            imageUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/15/c7/55/15c755bc-7603-3ce3-e243-e59f1f9863ea/602527724171.jpg/600x600bb.jpg",
            followersText = "42.5M oyentes",
            isFollowing = true
        ),
        Artist(
            id = "art_daftpunk",
            name = "Daft Punk",
            imageUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/fd/4a/77/fd4a77db-0ebc-d043-41a2-f32fa1bb0fb4/dj.qrikkdwj.jpg/600x600bb.jpg",
            followersText = "28.1M oyentes",
            isFollowing = true
        ),
        Artist(
            id = "art_canserbero",
            name = "Canserbero",
            imageUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/f9/d6/62/f9d662ac-9699-0dd0-8421-3e3d5adae8a0/792278023989.png/600x600bb.jpg",
            followersText = "14.2M oyentes",
            isFollowing = true
        ),
        Artist(
            id = "art_pinkfloyd",
            name = "Pink Floyd",
            imageUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/3e/17/ec/3e17ec6d-f980-c64f-19e0-a6fd8bbf0c10/886445635850.jpg/600x600bb.jpg",
            followersText = "22.4M oyentes",
            isFollowing = false
        ),
        Artist(
            id = "art_nirvana",
            name = "Nirvana",
            imageUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/46/24/33/462433f9-ee74-2d60-4538-859826a7bed7/00720642472729.rgb.jpg/600x600bb.jpg",
            followersText = "31.0M oyentes",
            isFollowing = false
        ),
        Artist(
            id = "art_gorillaz",
            name = "Gorillaz",
            imageUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/1c/0f/81/1c0f818a-e458-dd84-6f1b-ccbdf5fe14d6/825646291045.jpg/600x600bb.jpg",
            followersText = "18.9M oyentes",
            isFollowing = false
        ),
        Artist(
            id = "art_beatles",
            name = "The Beatles",
            imageUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/fa/5b/89/fa5b898d-bad6-e053-4195-260e5c74f2bb/00602567725466.rgb.jpg/600x600bb.jpg",
            followersText = "49.0M oyentes",
            isFollowing = false
        ),
        Artist(
            id = "art_linkinpark",
            name = "Linkin Park",
            imageUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/13/44/05/134405bd-9e27-a678-8953-b5f724201f95/093624948988.jpg/600x600bb.jpg",
            followersText = "35.2M oyentes",
            isFollowing = false
        )
    )

    suspend fun searchTracks(query: String): List<YouTubeTrackResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            val recent = userProfileManager.recentSearches.value
            if (recent.isNotEmpty()) {
                val topArtist = recent.first()
                val dynamicTracks = YouTubeAudioEngine.searchRealYouTube(topArtist)
                if (dynamicTracks.isNotEmpty()) {
                    return@withContext (dynamicTracks.take(6) + baseRecommendedTracks.take(4)).distinctBy { it.videoId }
                }
            }
            return@withContext baseRecommendedTracks
        }

        userProfileManager.addSearchQuery(trimmed)

        val liveResults = YouTubeAudioEngine.searchRealYouTube(trimmed)
        if (liveResults.isNotEmpty()) {
            return@withContext liveResults
        }

        baseRecommendedTracks.filter {
            it.title.contains(trimmed, ignoreCase = true) ||
                    it.channelOrArtist.contains(trimmed, ignoreCase = true)
        }
    }

    fun parseDurationTextToMs(text: String): Long {
        val clean = text.trim()
        val parts = clean.split(":")
        return when (parts.size) {
            2 -> {
                val min = parts[0].toLongOrNull() ?: 3L
                val sec = parts[1].toLongOrNull() ?: 30L
                (min * 60 + sec) * 1000L
            }
            3 -> {
                val hr = parts[0].toLongOrNull() ?: 0L
                val min = parts[1].toLongOrNull() ?: 0L
                val sec = parts[2].toLongOrNull() ?: 0L
                (hr * 3600 + min * 60 + sec) * 1000L
            }
            else -> 210000L
        }
    }

    private val TAG = "MusicRepository"

    /**
     * Descarga inmediata y sin restricciones: Permite datos móviles (celular 3G/4G/5G) y WiFi.
     */
    suspend fun downloadTrackDirectly(
        track: YouTubeTrackResult,
        quality: AudioQuality,
        onProgress: (Float) -> Unit = {}
    ): LocalSong = withContext(Dispatchers.IO) {
        onProgress(0.05f)
        val sanitizedTitle = track.title.replace("[^a-zA-Z0-9.-]".toRegex(), "_")

        val individualCoverPath = LocalMusicScanner.getOrCreateCoverFile(
            context = context,
            videoId = track.videoId,
            title = track.title,
            artist = track.channelOrArtist,
            remoteThumbnailUrl = track.thumbnailUrl
        )
        onProgress(0.10f)

        var audioStreamUrl: String? = null
        var trackDurationText = track.durationText
        var trackDurationMs = track.durationMs

        if (!track.videoId.startsWith("track_") && !track.videoId.startsWith("local_")) {
            try {
                audioStreamUrl = YouTubeAudioEngine.resolveDownloadUrl(track.videoId, quality.format) { convProgress ->
                    onProgress(convProgress)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Fallo al resolver URL directa de YouTube para ${track.videoId}: ${e.message}")
            }
        }

        if (audioStreamUrl.isNullOrEmpty() && track.title.isNotBlank()) {
            try {
                val clean = YouTubeAudioEngine.cleanSongTitle(track.title)
                val liveList = YouTubeAudioEngine.searchRealYouTube("${track.channelOrArtist} $clean")
                val alt = liveList.firstOrNull { it.videoId.isNotEmpty() && !it.videoId.startsWith("track_") && it.videoId != track.videoId }
                if (alt != null) {
                    audioStreamUrl = YouTubeAudioEngine.resolveDownloadUrl(alt.videoId, quality.format) { onProgress(it) }
                    if (!audioStreamUrl.isNullOrEmpty()) {
                        trackDurationMs = alt.durationMs
                        trackDurationText = alt.durationText
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Reintento de búsqueda YouTube falló: ${e.message}")
            }
        }

        if (audioStreamUrl.isNullOrEmpty()) {
            val (resolvedUrl, resolvedMs, resolvedText) = YouTubeAudioEngine.resolveRealAudioStream(
                track.channelOrArtist,
                track.title
            )
            if (!resolvedUrl.isNullOrEmpty()) {
                audioStreamUrl = resolvedUrl
                if (resolvedMs > 0) trackDurationMs = resolvedMs
                if (resolvedText.isNotBlank()) trackDurationText = resolvedText
            }
        }

        if (audioStreamUrl.isNullOrEmpty() && !track.previewAudioUrl.isNullOrEmpty()) {
            audioStreamUrl = track.previewAudioUrl
        }

        var audioBytes: ByteArray? = null
        val streamUrl = audioStreamUrl
        if (!streamUrl.isNullOrEmpty() && streamUrl.startsWith("http")) {
            try {
                val req = Request.Builder()
                    .url(streamUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body
                        val contentLength = body?.contentLength() ?: -1L
                        val inStream = body?.byteStream()
                        val outStream = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        var read: Int
                        var totalRead = 0L
                        if (inStream != null) {
                            while (inStream.read(buffer).also { read = it } != -1) {
                                outStream.write(buffer, 0, read)
                                totalRead += read
                                if (contentLength > 0) {
                                    val streamRatio = (totalRead.toFloat() / contentLength).coerceIn(0f, 1f)
                                    onProgress(0.50f + streamRatio * 0.45f)
                                } else {
                                    val estRatio = (totalRead.toFloat() / (4 * 1024 * 1024)).coerceIn(0f, 0.95f)
                                    onProgress(0.50f + estRatio * 0.45f)
                                }
                            }
                            audioBytes = outStream.toByteArray()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error descargando bytes de stream: ${e.message}")
            }
        }

        var fallbackBytes: ByteArray? = null
        val firstBytes = audioBytes
        if (firstBytes == null || firstBytes.isEmpty()) {
            val (altUrl, altMs, _) = YouTubeAudioEngine.resolveRealAudioStream(track.channelOrArtist, track.title)
            if (!altUrl.isNullOrEmpty() && altUrl != streamUrl) {
                try {
                    val req = Request.Builder()
                        .url(altUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .build()
                    httpClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            fallbackBytes = resp.body?.bytes()
                            if (altMs > 0) trackDurationMs = altMs
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Descarga alternativa también falló: ${e.message}")
                }
            }
        }

        val audioBytesNonNull: ByteArray = firstBytes ?: fallbackBytes
        ?: throw java.io.IOException("No se pudo descargar el audio para \"${track.title}\". Comprueba tu conexión a Internet e inténtalo nuevamente.")

        if (audioBytesNonNull.isEmpty()) {
            throw java.io.IOException("El audio descargado está vacío para \"${track.title}\".")
        }

        val finalFormat = if (streamUrl?.contains("m4a", ignoreCase = true) == true ||
            streamUrl?.contains("aac", ignoreCase = true) == true ||
            streamUrl?.contains("apple", ignoreCase = true) == true) {
            "M4A"
        } else {
            "MP3"
        }

        val finalExt = finalFormat.lowercase()
        val fileName = "${track.channelOrArtist} - $sanitizedTitle.$finalExt"

        onProgress(0.96f)

        val savedUri = LocalMusicScanner.saveSongToMusicFolder(
            context = context,
            fileName = fileName,
            title = track.title,
            artist = track.channelOrArtist,
            inputStream = ByteArrayInputStream(audioBytesNonNull),
            format = finalFormat
        )

        val localPath = if (savedUri != null && savedUri.scheme == "file") {
            savedUri.path ?: savedUri.toString()
        } else if (savedUri != null) {
            savedUri.toString()
        } else {
            val appMusicDir = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC), "Audiophiles")
            File(appMusicDir, fileName).absolutePath
        }

        var realMs = trackDurationMs
        try {
            val mmr = MediaMetadataRetriever()
            if (localPath.startsWith("/")) {
                mmr.setDataSource(localPath)
            } else if (savedUri != null) {
                mmr.setDataSource(context, savedUri)
            }
            val extractedDur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val parsed = extractedDur?.toLongOrNull() ?: 0L
            if (parsed > 0) {
                realMs = parsed
            }
            mmr.release()
        } catch (e: Exception) {
            Log.w(TAG, "MediaMetadataRetriever no pudo leer duracion exacta: ${e.message}")
        }
        if (realMs <= 0) {
            realMs = parseDurationTextToMs(trackDurationText)
        }

        val finalDurationText = "%d:%02d".format(realMs / 1000 / 60, (realMs / 1000) % 60)

        val downloadedEntity = DownloadedSongEntity(
            videoId = track.videoId,
            title = track.title,
            artist = track.channelOrArtist,
            durationText = finalDurationText,
            thumbnailUrl = individualCoverPath,
            localFilePath = localPath,
            bitrate = quality.bitrate,
            format = finalFormat,
            downloadedAt = System.currentTimeMillis()
        )
        dao.insertSong(downloadedEntity)
        onProgress(1.0f)

        LocalSong(
            id = downloadedEntity.downloadedAt,
            title = track.title,
            artist = track.channelOrArtist,
            path = localPath,
            durationMs = realMs,
            albumArtUri = individualCoverPath,
            isDownloadedFromAudiophiles = true,
            qualityLabel = "${quality.bitrate} $finalFormat"
        )
    }

    fun enqueueDownload(track: YouTubeTrackResult, quality: AudioQuality) {
        val inputData = workDataOf(
            DownloadSongWorker.KEY_VIDEO_ID to track.videoId,
            DownloadSongWorker.KEY_TITLE to track.title,
            DownloadSongWorker.KEY_ARTIST to track.channelOrArtist,
            DownloadSongWorker.KEY_FORMAT to quality.format,
            DownloadSongWorker.KEY_BITRATE to quality.bitrate,
            DownloadSongWorker.KEY_DURATION_TEXT to track.durationText,
            DownloadSongWorker.KEY_THUMBNAIL_URL to track.thumbnailUrl,
            DownloadSongWorker.KEY_AUDIO_URL to (track.previewAudioUrl ?: "")
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<DownloadSongWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag("download_${track.videoId}")
            .build()

        workManager.enqueueUniqueWork(
            "download_${track.videoId}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    suspend fun getLocalMusicList(): List<LocalSong> = withContext(Dispatchers.IO) {
        LocalMusicScanner.fetchLocalMusic(context)
    }

    suspend fun deleteLocalSong(song: LocalSong): Boolean = withContext(Dispatchers.IO) {
        try {
            dao.deleteSongByPathOrTitle(song.path, song.title)
            playlistDao.removeSongFromAllPlaylists(song.id.toString(), song.title)
            LocalMusicScanner.deletePhysicalSong(context, song.path, song.title)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getDownloadedSongs(): Flow<List<DownloadedSongEntity>> {
        return dao.getAllDownloadedSongs()
    }

    fun observeDownloadWork(videoId: String): Flow<WorkInfo?> {
        return workManager.getWorkInfosForUniqueWorkFlow("download_$videoId")
            .map { it.firstOrNull() }
    }

    // Playlists
    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    suspend fun createPlaylist(name: String, description: String = ""): Long = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylist(PlaylistEntity(name = name, description = description))
    }

    suspend fun deletePlaylist(playlist: PlaylistEntity) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylist(playlist)
    }

    suspend fun updatePlaylist(playlistId: Long, name: String, description: String) = withContext(Dispatchers.IO) {
        playlistDao.updatePlaylist(playlistId, name, description)
    }

    fun getSongsForPlaylist(playlistId: Long): Flow<List<PlaylistSongEntity>> =
        playlistDao.getSongsForPlaylist(playlistId)

    suspend fun addSongToPlaylist(
        playlistId: Long,
        songId: String,
        title: String,
        artist: String,
        coverUrl: String?,
        audioUrl: String?,
        durationText: String = "3:30",
        isLocal: Boolean = false
    ) = withContext(Dispatchers.IO) {
        // Asignar el siguiente orderIndex disponible
        val nextIndex = playlistDao.getMaxOrderIndex(playlistId) + 1
        playlistDao.insertSongToPlaylist(
            PlaylistSongEntity(
                playlistId = playlistId,
                songId = songId,
                title = title,
                artist = artist,
                coverUrl = coverUrl,
                audioUrl = audioUrl,
                durationText = durationText,
                isLocal = isLocal,
                orderIndex = nextIndex
            )
        )
    }

    /** Reordena las canciones de una playlist moviendo de fromIndex a toIndex. */
    suspend fun reorderPlaylistSongs(
        playlistId: Long,
        currentSongs: List<PlaylistSongEntity>,
        fromIndex: Int,
        toIndex: Int
    ) = withContext(Dispatchers.IO) {
        if (fromIndex !in currentSongs.indices) return@withContext
        if (toIndex !in currentSongs.indices) return@withContext
        if (fromIndex == toIndex) return@withContext

        val mutable = currentSongs.toMutableList()
        val moved = mutable.removeAt(fromIndex)
        mutable.add(toIndex, moved)

        playlistDao.reorderSongs(mutable.map { it.id })
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String) = withContext(Dispatchers.IO) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    // Reviews (5 estrellas y texto abierto)
    fun getReviewsForSong(songId: String): Flow<List<SongReviewEntity>> =
        reviewDao.getReviewsForSong(songId)

    fun getAllRecentReviews(): Flow<List<SongReviewEntity>> =
        reviewDao.getAllRecentReviews()

    fun getAllReviews(): Flow<List<SongReviewEntity>> = reviewDao.getAllReviews()

    suspend fun updateSongReview(id: Long, rating: Float, comment: String) = withContext(Dispatchers.IO) {
        reviewDao.updateReview(id, rating.coerceIn(1f, 5f), comment)
    }

    suspend fun deleteSongReview(id: Long) = withContext(Dispatchers.IO) {
        reviewDao.deleteReview(id)
    }

    suspend fun addSongReview(
        songId: String,
        songTitle: String,
        artist: String,
        rating: Float,
        comment: String
    ) = withContext(Dispatchers.IO) {
        val author = userProfileManager.profile.value.username
        reviewDao.insertReview(
            SongReviewEntity(
                songId = songId,
                songTitle = songTitle,
                artist = artist,
                authorName = author,
                rating = rating.coerceIn(1f, 5f),
                comment = comment
            )
        )
    }

    // Favoritos y Recientes
    fun getAllFavorites(): Flow<List<FavoriteSongEntity>> = favoriteAndRecentDao.getAllFavorites()
    fun getFavoritesCount(): Flow<Int> = favoriteAndRecentDao.getFavoritesCount()
    fun isFavorite(songId: String): Flow<Boolean> = favoriteAndRecentDao.isFavorite(songId)
    suspend fun insertFavorite(song: FavoriteSongEntity) = favoriteAndRecentDao.insertFavorite(song)
    suspend fun removeFavorite(songId: String) = favoriteAndRecentDao.removeFavorite(songId)

    fun getRecentlyPlayed(): Flow<List<RecentSongEntity>> = favoriteAndRecentDao.getRecentlyPlayed()
    fun getRecentlyPlayedCount(): Flow<Int> = favoriteAndRecentDao.getRecentlyPlayedCount()
    suspend fun recordPlayed(song: RecentSongEntity) = favoriteAndRecentDao.recordPlayed(song)
    suspend fun clearRecentHistory() = favoriteAndRecentDao.clearRecentHistory()

    fun getUserProfileManager(): UserProfileManager = userProfileManager
}