package com.example.data.model

import com.example.data.local.SongReviewEntity

data class SongReviewSummary(
    val songId: String,
    val songTitle: String,
    val artist: String,
    val reviewCount: Int,
    val averageRating: Float,
    val lastReviewedAt: Long
)

fun List<SongReviewEntity>.averageRating(): Float =
    if (isEmpty()) 0f else map { it.rating }.average().toFloat()

/** Agrupa las reseñas por canción, de la reseñada más recientemente a la más antigua. */
fun List<SongReviewEntity>.summarizeBySong(): List<SongReviewSummary> =
    groupBy { it.songId }
        .map { (songId, reviews) ->
            val latest = reviews.maxBy { it.createdAt }
            SongReviewSummary(
                songId = songId,
                songTitle = latest.songTitle,
                artist = latest.artist,
                reviewCount = reviews.size,
                averageRating = reviews.averageRating(),
                lastReviewedAt = latest.createdAt
            )
        }
        .sortedByDescending { it.lastReviewedAt }
