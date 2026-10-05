package com.example.data.model

data class LocalSong(
    val id: Long,
    val title: String,
    val artist: String,
    val path: String,
    val durationMs: Long = 0L,
    val albumArtUri: String? = null,
    val isDownloadedFromAudiophiles: Boolean = false,
    val qualityLabel: String? = null
)
