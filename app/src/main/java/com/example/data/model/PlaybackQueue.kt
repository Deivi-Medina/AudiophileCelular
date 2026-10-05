package com.example.data.model

import com.example.data.local.PlaylistSongEntity

enum class RepeatMode { OFF, ALL, ONE }

sealed class PlaybackQueueItem {
    data class Stream(val track: YouTubeTrackResult) : PlaybackQueueItem()
    data class Local(val song: LocalSong) : PlaybackQueueItem()
}

data class PlaybackQueue(
    val items: List<PlaybackQueueItem> = emptyList(),
    val originalItems: List<PlaybackQueueItem> = emptyList(),
    val currentIndex: Int = -1,
    val playlistId: Long? = null,
    val playlistName: String? = null
) {
    val current: PlaybackQueueItem?
        get() = items.getOrNull(currentIndex)

    val isActive: Boolean
        get() = items.isNotEmpty() && currentIndex in items.indices

    companion object {
        val EMPTY = PlaybackQueue()
    }
}

fun PlaylistSongEntity.toQueueItem(availableLocals: List<LocalSong>): PlaybackQueueItem {
    return if (isLocal) {
        val matched = availableLocals.find { it.id.toString() == songId }
        PlaybackQueueItem.Local(
            matched ?: LocalSong(
                id = songId.toLongOrNull() ?: id,
                title = title,
                artist = artist,
                path = audioUrl ?: "",
                durationMs = 210_000L,
                albumArtUri = coverUrl,
                isDownloadedFromAudiophiles = true
            )
        )
    } else {
        PlaybackQueueItem.Stream(
            YouTubeTrackResult(
                videoId = songId,
                title = title,
                channelOrArtist = artist,
                durationText = durationText,
                durationMs = 210_000L,
                thumbnailUrl = coverUrl ?: "",
                previewAudioUrl = audioUrl,
                disponiblesQualities = AudioQuality.DEFAULT_QUALITIES
            )
        )
    }
}