package com.example.data.artist

import android.content.Context
import com.example.data.model.ArtistProfileUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Cliente HTTP de las llamadas a Wikipedia: tiempos cortos (la ficha es un
 * extra del menú, no puede tener al usuario esperando) y sin reintentos.
 */
private fun defaultArtistHttpClient(): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .followRedirects(true)
    .build()

/**
 * De dónde sale la ficha (foto + descripción) del artista de una canción.
 *
 * La UI del menú del vinilo solo conoce esta interfaz, así que se puede cambiar
 * la fuente (o falsearla en pruebas) sin tocar la pantalla.
 */
interface ArtistProfileSource {
    /** Devuelve la ficha de [artistName]; nunca lanza: los fallos son estados. */
    suspend fun load(artistName: String): ArtistProfileUiState

    companion object {
        /** Fuente vacía: siempre "no hay ficha". Útil para previews y pruebas. */
        val NONE: ArtistProfileSource = object : ArtistProfileSource {
            override suspend fun load(artistName: String): ArtistProfileUiState =
                ArtistProfileUiState.Unavailable(ArtistProfileUiState.Unavailable.Reason.NoProfile)
        }
    }
}

/**
 * Punto único de conexión, con el mismo patrón que `SongReviewsProvider`:
 * la pantalla pide la fuente y aquí se decide cuál es.
 */
object ArtistProfileProvider {
    /** Sustituto opcional (pruebas / previews). Si no es `null`, manda él. */
    @Volatile
    var customSource: ArtistProfileSource? = null

    @Volatile
    private var instance: ArtistProfileSource? = null

    /** Fuente real de la app: Wikipedia en español, creada una sola vez. */
    fun of(context: Context): ArtistProfileSource {
        customSource?.let { return it }
        instance?.let { return it }
        return synchronized(this) {
            instance ?: WikipediaArtistProfileSource(context.applicationContext).also { instance = it }
        }
    }
}

/**
 * Ficha del artista desde **Wikipedia en español**, que es pública y no pide
 * ninguna clave:
 *
 * 1. Búsqueda del nombre del artista para dar con el título exacto del artículo:
 *    `GET https://es.wikipedia.org/w/api.php?action=query&list=search&srsearch=…&format=json&srlimit=1`
 * 2. Resumen del artículo por su título:
 *    `GET https://es.wikipedia.org/api/rest_v1/page/summary/<Titulo>`
 *    → `extract` (descripción) y `thumbnail.source` / `originalimage.source` (foto).
 *
 * Detalles que importan:
 * - Wikimedia exige una cabecera `User-Agent` descriptiva: va en las dos llamadas.
 * - Las peticiones usan el mismo stack HTTP que el resto de la app (OkHttp).
 * - El JSON se lee con `org.json`, que ya usa `YouTubeAudioEngine`.
 * - Devuelve [ArtistProfileUiState.Unavailable] con motivo `Offline` si falla la red,
 *   `NoArtist` si la canción no trae nombre de artista y `NoProfile` si no hay
 *   artículo que corresponda. **Nunca inventa datos.**
 * - Todo pasa por [ArtistProfileCache]: solo se va a la red la primera vez.
 */
