package com.example.data.model

data class AudioQuality(
    val bitrate: String,
    val format: String,
    val label: String
) {
    companion object {
        val DEFAULT_QUALITIES = listOf(
            AudioQuality(bitrate = "320 kbps", format = "MP3", label = "320 kbps MP3 (Hi-Fi)"),
            AudioQuality(bitrate = "256 kbps", format = "AAC", label = "256 kbps AAC (Master)"),
            AudioQuality(bitrate = "192 kbps", format = "MP3", label = "192 kbps MP3 (Standard)"),
            AudioQuality(bitrate = "FLAC", format = "FLAC", label = "Lossless FLAC (Studio)")
        )
    }
}
