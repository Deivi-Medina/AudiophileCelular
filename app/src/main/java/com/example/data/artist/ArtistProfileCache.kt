package com.example.data.artist

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.ArtistProfileUiState
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Caché de fichas de artista (foto + descripción) para que abrir el menú del
 * vinilo no vuelva a descargar lo mismo cada vez.
 *
 * Dos niveles:
 * - **Memoria** ([memory]): durante la sesión. Guarda tanto las fichas que
 *   llegaron ([ArtistProfileUiState.Available]) como las respuestas "no hay
 *   artículo" ([ArtistProfileUiState.Unavailable] con motivo `NoProfile` o
 *   `NoArtist`), que también son definitivas mientras la sesión dure.
 * - **Disco** ([SharedPreferences]): solo las fichas que llegaron, que son las
 *   que merece la pena conservar entre arranques. Así el menú enseña la foto y
 *   la descripción de un artista ya visto **sin conexión**.
 *
 * Un fallo de red ([ArtistProfileUiState.Unavailable] con motivo `Offline`) **no
 * se cachea nunca**: es temporal y el siguiente intento debe volver a la red.
 *
 * Clave de caché: el nombre del artista normalizado (minúsculas, sin acentos y
 * sin signos), de modo que "Bad Bunny", "bad bunny" y "Bad  Bunny" comparten
 * entrada. El JSON guardado en disco es una lista ordenada de entradas y se
 * recorta a [MAX_ENTRIES] por el principio (las fichas más antiguas salen).
 */
class ArtistProfileCache(
    context: Context,
    private val maxEntries: Int = MAX_ENTRIES
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val memory = ConcurrentHashMap<String, ArtistProfileUiState>()

    @Volatile
    private var diskLoaded = false

    /** Ficha cacheada de [artistName], o `null` si toca ir a la red. */
    fun get(artistName: String): ArtistProfileUiState? {
        val key = cacheKeyOf(artistName)
        if (key.isEmpty()) return null
        memory[key]?.let { return it }
        loadDiskOnce()
        return memory[key]
    }

    /**
     * Guarda el resultado de una consulta. Los fallos de red se descartan y los
     * `Available` sin ningún dato (ni foto ni texto) también: no aportan nada.
     */
    fun put(artistName: String, state: ArtistProfileUiState) {
        val key = cacheKeyOf(artistName)
        if (key.isEmpty()) return
        when (state) {
            is ArtistProfileUiState.Available -> {
                if (state.photoUrl.isNullOrBlank() && state.description.isNullOrBlank()) return
                memory[key] = state
                persist(key, state)
            }
            is ArtistProfileUiState.Unavailable -> {
                if (state.reason == ArtistProfileUiState.Unavailable.Reason.Offline) return
                memory[key] = state
            }
            ArtistProfileUiState.Loading -> Unit
        }
    }

    // ─────────────────────────────── Disco ───────────────────────────────

    /** Lee el JSON de disco una sola vez; después todo sale de memoria. */
    private fun loadDiskOnce() {
        if (diskLoaded) return
        synchronized(this) {
            if (diskLoaded) return
            val raw = prefs.getString(KEY_ENTRIES, null)
            if (!raw.isNullOrBlank()) {
                runCatching {
                    val entries = JSONArray(raw)
                    for (i in 0 until entries.length()) {
                        val entry = entries.optJSONObject(i) ?: continue
                        val key = entry.optString(KEY_NAME)
                        if (key.isBlank()) continue
                        memory[key] = ArtistProfileUiState.Available(
                            photoUrl = entry.optString(KEY_PHOTO).takeIf { it.isNotBlank() },
                            description = entry.optString(KEY_DESCRIPTION).takeIf { it.isNotBlank() }
                        )
                    }
                }
            }
            diskLoaded = true
        }
    }

    private fun persist(key: String, state: ArtistProfileUiState.Available) {
        synchronized(this) {
            loadDiskOnce()
            val entries = JSONArray()
            // Lo que ya hubiera en disco, menos esta misma clave.
            val previous = runCatching { JSONArray(prefs.getString(KEY_ENTRIES, "[]") ?: "[]") }
                .getOrDefault(JSONArray())
            for (i in 0 until previous.length()) {
                val entry = previous.optJSONObject(i) ?: continue
                if (entry.optString(KEY_NAME) == key) continue
                entries.put(entry)
            }
            entries.put(
                JSONObject()
                    .put(KEY_NAME, key)
                    .put(KEY_PHOTO, state.photoUrl.orEmpty())
                    .put(KEY_DESCRIPTION, state.description.orEmpty())
            )
            // Recorte: fuera las más antiguas.
            val trimmed = JSONArray()
            val from = (entries.length() - maxEntries).coerceAtLeast(0)
            for (i in from until entries.length()) trimmed.put(entries.opt(i))
            prefs.edit().putString(KEY_ENTRIES, trimmed.toString()).apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "audiophiles_artist_profile_cache"
        private const val KEY_ENTRIES = "artist_profiles"
        private const val KEY_NAME = "name"
        private const val KEY_PHOTO = "photo"
        private const val KEY_DESCRIPTION = "description"

        /** Tope de fichas guardadas en disco (son unos pocos cientos de bytes cada una). */
        const val MAX_ENTRIES = 40

        /** Clave de caché insensible a mayúsculas, acentos y signos de puntuación. */
        fun cacheKeyOf(artistName: String): String =
            Normalizer.normalize(artistName, Normalizer.Form.NFD)
                .replace(Regex("\\p{Mn}+"), "")
                .lowercase(Locale.ROOT)
                .replace(Regex("[^a-z0-9]+"), " ")
                .trim()
    }
}
