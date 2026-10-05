package com.example.data.local

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.LocalSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

object LocalMusicScanner {

    private const val TAG = "LocalMusicScanner"
    const val AUDIOPHILES_SUBDIRECTORY = "Music/Audiophiles"
    private const val TRASH_PREFIX = ".deleted_"

    private const val PREFS_DELETED = "audiophiles_deleted_songs_v2"
    private const val KEY_DELETED_SET = "deleted_set"

    // ═══════════════════════════════════════════════════════════════
    // LISTA NEGRA
    // ═══════════════════════════════════════════════════════════════

    fun markSongAsDeleted(context: Context, path: String, title: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS_DELETED, Context.MODE_PRIVATE)
            val currentSet = prefs.getStringSet(KEY_DELETED_SET, emptySet())?.toMutableSet() ?: mutableSetOf()
            val normTitle = normalize(title)
            if (normTitle.isNotEmpty()) currentSet.add(normTitle)
            currentSet.add(title.lowercase().trim())
            if (path.isNotEmpty()) {
                currentSet.add(path)
                currentSet.add(path.substringAfterLast("/"))
            }
            prefs.edit().putStringSet(KEY_DELETED_SET, currentSet).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error marcando canción como borrada: ${e.message}")
        }
    }

    fun isSongDeleted(context: Context, path: String, title: String): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREFS_DELETED, Context.MODE_PRIVATE)
            val set = prefs.getStringSet(KEY_DELETED_SET, emptySet()) ?: return false
            val norm = normalize(title)
            set.contains(norm) || set.contains(title.lowercase().trim()) || set.contains(path) ||
                    (path.isNotEmpty() && set.contains(path.substringAfterLast("/")))
        } catch (_: Exception) {
            false
        }
    }

    fun normalize(str: String): String = str.lowercase().replace("[^a-z0-9]".toRegex(), "")

    // ═══════════════════════════════════════════════════════════════
    // PERMISOS
    // ═══════════════════════════════════════════════════════════════

    fun hasAudioReadPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // DIRECTORIOS
    // ═══════════════════════════════════════════════════════════════

    private fun getPrivateMusicDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Audiophiles")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getPublicMusicDir(): File {
        return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Audiophiles")
    }

    private fun getCoversDir(context: Context): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        val dir = File(base, "covers")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILIDADES DE AUDIO
    // ═══════════════════════════════════════════════════════════════

    fun parseDurationTextToMs(text: String): Long {
        val parts = text.trim().split(":")
        return when (parts.size) {
            2 -> ((parts[0].toLongOrNull() ?: 3L) * 60 + (parts[1].toLongOrNull() ?: 30L)) * 1000L
            3 -> ((parts[0].toLongOrNull() ?: 0L) * 3600 + (parts[1].toLongOrNull() ?: 0L) * 60 + (parts[2].toLongOrNull() ?: 0L)) * 1000L
            else -> 210000L
        }
    }

    fun getAudioDurationFromFile(filePath: String, fallbackMs: Long = 210000L): Long {
        if (filePath.isEmpty()) return fallbackMs
        try {
            val cleanPath = if (filePath.startsWith("file://")) filePath.removePrefix("file://") else filePath
            val f = File(cleanPath)
            if (!f.exists()) return fallbackMs
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(f.absolutePath)
            val durStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val extracted = durStr?.toLongOrNull() ?: 0L
            mmr.release()
            if (extracted > 1000L) return extracted
        } catch (_: Exception) {}
        return fallbackMs
    }

    // ═══════════════════════════════════════════════════════════════
    // PORTADAS (EMBEDDED METADATA -> REMOTO -> PROCEDURAL)
    // ═══════════════════════════════════════════════════════════════

    fun getOrCreateCoverFile(
        context: Context,
        videoId: String,
        title: String,
        artist: String,
        remoteThumbnailUrl: String? = null,
        filePath: String? = null
    ): String {
        try {
            val coversDir = getCoversDir(context)
            val safeId = if (videoId.isNotBlank()) {
                videoId.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            } else {
                normalize(title)
            }
            val coverFile = File(coversDir, "cover_${safeId}.jpg")

            if (coverFile.exists() && coverFile.length() > 500) {
                return "file://${coverFile.absolutePath}"
            }
            if (coverFile.exists()) coverFile.delete()

            // 1. INTENTAR EXTRAER CARÁTULA INCRUSTADA EN LOS METADATOS DEL ARCHIVO DE AUDIO
            if (!filePath.isNullOrEmpty()) {
                if (extractEmbeddedCover(filePath, coverFile)) {
                    return "file://${coverFile.absolutePath}"
                }
            }

            // 2. INTENTAR DESCARGAR MINIATURA REMOTA SI EXISTE
            if (!remoteThumbnailUrl.isNullOrEmpty() && remoteThumbnailUrl.startsWith("http")) {
                if (downloadImage(remoteThumbnailUrl, coverFile)) {
                    return "file://${coverFile.absolutePath}"
                }
            }

            // 3. SI NO TIENE NADA, GENERAR CARÁTULA PROCEDURAL
            generateProceduralCover(coverFile, safeId, title, artist)
            return if (coverFile.exists() && coverFile.length() > 500) {
                "file://${coverFile.absolutePath}"
            } else ""
        } catch (e: Exception) {
            Log.e(TAG, "Error en getOrCreateCoverFile: ${e.message}", e)
            return ""
        }
    }

    private fun extractEmbeddedCover(filePath: String, destination: File): Boolean {
        var mmr: MediaMetadataRetriever? = null
        try {
            val cleanPath = if (filePath.startsWith("file://")) filePath.removePrefix("file://") else filePath
            val f = File(cleanPath)
            if (!f.exists()) return false

            mmr = MediaMetadataRetriever()
            mmr.setDataSource(f.absolutePath)
            val artBytes = mmr.embeddedPicture
            if (artBytes != null && artBytes.isNotEmpty()) {
                val bitmap = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                if (bitmap != null) {
                    FileOutputStream(destination).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                    }
                    bitmap.recycle()
                    return destination.exists() && destination.length() > 500
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo extraer carátula incrustada: ${e.message}")
        } finally {
            try {
                mmr?.release()
            } catch (_: Exception) {}
        }
        return false
    }

    private fun downloadImage(urlStr: String, destination: File): Boolean {
        return try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            if (conn.responseCode == 200) {
                val bytes = conn.inputStream.readBytes()
                if (bytes.size > 500) {
                    FileOutputStream(destination).use { it.write(bytes) }
                    true
                } else false
            } else false
        } catch (e: Exception) {
            Log.w(TAG, "Fallo descargando cover: ${e.message}")
            false
        }
    }

    private fun generateProceduralCover(coverFile: File, safeId: String, title: String, artist: String) {
        try {
            val size = 512
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val hash = abs((safeId + title + artist).hashCode())
            val palettes = listOf(
                Pair(0xFF2E0854.toInt(), 0xFF7928CA.toInt()),
                Pair(0xFF0F2027.toInt(), 0xFF00ADB5.toInt()),
                Pair(0xFF330825.toInt(), 0xFFFF007F.toInt()),
                Pair(0xFF141E30.toInt(), 0xFF243B55.toInt()),
                Pair(0xFF16222F.toInt(), 0xFF00DFD8.toInt()),
                Pair(0xFF3A1C71.toInt(), 0xFFD76D77.toInt()),
                Pair(0xFF1F1C2C.toInt(), 0xFF928DAB.toInt())
            )
            val (c1, c2) = palettes[hash % palettes.size]
            canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(),
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), c1, c2, Shader.TileMode.CLAMP)
                })
            val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; color = 0x26FFFFFF; strokeWidth = 2.5f
            }
            val cx = size / 2f
            val cy = size / 2f
            for (r in listOf(75f, 115f, 155f, 195f, 225f)) canvas.drawCircle(cx, cy, r, ringPaint)
            canvas.drawCircle(cx, cy, 70f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55000000 })
            val initial = (if (artist.isNotBlank() && artist != "YouTube Music" && artist != "Desconocido") artist else title).take(1).uppercase()
            val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFFFFFF.toInt(); textSize = 68f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER
            }
            canvas.drawText(initial, cx, cy + 24f, tp)
            FileOutputStream(coverFile).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error en generateProceduralCover: ${e.message}", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GUARDAR CANCIÓN (HÍBRIDO)
    // ═══════════════════════════════════════════════════════════════

    suspend fun saveSongToMusicFolder(
        context: Context,
        fileName: String,
        title: String,
        artist: String,
        inputStream: InputStream,
        format: String = "MP3"
    ): Uri? = withContext(Dispatchers.IO) {
        val extension = when (format.uppercase()) {
            "AAC", "M4A" -> "m4a"
            "WAV" -> "wav"
            "FLAC" -> "flac"
            else -> "mp3"
        }
        val cleanFileName = if (fileName.contains('.')) fileName else "$fileName.$extension"
        val mimeType = when (extension) {
            "m4a" -> "audio/mp4"
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            else -> "audio/mpeg"
        }

        val audioBytes = inputStream.readBytes()

        val privateDir = getPrivateMusicDir(context)
        val privateFile = File(privateDir, cleanFileName)
        try {
            FileOutputStream(privateFile).use { it.write(audioBytes) }
            Log.d(TAG, "✅ Guardado en PRIVADO: ${privateFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error guardando en privado: ${e.message}", e)
            return@withContext null
        }

        if (hasAudioReadPermission(context)) {
            try {
                val publicDir = getPublicMusicDir()
                if (!publicDir.exists()) publicDir.mkdirs()
                val publicFile = File(publicDir, cleanFileName)
                FileOutputStream(publicFile).use { it.write(audioBytes) }
                Log.d(TAG, "✅ Copia en PÚBLICO: ${publicFile.absolutePath}")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ No se pudo copiar a público: ${e.message}")
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver: ContentResolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Audio.Media.DISPLAY_NAME, cleanFileName)
                        put(MediaStore.Audio.Media.TITLE, title)
                        put(MediaStore.Audio.Media.ARTIST, artist)
                        put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                        put(MediaStore.Audio.Media.RELATIVE_PATH, AUDIOPHILES_SUBDIRECTORY)
                        put(MediaStore.Audio.Media.IS_PENDING, 1)
                    }
                    val collectionUri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    val itemUri = resolver.insert(collectionUri, contentValues)
                    if (itemUri != null) {
                        resolver.openOutputStream(itemUri)?.use { it.write(audioBytes) }
                        contentValues.clear()
                        contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                        resolver.update(itemUri, contentValues, null, null)
                        Log.d(TAG, "✅ Registrado en MediaStore: $itemUri")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ No se pudo registrar en MediaStore: ${e.message}")
            }
        }

        Uri.fromFile(privateFile)
    }

    // ═══════════════════════════════════════════════════════════════
    // BORRAR CANCIÓN (ROBUSTO)
    // ═══════════════════════════════════════════════════════════════

    suspend fun deletePhysicalSong(
        context: Context,
        path: String,
        title: String,
        videoId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        var deleted = false
        val normTarget = normalize(title)

        markSongAsDeleted(context, path, title)

        try {
            if (path.isNotEmpty() && !path.startsWith("content://")) {
                val cleanPath = if (path.startsWith("file://")) path.removePrefix("file://") else path
                val f = File(cleanPath)
                if (f.exists() && f.delete()) deleted = true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error borrando archivo directo: ${e.message}")
        }

        try {
            val privateDir = getPrivateMusicDir(context)
            privateDir.listFiles()?.forEach { file ->
                val fileNorm = normalize(file.nameWithoutExtension)
                if (fileNorm.contains(normTarget) || normTarget.contains(fileNorm) ||
                    (videoId != null && file.name.contains(videoId))) {
                    if (file.delete()) deleted = true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error borrando en privado: ${e.message}")
        }

        try {
            val publicDir = getPublicMusicDir()
            if (publicDir.exists()) {
                publicDir.listFiles()?.forEach { file ->
                    val fileNorm = normalize(file.nameWithoutExtension)
                    if (fileNorm.contains(normTarget) || normTarget.contains(fileNorm) ||
                        (videoId != null && file.name.contains(videoId))) {
                        if (file.delete()) {
                            deleted = true
                        } else {
                            val trashFile = File(file.parent, "$TRASH_PREFIX${file.name}")
                            if (file.renameTo(trashFile)) deleted = true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error procesando pública: ${e.message}")
        }

        try {
            val resolver = context.contentResolver
            val selection = "${MediaStore.Audio.Media.TITLE} = ? OR ${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf(title, "%$title%")

            resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Media._ID),
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val ids = mutableListOf<Long>()
                while (cursor.moveToNext()) ids.add(cursor.getLong(idColumn))
                cursor.close()

                for (id in ids) {
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    try {
                        if (resolver.delete(uri, null, null) > 0) deleted = true
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error en MediaStore delete: ${e.message}")
        }

        try {
            val coversDir = getCoversDir(context)
            val safeId = videoId?.replace("[^a-zA-Z0-9_-]".toRegex(), "_") ?: normTarget
            val coverFile = File(coversDir, "cover_${safeId}.jpg")
            if (coverFile.exists()) coverFile.delete()
        } catch (_: Exception) {}

        deleted
    }

    // ═══════════════════════════════════════════════════════════════
    // ESCANEAR BIBLIOTECA
    // ═══════════════════════════════════════════════════════════════

    suspend fun fetchLocalMusic(context: Context): List<LocalSong> = withContext(Dispatchers.IO) {
        val songList = mutableListOf<LocalSong>()
        val resolver: ContentResolver = context.contentResolver
        val addedTitles = mutableSetOf<String>()

        val dbSongs = try {
            AudiophilesDatabase.getInstance(context).downloadedSongDao().getDownloadedSongsList()
        } catch (e: Exception) {
            Log.e(TAG, "Error consultando Room: ${e.message}", e)
            emptyList()
        }

        for (dbSong in dbSongs) {
            if (isSongDeleted(context, dbSong.localFilePath, dbSong.title)) continue

            val cleanPath = if (dbSong.localFilePath.startsWith("file://"))
                dbSong.localFilePath.removePrefix("file://") else dbSong.localFilePath
            val file = File(cleanPath)

            if (!file.exists() || file.length() < 5000) continue

            val cover = getOrCreateCoverFile(
                context = context,
                videoId = dbSong.videoId,
                title = dbSong.title,
                artist = dbSong.artist,
                remoteThumbnailUrl = dbSong.thumbnailUrl,
                filePath = dbSong.localFilePath
            )

            val norm = normalize(dbSong.title)
            addedTitles.add(norm)

            val parsedMs = parseDurationTextToMs(dbSong.durationText)
            val realDuration = getAudioDurationFromFile(dbSong.localFilePath, parsedMs)

            songList.add(
                LocalSong(
                    id = dbSong.downloadedAt,
                    title = dbSong.title,
                    artist = dbSong.artist,
                    path = dbSong.localFilePath,
                    durationMs = realDuration,
                    albumArtUri = cover,
                    isDownloadedFromAudiophiles = true,
                    qualityLabel = "${dbSong.bitrate} ${dbSong.format}"
                )
            )
        }

        try {
            val privateDir = getPrivateMusicDir(context)
            privateDir.listFiles()?.forEach { file ->
                if (!file.isFile) return@forEach
                val ext = file.extension.lowercase()
                if (ext !in listOf("mp3", "m4a", "wav", "flac", "aac")) return@forEach
                if (file.name.startsWith(TRASH_PREFIX)) return@forEach

                val simpleName = file.nameWithoutExtension
                val normName = normalize(simpleName)

                if (isSongDeleted(context, file.absolutePath, simpleName)) return@forEach
                if (addedTitles.any { it.contains(normName) || normName.contains(it) }) return@forEach

                val parts = simpleName.split(" - ")
                val artist = if (parts.size > 1) parts[0].trim() else "Audiophile's Master"
                val title = if (parts.size > 1) parts.subList(1, parts.size).joinToString(" - ").trim() else simpleName

                val cover = getOrCreateCoverFile(
                    context = context,
                    videoId = "priv_${file.lastModified()}",
                    title = title,
                    artist = artist,
                    filePath = file.absolutePath
                )

                val realMs = getAudioDurationFromFile(file.absolutePath, 210000L)
                addedTitles.add(normName)

                songList.add(
                    LocalSong(
                        id = file.lastModified(),
                        title = title,
                        artist = artist,
                        path = file.absolutePath,
                        durationMs = realMs,
                        albumArtUri = cover,
                        isDownloadedFromAudiophiles = true,
                        qualityLabel = "Hi-Fi ${ext.uppercase()}"
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error escaneando carpeta privada: ${e.message}", e)
        }

        if (hasAudioReadPermission(context)) {
            try {
                val publicDir = getPublicMusicDir()
                if (publicDir.exists()) {
                    publicDir.listFiles()?.forEach { file ->
                        if (!file.isFile) return@forEach
                        if (file.name.startsWith(TRASH_PREFIX)) return@forEach
                        val ext = file.extension.lowercase()
                        if (ext !in listOf("mp3", "m4a", "wav", "flac", "aac")) return@forEach

                        val simpleName = file.nameWithoutExtension
                        val normName = normalize(simpleName)

                        if (isSongDeleted(context, file.absolutePath, simpleName)) return@forEach
                        if (addedTitles.any { it.contains(normName) || normName.contains(it) }) return@forEach

                        val parts = simpleName.split(" - ")
                        val artist = if (parts.size > 1) parts[0].trim() else "Desconocido"
                        val title = if (parts.size > 1) parts.subList(1, parts.size).joinToString(" - ").trim() else simpleName

                        val cover = getOrCreateCoverFile(
                            context = context,
                            videoId = "pub_${file.lastModified()}",
                            title = title,
                            artist = artist,
                            filePath = file.absolutePath
                        )

                        val realMs = getAudioDurationFromFile(file.absolutePath, 210000L)
                        addedTitles.add(normName)

                        songList.add(
                            LocalSong(
                                id = file.lastModified(),
                                title = title,
                                artist = artist,
                                path = file.absolutePath,
                                durationMs = realMs,
                                albumArtUri = cover,
                                isDownloadedFromAudiophiles = true,
                                qualityLabel = "Hi-Fi ${ext.uppercase()}"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error escaneando carpeta pública: ${e.message}", e)
            }

            try {
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DURATION
                )
                val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
                val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

                resolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection, selection, null, sortOrder
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                    val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val rawTitle = cursor.getString(titleCol) ?: "Pista"
                        val artist = cursor.getString(artistCol) ?: "Desconocido"
                        val path = cursor.getString(dataCol) ?: ""
                        val durationMs = cursor.getLong(durCol)

                        if (path.contains(TRASH_PREFIX)) continue
                        if (isSongDeleted(context, path, rawTitle)) continue

                        val norm = normalize(rawTitle)
                        if (addedTitles.any { it.contains(norm) || norm.contains(it) }) continue

                        val cleanArtist = if (artist.equals("<unknown>", true) || artist.isBlank()) "Desconocido" else artist
                        val isAudiophile = path.contains("Audiophiles", ignoreCase = true)

                        val cover = getOrCreateCoverFile(
                            context = context,
                            videoId = "ms_$id",
                            title = rawTitle,
                            artist = cleanArtist,
                            filePath = path
                        )

                        addedTitles.add(norm)
                        songList.add(
                            LocalSong(
                                id = id,
                                title = rawTitle,
                                artist = cleanArtist,
                                path = path,
                                durationMs = if (durationMs > 0) durationMs else 210000L,
                                albumArtUri = cover,
                                isDownloadedFromAudiophiles = isAudiophile,
                                qualityLabel = if (isAudiophile) "Hi-Fi" else null
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "MediaStore no accesible: ${e.message}")
            }
        }

        songList
    }
}