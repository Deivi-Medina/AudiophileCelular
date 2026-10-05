package com.example.data.model

import com.example.data.local.ListenLaterEntity
import com.example.data.local.PinnedFavoriteEntity
import com.example.data.local.SongReviewEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Referencia mínima a una canción (local o de YouTube) para reseñarla, fijarla o guardarla. */
data class SongRef(
    val songId: String,
    val title: String,
    val artist: String,
    val coverUrl: String?
)

fun SongReviewEntity.toSongRef() = SongRef(songId, songTitle, artist, coverUrl)
fun ListenLaterEntity.toSongRef() = SongRef(songId, songTitle, artist, coverUrl)
fun PinnedFavoriteEntity.toSongRef() = SongRef(songId, songTitle, artist, coverUrl)

data class ReviewMonth(val label: String, val reviews: List<SongReviewEntity>)

fun List<SongReviewEntity>.averageRating(): Float =
    if (isEmpty()) 0f else map { it.rating }.average().toFloat()

/** Diario: reseñas agrupadas por mes ("Octubre 2026"), de la más reciente a la más antigua. */
fun List<SongReviewEntity>.groupByMonth(locale: Locale = Locale.forLanguageTag("es")): List<ReviewMonth> {
    val calendar = Calendar.getInstance()
    val formatter = SimpleDateFormat("MMMM yyyy", locale)
    return sortedByDescending { it.createdAt }
        .groupBy { review ->
            calendar.timeInMillis = review.createdAt
            calendar.get(Calendar.YEAR) * 12 + calendar.get(Calendar.MONTH)
        }
        .map { (_, reviews) ->
            val label = formatter.format(Date(reviews.first().createdAt))
            ReviewMonth(label.replaceFirstChar { it.titlecase(locale) }, reviews)
        }
}
