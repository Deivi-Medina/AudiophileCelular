package com.example.data.reviews

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Reseñas de **otras personas** guardadas en Firestore.
 *
 * Estructura esperada (una reseña = un documento):
 * ```
 * songReviews/{songId}/reviews/{reviewId}
 *   authorName:     "Ana"              // obligatorio
 *   authorPhotoUrl: "https://…"        // opcional
 *   rating:         4.5                // 0..5 (number)
 *   comment:        "Un discazo"       // texto
 *   createdAt:      1730000000000      // millis (o Timestamp)
 * ```
 *
 * Reglas de comportamiento:
 * - Escucha en vivo ([callbackFlow] + `addSnapshotListener`): si alguien publica
 *   una reseña mientras el menú está abierto, aparece sin recargar.
 * - Si `songId` está vacío devuelve la lista vacía **sin tocar la red**.
 * - Si Firestore falla, el flujo se cierra con el error: la UI ya sabe pintar
 *   [SongReviewsUiState.Error] y no se inventa ninguna reseña.
 * - `createdAt` es obligatorio para que el `orderBy` no descarte documentos:
 *   está documentado en `FIREBASE_ACTIVACION.md`.
 *
 * Esta clase **no** decide si se usa: eso lo hace [SongReviewsProvider] a través
 * de `FirebaseBootstrap`, y solo si Firebase está inicializado.
 */
class FirestoreSongReviewsSource(
    private val firestore: FirebaseFirestore,
    private val maxReviews: Long = MAX_REVIEWS
) : SongReviewsSource {

    override fun observeReviews(songId: String): Flow<List<PublicSongReview>> = callbackFlow {
        if (songId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = try {
            firestore.collection(COLLECTION_SONG_REVIEWS)
                .document(songId)
                .collection(COLLECTION_REVIEWS)
                .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
                .limit(maxReviews)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val reviews = snapshot?.documents.orEmpty()
                        .mapNotNull { document -> document.toPublicSongReview(songId) }
                    trySend(reviews)
                }
        } catch (t: Throwable) {
            // Sin configuración / sin red / listener rechazado: la UI pinta el error.
            close(t)
            return@callbackFlow
        }

        awaitClose { registration.remove() }
    }

    companion object {
        /** Colección raíz: `songReviews/{songId}/reviews`. */
        const val COLLECTION_SONG_REVIEWS = "songReviews"
        const val COLLECTION_REVIEWS = "reviews"

        /** Campos del documento (los nombres alternativos se aceptan por comodidad). */
        const val FIELD_CREATED_AT = "createdAt"

        /** Tope de reseñas que se traen por canción. */
        const val MAX_REVIEWS = 50L
    }
}

/**
 * Traduce un documento de Firestore a [PublicSongReview]. Devuelve `null` si el
 * documento no trae nada aprovechable (ni autor, ni comentario, ni nota).
 *
 * Todo se lee con `runCatching` porque un campo con el tipo equivocado en
 * Firestore lanza excepción al convertirlo, y un documento mal escrito de otra
 * persona no puede tumbar el menú del vinilo.
 */
private fun DocumentSnapshot.toPublicSongReview(songId: String): PublicSongReview? {
    val author = stringField("authorName", "author", "displayName").orEmpty().trim()
    val comment = stringField("comment", "text", "body").orEmpty().trim()
    val rating = doubleField("rating")?.toFloat() ?: 0f
    if (author.isEmpty() && comment.isEmpty() && rating <= 0f) return null

    return PublicSongReview(
        id = id,
        songId = songId,
        authorName = author.ifEmpty { "Anónimo" },
        authorPhotoUrl = stringField("authorPhotoUrl", "authorPhoto", "photoUrl"),
        rating = rating.coerceIn(0f, 5f),
        comment = comment,
        createdAt = createdAtField()
    )
}

private fun DocumentSnapshot.stringField(vararg names: String): String? {
    for (name in names) {
        val value = runCatching { getString(name) }.getOrNull()
        if (!value.isNullOrBlank()) return value
    }
    return null
}

private fun DocumentSnapshot.doubleField(name: String): Double? =
    runCatching { getDouble(name) }.getOrNull()
        ?: runCatching { getLong(name)?.toDouble() }.getOrNull()

/** `createdAt` como millis: acepta number (millis) o Timestamp. */
private fun DocumentSnapshot.createdAtField(): Long =
    runCatching { getLong("createdAt") }.getOrNull()
        ?: runCatching { getTimestamp("createdAt")?.toDate()?.time }.getOrNull()
        ?: 0L
