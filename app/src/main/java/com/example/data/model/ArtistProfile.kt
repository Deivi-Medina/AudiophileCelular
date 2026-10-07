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

    /** La fuente externa existe pero no tiene ficha de este artista. */
    data object Unavailable : ArtistProfileUiState
}
