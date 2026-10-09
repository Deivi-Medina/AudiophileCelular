package com.example.data.reviews

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Reseña de **otra persona** sobre una canción.
 *
 * No es la reseña del dueño (esa vive en Room, `SongReviewEntity`), sino lo que
 * han escrito otros usuarios. Todavía no existe servidor: ver [SongReviewsProvider].
 */
data class PublicSongReview(
    val id: String,
    val songId: String,
    val authorName: String,
    val authorPhotoUrl: String? = null,
    val rating: Float,
    val comment: String,
    val createdAt: Long
)

/**
 * De dónde salen las reseñas de otras personas para una canción.
 *
 * La pantalla del menú del artista (menú del vinilo) ya está cableada a esta
 * interfaz: cuando exista el servidor (Firebase, con `google-services.json`)
 * solo hay que escribir una implementación y asignarla en [SongReviewsProvider].
 */
interface SongReviewsSource {
    fun observeReviews(songId: String): Flow<List<PublicSongReview>>

    companion object {
        /** Fuente vacía reutilizable (sin servidor todavía). */
        val EMPTY: SongReviewsSource = object : SongReviewsSource {
            override fun observeReviews(songId: String): Flow<List<PublicSongReview>> = flowOf(emptyList())
        }
    }
}

/** Implementación de hoy: no hay servidor, así que nunca hay reseñas de otras personas. */
object NoSongReviewsSource : SongReviewsSource by SongReviewsSource.EMPTY

/**
 * Punto único de conexión.
 *
 * La segunda mitad (Firebase) solo tendrá que hacer, al arrancar la app:
 * ```
 * SongReviewsProvider.source = FirestoreSongReviewsSource(...)
 * ```
 * y el menú del artista enseñará las reseñas sin tocar más UI.
 */
object SongReviewsProvider {
    @Volatile
    var source: SongReviewsSource = NoSongReviewsSource
}

/** Estado de la sección de reseñas del menú del artista. */
sealed interface SongReviewsUiState {
    /** Esperando la respuesta de la fuente. */
    data object Loading : SongReviewsUiState

    /** La fuente respondió y no hay ninguna reseña de otras personas. */
    data object Empty : SongReviewsUiState

    /** Hay reseñas que enseñar. */
    data class Content(val reviews: List<PublicSongReview>) : SongReviewsUiState

    /** La fuente falló (p. ej. sin conexión). */
    data class Error(val message: String) : SongReviewsUiState
}

/** Traduce lo que llega de la fuente al estado que pinta la UI. */
fun reviewsUiStateOf(reviews: List<PublicSongReview>?): SongReviewsUiState = when {
    reviews == null -> SongReviewsUiState.Loading
    reviews.isEmpty() -> SongReviewsUiState.Empty
    else -> SongReviewsUiState.Content(reviews.sortedByDescending { it.createdAt })
}
