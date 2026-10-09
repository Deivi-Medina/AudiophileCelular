package com.example.data.model

/**
 * Ficha del artista de la canción que se está escuchando, para la cabecera del
 * menú del vinilo.
 *
 * El **nombre** siempre se conoce (viene de la canción). La **foto** y la
 * **descripción** las traerá una fuente externa en la segunda mitad de esta
 * función; hasta entonces la UI enseña un placeholder de carga limpio y nunca
 * datos inventados.
 */
sealed interface ArtistProfileUiState {
    /** Sin foto ni descripción todavía: la UI pinta el estado de carga. */
    data object Loading : ArtistProfileUiState

    /** Llegó la ficha externa. Cualquiera de los dos campos puede faltar. */
    data class Available(
        val photoUrl: String? = null,
        val description: String? = null
    ) : ArtistProfileUiState

    /**
     * No hay foto ni descripción que enseñar. [reason] sirve para que la UI
     * explique **qué** ha pasado en vez de un mensaje genérico.
     */
    data class Unavailable(val reason: Reason = Reason.NoProfile) : ArtistProfileUiState {
        enum class Reason {
            /** La fuente externa no tiene artículo de este artista. */
            NoProfile,

            /** La canción no trae nombre de artista, así que no hay nada que buscar. */
            NoArtist,

            /** No se pudo llegar a la fuente (sin conexión, timeout…). Es temporal. */
            Offline
        }
    }
}
