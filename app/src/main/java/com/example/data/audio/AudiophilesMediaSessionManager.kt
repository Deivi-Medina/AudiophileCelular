package com.example.data.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.net.URL

class AudiophilesMediaSessionManager(
    private val context: Context,
    private val onPlayAction: () -> Unit,
    private val onPauseAction: () -> Unit,
    private val onNextAction: () -> Unit,
    private val onPrevAction: () -> Unit,
    private val onSeekAction: (Long) -> Unit
) {
    private val TAG = "AudiophilesMediaSession"
    private val CHANNEL_ID = "audiophiles_spotify_style_channel"
    private val NOTIFICATION_ID = 4040

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var mediaSession: MediaSession? = null
    private var isPlaying = false
    private var currentTitle = ""
    private var currentArtist = ""
    private var currentDuration = 210000L
    private var currentCoverBitmap: Bitmap? = null
    private var lastCoverUrl: String? = null

    // ─── FIX: Referencias persistentes de foco de audio ───
    private var currentAudioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus: Boolean = false
    private var isRequestingFocus: Boolean = false

    // Tras una interrupción breve solo se reanuda si estaba sonando; si la pausó el usuario, sigue en pausa.
    private var resumeOnFocusGain = false

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                resumeOnFocusGain = isPlaying
                hasAudioFocus = false
                if (isPlaying) onPauseAction()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                resumeOnFocusGain = false
                if (isPlaying) onPauseAction()
                abandonAudioFocus()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                if (resumeOnFocusGain) {
                    resumeOnFocusGain = false
                    onPlayAction()
                }
            }
        }
    }

    init {
        createNotificationChannel()
        setupMediaSession()
        setupReceiverCallbacks()
    }

    private fun setupReceiverCallbacks() {
        MediaNotificationReceiver.onPlayCallback = { onPlayAction() }
        MediaNotificationReceiver.onPauseCallback = { onPauseAction() }
        MediaNotificationReceiver.onNextCallback = { onNextAction() }
        MediaNotificationReceiver.onPrevCallback = { onPrevAction() }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Audiophile's Hi-Fi Player",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controles multimedia estilo Spotify de audio en alta fidelidad"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupMediaSession() {
        try {
            mediaSession = MediaSession(context, "AudiophilesMediaSession").apply {
                setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)

                setCallback(object : MediaSession.Callback() {
                    override fun onPlay() {
                        super.onPlay()
                        onPlayAction()
                    }

                    override fun onPause() {
                        super.onPause()
                        onPauseAction()
                    }

                    override fun onSkipToNext() {
                        super.onSkipToNext()
                        onNextAction()
                    }

                    override fun onSkipToPrevious() {
                        super.onSkipToPrevious()
                        onPrevAction()
                    }

                    override fun onSeekTo(pos: Long) {
                        super.onSeekTo(pos)
                        onSeekAction(pos)
                    }

                    override fun onStop() {
                        super.onStop()
                        onPauseAction()
                    }
                })

                val activityIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    activityIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                setSessionActivity(pendingIntent)
                isActive = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando MediaSession: ${e.message}", e)
        }
    }

    /**
     * FIX: Pide foco de audio REUTILIZANDO el mismo AudioFocusRequest.
     * Si ya lo tenemos, no vuelve a pedirlo → evita AUDIOFOCUS_LOSS → evita la pausa fantasma.
     */
    fun requestAudioFocus(): Boolean {
        // Si ya tenemos el foco, devolver inmediatamente true sin re-pedirlo
        if (hasAudioFocus) {
            return true
        }

        // Evitar peticiones concurrentes
        if (isRequestingFocus) {
            return false
        }
        isRequestingFocus = true

        val result = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Reutilizar la request existente si ya está creada
                val focusRequest = currentAudioFocusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAcceptsDelayedFocusGain(true)
                    .setWillPauseWhenDucked(false)
                    .setOnAudioFocusChangeListener(focusChangeListener)
                    .build()
                    .also { currentAudioFocusRequest = it }

                audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    focusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requestAudioFocus: ${e.message}")
            false
        } finally {
            isRequestingFocus = false
        }

        hasAudioFocus = result
        return result
    }

    /**
     * FIX: Abandonar el foco correctamente al liberar el reproductor.
     */
    fun abandonAudioFocus() {
        if (!hasAudioFocus) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                currentAudioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(focusChangeListener)
            }
        } catch (_: Exception) {}
        hasAudioFocus = false
    }

    fun updatePlaybackState(playing: Boolean, positionMs: Long, speed: Float = 1.0f) {
        this.isPlaying = playing
        val state = if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        val actions = PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_SEEK_TO or
                PlaybackState.ACTION_STOP

        val playbackSpeed = if (playing) speed else 0.0f
        val playbackState = PlaybackState.Builder()
            .setActions(actions)
            .setState(state, positionMs, playbackSpeed, SystemClock.elapsedRealtime())
            .build()

        mediaSession?.setPlaybackState(playbackState)
        showNotification()
    }

    fun updateMetadata(title: String, artist: String, durationMs: Long, coverUrl: String?) {
        this.currentTitle = title
        this.currentArtist = artist
        this.currentDuration = if (durationMs > 0) durationMs else 210000L

        val initialCover = currentCoverBitmap ?: getDefaultArtBitmap()

        val metadataBuilder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, "Audiophile's Hi-Fi")
            .putString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST, "Audiophile's")
            .putLong(MediaMetadata.METADATA_KEY_DURATION, this.currentDuration)
            .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, initialCover)

        mediaSession?.setMetadata(metadataBuilder.build())

        if (coverUrl != lastCoverUrl) {
            lastCoverUrl = coverUrl
            loadCoverBitmap(coverUrl)
        }

        showNotification()
    }

    private fun loadCoverBitmap(coverUrl: String?) {
        if (coverUrl.isNullOrEmpty()) {
            currentCoverBitmap = null
            showNotification()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bitmap: Bitmap? = when {
                    coverUrl.startsWith("http://") || coverUrl.startsWith("https://") -> {
                        val stream = URL(coverUrl).openStream()
                        BitmapFactory.decodeStream(stream)
                    }
                    coverUrl.startsWith("content://") -> {
                        val uri = Uri.parse(coverUrl)
                        context.contentResolver.openInputStream(uri)?.use {
                            BitmapFactory.decodeStream(it)
                        }
                    }
                    coverUrl.startsWith("file://") -> {
                        val path = Uri.parse(coverUrl).path
                        if (path != null && File(path).exists()) {
                            BitmapFactory.decodeFile(path)
                        } else null
                    }
                    else -> {
                        val file = File(coverUrl)
                        if (file.exists()) {
                            BitmapFactory.decodeFile(file.absolutePath)
                        } else null
                    }
                }

                if (bitmap != null) {
                    currentCoverBitmap = bitmap
                    val updatedMetadata = MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_TITLE, currentTitle)
                        .putString(MediaMetadata.METADATA_KEY_ARTIST, currentArtist)
                        .putString(MediaMetadata.METADATA_KEY_ALBUM, "Audiophile's Hi-Fi")
                        .putString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST, "Audiophile's")
                        .putLong(MediaMetadata.METADATA_KEY_DURATION, currentDuration)
                        .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmap)
                    mediaSession?.setMetadata(updatedMetadata.build())
                    showNotification()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando carátula para notificación: ${e.message}")
            }
        }
    }

    private fun showNotification() {
        if (currentTitle.isEmpty()) return

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevIntent = Intent(context, MediaNotificationReceiver::class.java).apply {
            action = MediaNotificationReceiver.ACTION_PREV
        }
        val prevPendingIntent = PendingIntent.getBroadcast(
            context,
            3,
            prevIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseTitle = if (isPlaying) "Pausar" else "Reproducir"
        val playPauseIntent = Intent(context, MediaNotificationReceiver::class.java).apply {
            action = if (isPlaying) MediaNotificationReceiver.ACTION_PAUSE else MediaNotificationReceiver.ACTION_PLAY
        }
        val playPausePendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            playPauseIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextIntent = Intent(context, MediaNotificationReceiver::class.java).apply {
            action = MediaNotificationReceiver.ACTION_NEXT
        }
        val nextPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            nextIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val cover = currentCoverBitmap ?: getDefaultArtBitmap()

        val mediaStyle = Notification.MediaStyle().apply {
            mediaSession?.let { setMediaSession(it.sessionToken) }
            setShowActionsInCompactView(0, 1, 2)
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }

        builder
            .setSmallIcon(R.drawable.audiophiles_icon)
            .setLargeIcon(cover)
            .setContentTitle(currentTitle)
            .setContentText(currentArtist)
            .setSubText("Audiophile's • 320k Hi-Fi")
            .setContentIntent(contentPendingIntent)
            .setStyle(mediaStyle)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setAutoCancel(!isPlaying)
            .setOnlyAlertOnce(true)
            .setColor(0xFF8B5CF6.toInt())

        // setColorized requiere API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setColorized(true)
        }

        builder.addAction(
            Notification.Action.Builder(
                Icon.createWithResource(context, android.R.drawable.ic_media_previous),
                "Anterior",
                prevPendingIntent
            ).build()
        )

        builder.addAction(
            Notification.Action.Builder(
                Icon.createWithResource(context, playPauseIcon),
                playPauseTitle,
                playPausePendingIntent
            ).build()
        )

        builder.addAction(
            Notification.Action.Builder(
                Icon.createWithResource(context, android.R.drawable.ic_media_next),
                "Siguiente",
                nextPendingIntent
            ).build()
        )

        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando notificación multimedia: ${e.message}")
        }
    }

    private fun getDefaultArtBitmap(): Bitmap {
        val size = 512
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                0xFF8B5CF6.toInt(),
                0xFF13091F.toInt(),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        val symbolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 150f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("♬", (size / 2).toFloat(), (size / 2 + 50).toFloat(), symbolPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x55FFFFFF
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }
        canvas.drawRect(4f, 4f, size.toFloat() - 4f, size.toFloat() - 4f, borderPaint)

        return bitmap
    }

    fun dismissNotification() {
        try {
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            dismissNotification()
            abandonAudioFocus()
            mediaSession?.isActive = false
            mediaSession?.release()
        } catch (_: Exception) {}
        mediaSession = null
        MediaNotificationReceiver.onPlayCallback = null
        MediaNotificationReceiver.onPauseCallback = null
        MediaNotificationReceiver.onNextCallback = null
        MediaNotificationReceiver.onPrevCallback = null
    }
}