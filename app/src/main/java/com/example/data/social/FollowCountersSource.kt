package com.example.data.social

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Seguidores y seguidos del usuario de la app.
 *
 * La cabecera del perfil solo conoce esta interfaz, así que se puede cambiar la
 * fuente (o falsearla en pruebas) sin tocar la pantalla.
 */
interface FollowCountersSource {

    /** Cuántas personas siguen al usuario. */
    fun observeFollowersCount(): Flow<Int>

    /** A cuántas personas sigue el usuario. */
    fun observeFollowingCount(): Flow<Int>

    /** Ids de las personas que sigue (vacío si no hay backend o no sigue a nadie). */
    fun observeFollowingIds(): Flow<List<String>>

    companion object {
        /**
         * Fuente de hoy: no hay backend conectado, así que cero seguidores, cero
         * seguidos y lista vacía. **No se inventan datos.**
         */
        val NONE: FollowCountersSource = object : FollowCountersSource {
            override fun observeFollowersCount(): Flow<Int> = flowOf(0)
            override fun observeFollowingCount(): Flow<Int> = flowOf(0)
            override fun observeFollowingIds(): Flow<List<String>> = flowOf(emptyList())
        }
    }
}

/**
 * Punto único de conexión (mismo patrón que `SongReviewsProvider`):
 * `FirebaseBootstrap` lo cambia a la implementación de Firestore **solo** si
 * Firebase está inicializado. Si no lo está, se queda en [FollowCountersSource.NONE]
 * y la app se comporta exactamente como antes.
 */
object FollowCountersProvider {
    @Volatile
    var source: FollowCountersSource = FollowCountersSource.NONE
}

/**
 * Seguidores / seguidos leídos de Firestore.
 *
 * Estructura esperada:
 * ```
 * users/{uid}                 → followers: 12, following: 34 (number)
 * users/{uid}/following/{id}  → { since: 1730000000000 }   // opcional, para los ids
 * ```
 *
 * - `uid` sale de la sesión de Firebase Auth (`FirebaseAuth.currentUser`).
 *   **No se fuerza ningún inicio de sesión**: si no hay usuario, devuelve
 *   0 / lista vacía y no toca la red.
 * - Si el documento no existe todavía, devuelve 0 (usuario nuevo), no un error.
 * - Si Firestore falla, devuelve 0 / lista vacía: la cabecera del perfil nunca
 *   muestra un error por esto.
 */
class FirestoreFollowCountersSource(
    private val firestore: FirebaseFirestore,
    private val uidProvider: () -> String? = { currentUserId() }
) : FollowCountersSource {

    override fun observeFollowersCount(): Flow<Int> =
        observeUserDocument(
            fallback = 0,
            mapper = { it.numberField(FIELD_FOLLOWERS)?.toInt() ?: 0 }
        )

    override fun observeFollowingCount(): Flow<Int> =
        observeUserDocument(
            fallback = 0,
            mapper = { it.numberField(FIELD_FOLLOWING)?.toInt() ?: 0 }
        )

    override fun observeFollowingIds(): Flow<List<String>> = callbackFlow {
        val uid = uidProvider()
        if (uid.isNullOrBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = try {
            firestore.collection(COLLECTION_USERS)
                .document(uid)
                .collection(COLLECTION_FOLLOWING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    trySend(snapshot?.documents.orEmpty().map { it.id })
                }
        } catch (t: Throwable) {
            close(t)
            return@callbackFlow
        }

        awaitClose { registration.remove() }
    }

    /** Escucha `users/{uid}` y traduce el documento; cualquier fallo → [fallback]. */
    private fun <T> observeUserDocument(fallback: T, mapper: (DocumentSnapshot) -> T): Flow<T> =
        callbackFlow {
            val uid = uidProvider()
            if (uid.isNullOrBlank()) {
                trySend(fallback)
                close()
                return@callbackFlow
            }

            val registration = try {
                firestore.collection(COLLECTION_USERS)
                    .document(uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(fallback)
                            return@addSnapshotListener
                        }
                        val value = if (snapshot == null || !snapshot.exists()) {
                            fallback
                        } else {
                            runCatching { mapper(snapshot) }.getOrDefault(fallback)
                        }
                        trySend(value)
                    }
            } catch (t: Throwable) {
                close(t)
                return@callbackFlow
            }

            awaitClose { registration.remove() }
        }

    companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_FOLLOWING = "following"

        /** Campos del contador en `users/{uid}`. */
        const val FIELD_FOLLOWERS = "followers"
        const val FIELD_FOLLOWING = "following"

        /**
         * Usuario con sesión iniciada, si la hay. Sin `google-services.json` no
         * hay app de Firebase, así que esto devuelve `null` y no se pide nada.
         */
        private fun currentUserId(): String? = runCatching {
            FirebaseAuth.getInstance().currentUser?.uid
        }.getOrNull()
    }
}

private fun DocumentSnapshot.numberField(name: String): Double? =
    runCatching { getDouble(name) }.getOrNull()
        ?: runCatching { getLong(name)?.toDouble() }.getOrNull()
