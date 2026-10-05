package com.example.data.model

data class YouTubeTrackResult(
    val videoId: String,
    val title: String,
    val channelOrArtist: String,
    val durationText: String,
    val durationMs: Long = 0L,
    val thumbnailUrl: String,
    val previewAudioUrl: String? = null,
    val disponiblesQualities: List<AudioQuality> = AudioQuality.DEFAULT_QUALITIES
)
