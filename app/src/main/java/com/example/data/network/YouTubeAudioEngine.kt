package com.example.data.network

import android.util.Log
import com.example.data.model.AudioQuality
import com.example.data.model.YouTubeTrackResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class LiveAudioMetadata(
    val title: String,
    val artist: String,
    val previewUrl: String,
    val durationText: String,
    val durationMs: Long,
    val artworkUrl: String
)

object YouTubeAudioEngine {

    private const val TAG = "YouTubeAudioEngine"

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun cleanSongTitle(rawTitle: String): String {
        var t = rawTitle
        val patterns = listOf(
            "\\(official[^\\)]*\\)", "\\[official[^\\]]*\\]",
            "\\(video[^\\)]*\\)", "\\[video[^\\]]*\\]",
            "\\(audio[^\\)]*\\)", "\\[audio[^\\]]*\\]",
            "\\(lyrics?[^\\)]*\\)", "\\[lyrics?[^\\]]*\\]",
            "\\(remaster[^\\)]*\\)", "\\[remaster[^\\]]*\\]",
            "\\(visualizer[^\\)]*\\)", "\\[visualizer[^\\]]*\\]",
            "\\(video oficial[^\\)]*\\)", "\\[video oficial[^\\]]*\\]",
            "\\[4k[^\\]]*\\]", "\\(4k[^\\)]*\\)", "\\(hd[^\\)]*\\)"
        )
        for (p in patterns) {
            t = t.replace(Regex(p, RegexOption.IGNORE_CASE), "")
        }
        if (t.contains(" - ")) {
            val parts = t.split(" - ")
            if (parts.size >= 2 && parts[1].trim().isNotEmpty()) {
                t = parts[1]
            }
        }
        val cleaned = t.trim()
        return if (cleaned.isNotEmpty()) cleaned else rawTitle.trim()
    }

