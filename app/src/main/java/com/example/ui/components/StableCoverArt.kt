package com.example.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import java.io.File

/**
 * Convierte la url o ruta de una portada en un modelo que Coil entiende.
 * Acepta rutas locales (`file://`, rutas absolutas) y urls remotas.
 */
fun coverModelFor(coverUrl: String?): Any? = when {
    coverUrl.isNullOrBlank() -> null
    coverUrl.startsWith("file://") -> File(coverUrl.removePrefix("file://"))
    coverUrl.startsWith("/") -> File(coverUrl)
    else -> coverUrl
}

/**
 * Portada de canción que **no vuelve al placeholder mientras carga**.
 *
 * Al cambiar de canción se sigue dibujando la última portada cargada con éxito
 * hasta que la nueva esté lista, así no se ve el salto a la portada
 * predeterminada. La portada predeterminada solo aparece cuando de verdad no
 * hay ninguna portada anterior que mostrar (primer arranque de la app) o cuando
 * la canción actual no tiene portada.
 */
@Composable
fun StableCoverArt(
    coverUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    val defaultCover = painterResource(id = R.drawable.audiophiles_default_cover)

    // Última portada que se dibujó de verdad: se conserva entre canciones.
    var lastLoadedCover by remember { mutableStateOf<Drawable?>(null) }

    if (coverUrl.isNullOrBlank()) {
        // Esta canción no trae portada: aquí sí corresponde la predeterminada.
        Image(
            painter = defaultCover,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    val request = remember(coverUrl, context) {
        ImageRequest.Builder(context)
            .data(coverModelFor(coverUrl))
            .crossfade(true)
            .build()
    }

    val previousPainter = remember(lastLoadedCover) {
        lastLoadedCover?.let { previous ->
            runCatching {
                BitmapPainter(previous.toBitmap().asImageBitmap())
            }.getOrNull()
        }
    }

    Box(modifier = modifier) {
        // Capa inferior: la portada anterior se mantiene a la vista mientras la
        // nueva carga, en vez de dejar el hueco de la portada predeterminada.
        previousPainter?.let { painter ->
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        }
        AsyncImage(
            model = request,
            // Solo se usa el placeholder si aún no hay ninguna portada anterior.
            placeholder = if (previousPainter == null) defaultCover else null,
            error = defaultCover,
            contentDescription = contentDescription,
            contentScale = contentScale,
            onSuccess = { success ->
                if (lastLoadedCover !== success.result.drawable) {
                    lastLoadedCover = success.result.drawable
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
