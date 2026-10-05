package com.example.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.local.AudiophilesDatabase
import com.example.data.local.DownloadedSongEntity
import com.example.data.local.LocalMusicScanner
import kotlinx.coroutines.delay
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class DownloadSongWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_VIDEO_ID = "key_video_id"
        const val KEY_TITLE = "key_title"
        const val KEY_ARTIST = "key_artist"
        const val KEY_BITRATE = "key_bitrate"
        const val KEY_FORMAT = "key_format"
        const val KEY_THUMBNAIL_URL = "key_thumbnail_url"
        const val KEY_DURATION_TEXT = "key_duration_text"
        const val KEY_AUDIO_URL = "key_audio_url"
        const val KEY_PROGRESS = "progress"
        const val KEY_RESULT_PATH = "result_path"
        const val QUEUE_WORK_NAME = "audiophiles_download_queue"
    }

    override suspend fun doWork(): Result {
        val videoId = inputData.getString(KEY_VIDEO_ID) ?: return Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: "Canción"
        val artist = inputData.getString(KEY_ARTIST) ?: "Artista"
        val bitrate = inputData.getString(KEY_BITRATE) ?: "320 kbps"
        val format = inputData.getString(KEY_FORMAT) ?: "MP3"
        val thumbnailUrl = inputData.getString(KEY_THUMBNAIL_URL) ?: ""
        val durationText = inputData.getString(KEY_DURATION_TEXT) ?: "3:45"
        val audioUrl = inputData.getString(KEY_AUDIO_URL)

        Log.d("DownloadSongWorker", "Iniciando descarga en cola: $title ($bitrate)")

        try {
            // Report initial progress
            setProgress(workDataOf(KEY_PROGRESS to 0.05f))

            val sanitizedTitle = title.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
            val fileName = "$artist - $sanitizedTitle.${format.lowercase()}"

            // 1. Intentar resolver URL de descarga real completa desde YouTube
            var resolvedDownloadUrl: String? = null
            if (!videoId.startsWith("track_") && !videoId.startsWith("local_")) {
                setProgress(workDataOf(KEY_PROGRESS to 0.10f))
                try {
                    resolvedDownloadUrl = com.example.data.network.YouTubeAudioEngine.resolveDownloadUrl(videoId, format) { convProgress ->
                        kotlinx.coroutines.runBlocking {
                            setProgress(workDataOf(KEY_PROGRESS to convProgress))
                        }
                    }
                } catch (e: Exception) {
                    Log.w("DownloadSongWorker", "Fallo resolviendo stream YouTube: ${e.message}")
                }
            }

            // 2. Usar la URL de descarga directa o la URL de audio disponible
            var targetAudioUrl = resolvedDownloadUrl ?: audioUrl

            // Si aún no hay URL, intentar resolver stream alternativo
            if (targetAudioUrl.isNullOrEmpty() && title.isNotBlank()) {
                val (altUrl, _, _) = com.example.data.network.YouTubeAudioEngine.resolveRealAudioStream(artist, title)
                targetAudioUrl = altUrl
            }

            if (targetAudioUrl.isNullOrEmpty()) {
                Log.w("DownloadSongWorker", "No se encontró stream de audio para $title")
                return Result.failure(workDataOf("error_message" to "No se pudo obtener el audio para esta pista"))
            }

            // 3. Descarga real de audio byte por byte
            val audioBytes: ByteArray = fetchNetworkAudio(targetAudioUrl) { progressRatio ->
                // Progresión de descarga de red
                val adjusted = (0.20f + progressRatio * 0.75f).coerceIn(0.20f, 0.95f)
                setProgress(workDataOf(KEY_PROGRESS to adjusted))
            }

            if (audioBytes.size < 5000) {
                return Result.failure(workDataOf("error_message" to "El archivo de audio descargado está incompleto"))
            }

            val individualCover = LocalMusicScanner.getOrCreateCoverFile(
                context = context,
                videoId = videoId,
                title = title,
                artist = artist,
                remoteThumbnailUrl = thumbnailUrl
            )

            setProgress(workDataOf(KEY_PROGRESS to 0.95f))

            // Guardar en MediaStore /Music/Audiophiles/
            val finalFormat = if (audioBytes.isNotEmpty()) format else "WAV"
            val savedUri = LocalMusicScanner.saveSongToMusicFolder(
                context = context,
                fileName = fileName,
                title = title,
                artist = artist,
                inputStream = ByteArrayInputStream(audioBytes),
                format = finalFormat
            )

            val localPath = savedUri?.toString() ?: "content://media/external/audio/media"

            // Registrar en base de datos local Room
            val dao = AudiophilesDatabase.getInstance(context).downloadedSongDao()
            dao.insertSong(
                DownloadedSongEntity(
                    videoId = videoId,
                    title = title,
                    artist = artist,
                    durationText = durationText,
                    thumbnailUrl = individualCover,
                    localFilePath = localPath,
                    bitrate = bitrate,
                    format = finalFormat,
                    downloadedAt = System.currentTimeMillis()
                )
            )

            setProgress(workDataOf(KEY_PROGRESS to 1.0f))
            Log.d("DownloadSongWorker", "Descarga completada con éxito: $localPath")

            return Result.success(
                workDataOf(
                    KEY_RESULT_PATH to localPath,
                    KEY_PROGRESS to 1.0f
                )
            )
        } catch (e: Exception) {
            Log.e("DownloadSongWorker", "Fallo en descarga: ${e.message}", e)
            return Result.failure(workDataOf("error_message" to (e.message ?: "Error desconocido")))
        }
    }

    private suspend fun fetchNetworkAudio(urlStr: String, onProgress: suspend (Float) -> Unit): ByteArray {
        val url = URL(urlStr)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 15000
        connection.connect()

        val contentLength = connection.contentLength
        val inputStream = connection.inputStream
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        var bytesRead: Int
        var totalRead = 0

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            output.write(buffer, 0, bytesRead)
            totalRead += bytesRead
            if (contentLength > 0) {
                val ratio = (totalRead.toFloat() / contentLength).coerceIn(0.1f, 0.9f)
                onProgress(ratio)
            }
        }
        inputStream.close()
        return output.toByteArray()
    }
}
