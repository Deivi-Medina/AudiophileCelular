package com.example.data.model

import com.example.data.local.ListenLaterEntity
import com.example.data.local.PinnedFavoriteEntity
import com.example.data.local.SongReviewEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

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

// ─────────────────── DISTRIBUCIÓN DE NOTAS (gráfica del perfil) ───────────────────

/** Notas posibles en la app: de ½ en ½, de 0.5★ a 5★ = 10 filas. */
const val RATING_BUCKETS = 10

/** Fila (0..9) que corresponde a una nota de ½ a 5★. */
fun ratingBucketIndex(rating: Float): Int =
    (rating / 0.5f).roundToInt().minus(1).coerceIn(0, RATING_BUCKETS - 1)

/** Nota de la fila [index]: 0 -> 0.5★, 9 -> 5★. */
fun ratingForBucket(index: Int): Float = (index.coerceIn(0, RATING_BUCKETS - 1) + 1) * 0.5f

/** Texto de la nota tal y como se enseña en la gráfica: "½★", "1★", "4½★", "5★". */
fun ratingBucketLabel(index: Int): String {
    val rating = ratingForBucket(index)
    val whole = rating.toInt()
    return when {
        whole == 0 -> "½★"
        rating - whole > 0.1f -> "$whole½★"
        else -> "$whole★"
    }
}

/** Igual que [ratingBucketLabel] pero partiendo de una nota (p.ej. 4.0f -> "4★"). */
fun ratingStarsLabel(rating: Float): String = ratingBucketLabel(ratingBucketIndex(rating))

/** Cuántas reseñas hay con cada nota, de 0.5★ a 5★ (10 posiciones). */
fun List<SongReviewEntity>.ratingHistogram(): List<Int> {
    val counts = IntArray(RATING_BUCKETS)
    forEach { counts[ratingBucketIndex(it.rating)]++ }
    return counts.toList()
}

/**
 * La nota más usada, para resaltar su fila y escribir "Tu nota más usada".
 * Si hay empate gana la nota más alta (y si no hay reseñas, `null`).
 */
fun List<SongReviewEntity>.mostUsedRating(): Float? {
    val histogram = ratingHistogram()
    val maxCount = histogram.maxOrNull() ?: 0
    if (isEmpty() || maxCount == 0) return null
    return ratingForBucket(histogram.indexOfLast { it == maxCount })
}

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