class WikipediaArtistProfileSource(
    context: Context,
    private val client: OkHttpClient = defaultArtistHttpClient(),
    private val cache: ArtistProfileCache = ArtistProfileCache(context)
) : ArtistProfileSource {

    override suspend fun load(artistName: String): ArtistProfileUiState =
        withContext(Dispatchers.IO) {
            val artist = artistName.trim()
            if (artist.isBlank()) {
                return@withContext ArtistProfileUiState.Unavailable(
                    ArtistProfileUiState.Unavailable.Reason.NoArtist
                )
            }
            cache.get(artist)?.let { return@withContext it }

            val resolved = try {
                resolve(artist)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                // Sin red, timeout o respuesta ilegible: es temporal y no se cachea.
                return@withContext ArtistProfileUiState.Unavailable(
                    ArtistProfileUiState.Unavailable.Reason.Offline
                )
            }
            cache.put(artist, resolved)
            resolved
        }

    /** Búsqueda + resumen, sin caché: lo que de verdad toca la red. */
    private suspend fun resolve(artist: String): ArtistProfileUiState {
        val query = searchTermOf(artist)
        val title = searchTitle(query) ?: return noProfile()
        // Si el artículo que devuelve la búsqueda no es del mismo artista, mejor
        // no enseñar nada que enseñar la ficha de otro.
        if (!isLikelySameArtist(query, title)) return noProfile()
        val page = summary(title) ?: return noProfile()

        val description = page.optString(EXTRACT).trim().takeIf { it.isNotEmpty() }
        val photo = page.optJSONObject(THUMBNAIL)?.optString(SOURCE)?.takeIf { it.isNotBlank() }
            ?: page.optJSONObject(ORIGINAL_IMAGE)?.optString(SOURCE)?.takeIf { it.isNotBlank() }
        if (description == null && photo == null) return noProfile()
        return ArtistProfileUiState.Available(photoUrl = photo, description = description)
    }

    /** Título del artículo que la búsqueda considera mejor para [query]. */
    private suspend fun searchTitle(query: String): String? {
        val url = "$WIKI_API?action=query&list=search&srsearch=${encode(query)}" +
            "&format=json&srlimit=1"
        val json = getJson(url) ?: return null
        val results = json.optJSONObject("query")?.optJSONArray("search") ?: return null
        if (results.length() == 0) return null
        return results.optJSONObject(0)?.optString("title")?.takeIf { it.isNotBlank() }
    }

    /** Resumen del artículo [title] (404 → `null`). */
    private suspend fun summary(title: String): JSONObject? =
        getJson("$WIKI_SUMMARY/${encode(title)}")

    private suspend fun getJson(url: String): JSONObject? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()
        val response = client.newCall(request).await()
        response.use {
            if (!it.isSuccessful) return null
            val body = it.body?.string() ?: return null
            return runCatching { JSONObject(body) }.getOrNull()
        }
    }

    private fun noProfile() = ArtistProfileUiState.Unavailable(
        ArtistProfileUiState.Unavailable.Reason.NoProfile
    )

    companion object {
        private const val WIKI_API = "https://es.wikipedia.org/w/api.php"
        private const val WIKI_SUMMARY = "https://es.wikipedia.org/api/rest_v1/page/summary"
        private const val EXTRACT = "extract"
        private const val THUMBNAIL = "thumbnail"
        private const val ORIGINAL_IMAGE = "originalimage"
        private const val SOURCE = "source"

        /**
         * Wikimedia pide identificar la app y dejar un contacto; si no, puede
         * rechazar la petición.
         */
        const val USER_AGENT =
            "AudiophileCelular/1.0 (Android; +https://github.com/Deivi-Medina/AudiophileCelular)"

        /** Nombres que los canales de música añaden al artista y que no son parte del nombre. */
        private val CHANNEL_NOISE = listOf(" - topic", " vevo", " official", " oficial")

        /**
         * Limpia el nombre que trae la canción para que la búsqueda encuentre al
         * artista: "Bad Bunny - Topic" y "Bad Bunny VEVO" buscan "Bad Bunny".
         */
        fun searchTermOf(artistName: String): String {
            var cleaned = artistName.trim()
            var changed = true
            while (changed) {
                changed = false
                for (noise in CHANNEL_NOISE) {
                    if (cleaned.lowercase(Locale.ROOT).endsWith(noise)) {
                        cleaned = cleaned.dropLast(noise.length).trim()
                        changed = true
                    }
                }
            }
            return cleaned.ifBlank { artistName.trim() }
        }

        /**
         * ¿El artículo encontrado es del mismo artista que buscábamos? Evita
         * enseñar la ficha de otra persona cuando la búsqueda "adivina".
         */
        fun isLikelySameArtist(query: String, title: String): Boolean {
            val q = normalized(query)
            val t = normalized(title)
            if (q.isEmpty() || t.isEmpty()) return false
            if (q == t) return true
            if (t.startsWith(q) || q.startsWith(t)) return true
            val titleWords = t.split(' ').filter { it.isNotBlank() }
            val queryWords = q.split(' ').filter { it.length >= 3 }
            if (queryWords.isEmpty()) return false
            return queryWords.all { word -> titleWords.any { it == word || it.startsWith(word) } }
        }

        private fun normalized(value: String): String =
            Normalizer.normalize(value, Normalizer.Form.NFD)
                .replace(Regex("\\p{Mn}+"), "")
                .lowercase(Locale.ROOT)
                .replace(Regex("[^a-z0-9]+"), " ")
                .trim()

        /** Codifica para query (`+`) o para ruta (`%20`) indistintamente. */
        private fun encode(value: String): String =
            URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    }
}

/**
 * `Call.await()`: espera la respuesta sin bloquear ningún hilo y **cancela la
 * petición** si el menú se cierra antes de que conteste.
 */
private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response)
        }
    })
}