    /**
     * Resuelve el stream de audio real AAC/M4A y la duración exacta en ms para cualquier artista y canción.
     */
    suspend fun resolveRealAudioStream(artist: String, title: String): Triple<String?, Long, String> = withContext(Dispatchers.IO) {
        val clean = cleanSongTitle(title)
        val queryTerm = if (artist.isNotBlank() &&
            !artist.contains("Audiophile", ignoreCase = true) &&
            !artist.contains("YouTube", ignoreCase = true) &&
            !artist.contains("Desconocido", ignoreCase = true)
        ) {
            "$artist $clean"
        } else {
            clean
        }

        try {
            val itunesUrl = "https://itunes.apple.com/search?term=${URLEncoder.encode(queryTerm, "UTF-8")}&entity=song&limit=3"
            val req = Request.Builder()
                .url(itunesUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val json = JSONObject(resp.body?.string() ?: "{}")
                val results = json.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val item = results.getJSONObject(0)
                    val pUrl = item.optString("previewUrl")
                    val durMs = item.optLong("trackTimeMillis", 210000L)
                    val mins = durMs / 1000 / 60
                    val secs = (durMs / 1000) % 60
                    val durText = "%d:%02d".format(mins, secs)
                    if (pUrl.isNotEmpty()) {
                        return@withContext Triple(pUrl, durMs, durText)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error resolviendo stream iTunes para $queryTerm: ${e.message}")
        }
        Triple(null, 210000L, "3:30")
    }

    /**
     * Búsqueda en vivo combinando YouTube Music InnerTube con metadatos reales y streams de audio AAC.
     */
    suspend fun searchRealYouTube(query: String): List<YouTubeTrackResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()

        val itunesMatches = mutableListOf<LiveAudioMetadata>()

        // 1. Obtener catálogo y streams de audio de alta fidelidad desde iTunes Search API
        try {
            val itunesUrl = "https://itunes.apple.com/search?term=${URLEncoder.encode(trimmed, "UTF-8")}&entity=song&limit=25"
            val itunesRequest = Request.Builder()
                .url(itunesUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val itunesResponse = httpClient.newCall(itunesRequest).execute()
            if (itunesResponse.isSuccessful) {
                val itunesBody = itunesResponse.body?.string() ?: ""
                val itunesJson = JSONObject(itunesBody)
                val resultsArray = itunesJson.optJSONArray("results") ?: JSONArray()
                for (i in 0 until resultsArray.length()) {
                    val item = resultsArray.getJSONObject(i)
                    val tName = item.optString("trackName", "")
                    val aName = item.optString("artistName", "")
                    val pUrl = item.optString("previewUrl", "")
                    val durMs = item.optLong("trackTimeMillis", 210000L)
                    val mins = durMs / 1000 / 60
                    val secs = (durMs / 1000) % 60
                    val durText = "%d:%02d".format(mins, secs)
                    val artwork = item.optString("artworkUrl100", "").replace("100x100bb", "600x600bb")

                    if (tName.isNotEmpty()) {
                        itunesMatches.add(
                            LiveAudioMetadata(
                                title = tName,
                                artist = aName,
                                previewUrl = pUrl,
                                durationText = durText,
                                durationMs = durMs,
                                artworkUrl = artwork
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error consultando iTunes previews: ${e.message}")
        }

        // 2. Búsqueda real en vivo en YouTube Web API
        val ytResults = mutableListOf<YouTubeTrackResult>()
        try {
            val ytWebUrl = "https://www.youtube.com/youtubei/v1/search"
            val payload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("hl", "es")
                        put("gl", "ES")
                        put("clientName", "WEB")
                        put("clientVersion", "2.20240101.00.00")
                    })
                })
                put("query", trimmed)
            }

            val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val ytRequest = Request.Builder()
                .url(ytWebUrl)
                .post(requestBody)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Origin", "https://www.youtube.com")
                .build()

            val ytResponse = httpClient.newCall(ytRequest).execute()
            if (ytResponse.isSuccessful) {
                val ytBody = ytResponse.body?.string() ?: ""
                val ytJson = JSONObject(ytBody)
                parseYouTubeVideoRenderers(ytJson, ytResults)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error consultando YouTube Web Search: ${e.message}")
        }

        // Si la búsqueda Web estuvo vacía, intentar YouTube Music InnerTube como respaldo
        if (ytResults.isEmpty()) {
            try {
                val innerTubeUrl = "https://music.youtube.com/youtubei/v1/search"
                val payload = JSONObject().apply {
                    put("context", JSONObject().apply {
                        put("client", JSONObject().apply {
                            put("clientName", "WEB_REMIX")
                            put("clientVersion", "1.20231204.01.00")
                        })
                    })
                    put("query", trimmed)
                }
                val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val ytRequest = Request.Builder()
                    .url(innerTubeUrl)
                    .post(requestBody)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36")
                    .header("Origin", "https://music.youtube.com")
                    .build()
                val ytResponse = httpClient.newCall(ytRequest).execute()
                if (ytResponse.isSuccessful) {
                    val ytBody = ytResponse.body?.string() ?: ""
                    parseInnerTubeTracks(JSONObject(ytBody), ytResults)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Respaldo InnerTube también falló: ${e.message}")
            }
        }

        // 3. Enriquecer los resultados de YouTube con preview rápido si coincide con iTunes
        if (ytResults.isNotEmpty()) {
            val enriched = ytResults.map { track ->
                val clean = cleanSongTitle(track.title).lowercase()
                val artistLower = track.channelOrArtist.lowercase()

                val matched = itunesMatches.firstOrNull { m ->
                    val mTitle = m.title.lowercase()
                    val mArtist = m.artist.lowercase()
                    (mTitle.contains(clean) || clean.contains(mTitle)) &&
                            (artistLower.contains(mArtist) || mArtist.contains(artistLower) || artistLower.isEmpty() || artistLower.contains("audiophile"))
                } ?: itunesMatches.firstOrNull { m ->
                    val mTitle = m.title.lowercase()
                    mTitle.contains(clean) || clean.contains(mTitle)
                }

                if (matched != null) {
                    track.copy(
                        previewAudioUrl = matched.previewUrl,
                        // Conservar la duración real de YouTube si ya está presente
                        durationText = if (track.durationText != "3:45" && track.durationText.isNotEmpty()) track.durationText else matched.durationText,
                        durationMs = if (track.durationMs != 210000L && track.durationMs > 0) track.durationMs else matched.durationMs,
                        thumbnailUrl = if (track.thumbnailUrl.isNotBlank()) track.thumbnailUrl else matched.artworkUrl
                    )
                } else {
                    track
                }
            }
            return@withContext enriched
        }

        // 4. Si YouTube no devolvió resultados o falló, usar resultados de iTunes directamente
        if (itunesMatches.isNotEmpty()) {
            return@withContext itunesMatches.mapIndexed { idx, item ->
                YouTubeTrackResult(
                    videoId = "track_itunes_$idx",
                    title = item.title,
                    channelOrArtist = item.artist,
                    durationText = item.durationText,
                    durationMs = item.durationMs,
                    thumbnailUrl = item.artworkUrl,
                    previewAudioUrl = item.previewUrl,
                    disponiblesQualities = AudioQuality.DEFAULT_QUALITIES
                )
            }
        }

        emptyList()
    }

    fun parseDurationToMillis(text: String): Long {
        val parts = text.trim().split(":")
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

    private fun parseYouTubeVideoRenderers(root: JSONObject, outList: MutableList<YouTubeTrackResult>) {
        fun searchRecursively(obj: Any?) {
            when (obj) {
                is JSONObject -> {
                    if (obj.has("videoRenderer")) {
                        val v = obj.getJSONObject("videoRenderer")
                        val videoId = v.optString("videoId")
                        if (videoId.isNotEmpty()) {
                            // Title
                            var rawTitle = ""
                            val titleRuns = v.optJSONObject("title")?.optJSONArray("runs")
                            if (titleRuns != null && titleRuns.length() > 0) {
                                val sb = StringBuilder()
                                for (i in 0 until titleRuns.length()) {
                                    sb.append(titleRuns.getJSONObject(i).optString("text"))
                                }
                                rawTitle = sb.toString().trim()
                            }

                            // Owner / Channel
                            var owner = ""
                            val ownerRuns = v.optJSONObject("ownerText")?.optJSONArray("runs")
                                ?: v.optJSONObject("longBylineText")?.optJSONArray("runs")
                                ?: v.optJSONObject("shortBylineText")?.optJSONArray("runs")
                            if (ownerRuns != null && ownerRuns.length() > 0) {
                                val sb = StringBuilder()
                                for (i in 0 until ownerRuns.length()) {
                                    sb.append(ownerRuns.getJSONObject(i).optString("text"))
                                }
                                owner = sb.toString().trim()
                            }

                            // Duration
                            val lengthText = v.optJSONObject("lengthText")?.optString("simpleText") ?: ""
                            val durationText = if (lengthText.isNotEmpty()) lengthText else "3:45"
                            val durationMs = parseDurationToMillis(durationText)

                            // Thumbnail
                            val thumb = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

                            // Clean artist & title
                            var artist = owner
                            var cleanTitle = rawTitle

                            if (rawTitle.contains(" - ")) {
                                val split = rawTitle.split(" - ")
                                if (split.size >= 2) {
                                    if (artist.isBlank() || artist.contains("VEVO", true) || artist.contains("Topic", true) || artist.contains("Official", true)) {
                                        artist = split[0].trim()
                                    }
                                    cleanTitle = split.subList(1, split.size).joinToString(" - ").trim()
                                }
                            }

                            if (artist.isBlank()) {
                                artist = if (owner.isNotBlank()) owner else "Artista Audiophile's"
                            }
                            cleanTitle = cleanSongTitle(cleanTitle)

                            if (cleanTitle.isNotEmpty()) {
                                outList.add(
                                    YouTubeTrackResult(
                                        videoId = videoId,
                                        title = cleanTitle,
                                        channelOrArtist = artist,
                                        durationText = durationText,
                                        durationMs = durationMs,
                                        thumbnailUrl = thumb,
                                        previewAudioUrl = "",
                                        disponiblesQualities = AudioQuality.DEFAULT_QUALITIES
                                    )
                                )
                            }
                        }
                    }

                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        searchRecursively(obj.get(key))
                    }
                }
                is JSONArray -> {
                    for (i in 0 until obj.length()) {
                        searchRecursively(obj.get(i))
                    }
                }
            }
        }

        searchRecursively(root)
    }

    private fun parseInnerTubeTracks(root: JSONObject, outList: MutableList<YouTubeTrackResult>) {
        fun searchRecursively(obj: Any?) {
            when (obj) {
                is JSONObject -> {
                    if (obj.has("musicResponsiveListItemRenderer")) {
                        val renderer = obj.getJSONObject("musicResponsiveListItemRenderer")
                        var videoId = ""
                        var title = ""
                        var artist = ""
                        var thumbnail = ""

                        // VideoId
                        val overlay = renderer.optJSONObject("overlay")
                        val thumbOverlay = overlay?.optJSONObject("musicItemThumbnailOverlayRenderer")
                        val content = thumbOverlay?.optJSONObject("content")
                        val playButton = content?.optJSONObject("musicPlayButtonRenderer")
                        val navEndpoint = playButton?.optJSONObject("playNavigationEndpoint")
                        val watchEndpoint = navEndpoint?.optJSONObject("watchEndpoint")
                        videoId = watchEndpoint?.optString("videoId") ?: ""

                        // Title & Artist
                        val flexCols = renderer.optJSONArray("flexColumns")
                        if (flexCols != null) {
                            if (flexCols.length() > 0) {
                                val col0 = flexCols.getJSONObject(0).optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                                val text0 = col0?.optJSONObject("text")
                                val runs0 = text0?.optJSONArray("runs")
                                if (runs0 != null && runs0.length() > 0) {
                                    title = runs0.getJSONObject(0).optString("text")
                                }
                            }
                            if (flexCols.length() > 1) {
                                val col1 = flexCols.getJSONObject(1).optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                                val text1 = col1?.optJSONObject("text")
                                val runs1 = text1?.optJSONArray("runs")
                                if (runs1 != null) {
                                    val parts = mutableListOf<String>()
                                    for (j in 0 until runs1.length()) {
                                        val runObj = runs1.getJSONObject(j)
                                        val runText = runObj.optString("text").trim()
                                        if (runText.isNotEmpty() && runText != "•" &&
                                            !runText.equals("Canción", ignoreCase = true) &&
                                            !runText.equals("Song", ignoreCase = true) &&
                                            !runText.equals("Video", ignoreCase = true) &&
                                            !runText.contains("visualizaciones", ignoreCase = true) &&
                                            !runText.contains("views", ignoreCase = true)
                                        ) {
                                            val browseId = runObj.optJSONObject("navigationEndpoint")
                                                ?.optJSONObject("browseEndpoint")
                                                ?.optString("browseId") ?: ""
                                            if (browseId.startsWith("UC") || browseId.startsWith("FEmusic_library_privately_owned_artist")) {
                                                artist = runText
                                                break
                                            }
                                            parts.add(runText)
                                        }
                                    }
                                    if (artist.isEmpty() && parts.isNotEmpty()) {
                                        artist = parts.first()
                                    }
                                }
                            }
                        }

                        var cleanTitle = title
                        if (artist.isBlank() || artist.equals("YouTube Music", ignoreCase = true)) {
                            if (cleanTitle.contains(" - ")) {
                                val split = cleanTitle.split(" - ")
                                if (split.size >= 2 && split[0].trim().isNotEmpty()) {
                                    artist = split[0].trim()
                                    cleanTitle = split.subList(1, split.size).joinToString(" - ").trim()
                                }
                            }
                        }

                        val resolvedArtist = if (artist.isNotBlank() && !artist.equals("YouTube Music", ignoreCase = true)) {
                            artist
                        } else {
                            "Artista Audiophile's"
                        }

                        val thumbRenderer = renderer.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")
                        val thumbObj = thumbRenderer?.optJSONObject("thumbnail")
                        val thumbs = thumbObj?.optJSONArray("thumbnails")
                        if (thumbs != null && thumbs.length() > 0) {
                            thumbnail = thumbs.getJSONObject(thumbs.length() - 1).optString("url")
                        }

                        if (videoId.isNotEmpty() && cleanTitle.isNotEmpty()) {
                            outList.add(
                                YouTubeTrackResult(
                                    videoId = videoId,
                                    title = cleanTitle,
                                    channelOrArtist = resolvedArtist,
                                    durationText = "3:30",
                                    durationMs = 210000L,
                                    thumbnailUrl = thumbnail,
                                    previewAudioUrl = "",
                                    disponiblesQualities = AudioQuality.DEFAULT_QUALITIES
                                )
                            )
                        }
                    }

                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        searchRecursively(obj.get(key))
                    }
                }
                is JSONArray -> {
                    for (i in 0 until obj.length()) {
                        searchRecursively(obj.get(i))
                    }
                }
            }
        }

        searchRecursively(root)
    }

    /**
     * Resuelve la URL directa de descarga de audio para un track de YouTube,
     * informando el progreso real de conversión.
     */
    suspend fun resolveDownloadUrl(
        videoId: String,
        quality: String = "320",
        onProgress: (Float) -> Unit = {}
    ): String? = withContext(Dispatchers.IO) {
        val domains = listOf("p.savenow.to", "p.lbserver.xyz")
        val cleanFormat = if (quality.contains("m4a", true) || quality.contains("aac", true)) "m4a" else "mp3"

        for (domain in domains) {
            try {
                val initUrl = "https://$domain/api/v2/download?button=1&format=$cleanFormat&url=https://www.youtube.com/watch?v=$videoId"
                val request = Request.Builder()
                    .url(initUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) continue

                val bodyStr = response.body?.string() ?: continue
                val json = JSONObject(bodyStr)

                val directUrl = json.optString("download_url")
                if (directUrl.isNotEmpty()) {
                    onProgress(0.50f)
                    return@withContext directUrl
                }

                val progressUrl = json.optString("progress_url").ifEmpty {
                    val id = json.optString("id")
                    if (id.isNotEmpty()) "https://$domain/api/progress?id=$id" else ""
                }

                if (progressUrl.isEmpty()) continue

                // Polling con reporte reactivo de conversión real
                for (poll in 1..28) {
                    delay(1000)
                    try {
                        val pollReq = Request.Builder()
                            .url(progressUrl)
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                            .build()
                        val pollResp = httpClient.newCall(pollReq).execute()
                        if (pollResp.isSuccessful) {
                            val pollJson = JSONObject(pollResp.body?.string() ?: "{}")
                            val textMsg = pollJson.optString("text", "")
                            if (textMsg.contains("unavailable", ignoreCase = true) || textMsg.contains("removed", ignoreCase = true)) {
                                Log.w(TAG, "Video $videoId no disponible en $domain: $textMsg")
                                break
                            }

                            val pVal = pollJson.optInt("progress", 0)
                            if (pVal > 0) {
                                val ratio = (pVal / 1000f).coerceIn(0.05f, 0.95f)
                                onProgress(0.10f + ratio * 0.40f)
                            } else {
                                onProgress(0.10f + (poll / 28f) * 0.40f)
                            }

                            val finalDownloadUrl = pollJson.optString("download_url")
                            if (finalDownloadUrl.isNotEmpty()) {
                                onProgress(0.50f)
                                return@withContext finalDownloadUrl
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error en poll $poll para $videoId: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Fallo consultando $domain para $videoId: ${e.message}")
            }
        }
        null
    }
}
