package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.reviews.FirestoreSongReviewsSource
import com.example.data.reviews.SongReviewsProvider
import com.example.data.social.FirestoreFollowCountersSource
import com.example.data.social.FollowCountersProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Único sitio donde se enciende Firebase.
 *
 * Se llama una vez, al arrancar la app (ver `AudiophileApp.onCreate`).
 *
 * - **Si Firebase está inicializado** (es decir, el dueño ya puso
 *   `google-services.json` y el plugin `com.google.gms.google-services` está
 *   activado): conecta las reseñas compartidas y los contadores de
 *   seguidores / seguidos a Firestore.
 * - **Si no lo está**: no hace absolutamente nada. La app se queda como está hoy
 *   — `SongReviewsProvider.source` sigue siendo [com.example.data.reviews.NoSongReviewsSource]
 *   (sección de reseñas vacía) y `FollowCountersProvider.source` sigue siendo
 *   [com.example.data.social.FollowCountersSource.NONE] (0 seguidores, 0 seguidos).
 *   Sin cuelgues, sin errores en pantalla y sin datos inventados.
 *
 * Para encenderlo no hay que tocar código: ver `FIREBASE_ACTIVACION.md`.
 */
object FirebaseBootstrap {

    private const val TAG = "FirebaseBootstrap"

    /** `true` cuando la parte de Firebase ya quedó conectada. */
    @Volatile
    var isInstalled: Boolean = false
        private set

    /**
     * Conecta las fuentes de Firebase si hay configuración.
     *
     * @return `true` si Firebase quedó conectado; `false` si no hay configuración
     *   (y por tanto la app sigue sin backend, como hoy).
     */
    fun install(context: Context): Boolean {
        if (isInstalled) return true

        val appContext = context.applicationContext
        if (!FirebaseStatus.isConfigured(appContext)) {
            Log.i(TAG, "Sin configuración de Firebase: se mantienen las fuentes locales (0 / vacío).")
            return false
        }

        return try {
            val firestore = FirebaseFirestore.getInstance()
            SongReviewsProvider.source = FirestoreSongReviewsSource(firestore)
            FollowCountersProvider.source = FirestoreFollowCountersSource(firestore)
            isInstalled = true
            Log.i(TAG, "Firebase conectado: reseñas compartidas y contadores sociales activos.")
            true
        } catch (t: Throwable) {
            // Si Firestore no se puede crear, se vuelve al comportamiento de hoy.
            Log.e(TAG, "No se pudo conectar Firebase: ${t.message}")
            isInstalled = false
            false
        }
    }
}
