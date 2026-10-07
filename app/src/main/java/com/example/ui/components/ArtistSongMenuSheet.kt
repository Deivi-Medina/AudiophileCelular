package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ArtistProfileUiState
import com.example.data.model.ratingStarsLabel
import com.example.data.reviews.PublicSongReview
import com.example.data.reviews.SongReviewsProvider
import com.example.data.reviews.SongReviewsSource
import com.example.data.reviews.SongReviewsUiState
import com.example.data.reviews.reviewsUiStateOf
import com.example.ui.theme.Accent
import com.example.ui.theme.AccentDim
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgPrimary
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderStrong
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Duración de la apertura (portada → vinilo → menú) y del cierre. */
private const val VINYL_OPEN_MILLIS = 460
private const val VINYL_CLOSE_MILLIS = 300

/** Tamaño del vinilo ya abierto (el contenedor donde la portada se transforma).
 *  Coincide con la portada del reproductor (260 dp): así el cambio de portada a
 *  vinilo no da ningún salto. */
private val VINYL_SIZE = 260.dp

/** Cuánto hay que subir la portada para que se abra el menú (lo usa también el
 *  reproductor al poner el gesto sobre la portada). */
val COVER_PULL_TRIGGER = 48.dp

/** Cuánto hay que bajar en la parte de arriba del menú para cerrarlo. */
private val MENU_DISMISS_TRIGGER = 96.dp

/**
 * Menú del artista de la canción que está sonando (menú del vinilo).
 *
 * Se abre al subir la portada en el reproductor maximizado. Al abrirse, la
 * portada sube desde su sitio ([vinylStartY], en píxeles respecto al
 * reproductor), se redondea y se convierte en un **vinilo girando**; detrás
 * aparece este menú: cabecera tipo perfil de creador del artista y las reseñas
 * que **otras personas** han dejado de esa canción.
 *
 * El vinilo es solo la animación de apertura: no tiene aguja, no se toca y no
 * se arrastra. Lo único con gesto es la franja superior del menú, que se
 * desliza hacia abajo para cerrar (además del botón ✕).
 */
@Composable
fun SongArtistMenuSheet(
    visible: Boolean,
    songId: String?,
    title: String,
    artist: String,
    coverUrl: String?,
    vinylStartY: Float,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    artistProfile: ArtistProfileUiState = ArtistProfileUiState.Loading,
    reviewsSource: SongReviewsSource = SongReviewsProvider.source
) {
    val progress = remember { Animatable(0f) }
    var mounted by remember { mutableStateOf(false) }
    var dismissDrag by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val spin = rememberVinylSpin()

    LaunchedEffect(visible) {
        if (visible) {
            mounted = true
            progress.animateTo(1f, tween(durationMillis = VINYL_OPEN_MILLIS, easing = FastOutSlowInEasing))
        } else if (mounted) {
            progress.animateTo(0f, tween(durationMillis = VINYL_CLOSE_MILLIS, easing = FastOutLinearInEasing))
            dismissDrag = 0f
            mounted = false
        }
    }

    // Mientras el menú no está montado no dibuja nada: el reproductor se ve intacto.
    if (!mounted) return

    val p = progress.value
    val vinylTargetY = with(density) { 30.dp.toPx() }
    val vinylFullHeightPx = with(density) { VINYL_SIZE.toPx() }
    /** Altura de la franja superior: hueco del vinilo + la zona que cierra al deslizar. */
    val vinylSlotPx = vinylTargetY + vinylFullHeightPx + with(density) { 8.dp.toPx() }
    val vinylY = lerpFloat(vinylStartY, vinylTargetY, p) + dismissDrag
    val dragShift = dismissDrag.roundToInt()

    // La portada se encoge hasta el tamaño de la etiqueta del vinilo.
    val coverMorph = smoothStep(p, 0f, 0.6f)
    val coverAlpha = 1f - smoothStep(p, 0.32f, 0.62f)
    val discAlpha = smoothStep(p, 0.16f, 0.72f)
    val contentAlpha = smoothStep(p, 0.32f, 0.88f)

    Box(modifier = modifier.fillMaxSize().testTag("artist_song_menu")) {
        // Fondo del menú: tapa el reproductor que hay detrás.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = smoothStep(p, 0.10f, 0.80f) }
                .background(BgPrimary)
                .background(
                    Brush.radialGradient(
                        colors = listOf(AccentDim.copy(alpha = 0.55f), Color.Transparent),
                        center = Offset(0f, 0f),
                        radius = 900f
                    )
                )
        )

        // El vinilo: la portada que sube y se convierte en disco girando.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, vinylY.roundToInt()) },
            contentAlignment = Alignment.TopCenter
        ) {
            Box(modifier = Modifier.size(VINYL_SIZE).testTag("artist_menu_vinyl")) {
                StableCoverArt(
                    coverUrl = coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxSize(lerpFloat(1f, 0.38f, coverMorph))
                        .graphicsLayer { alpha = coverAlpha }
                        .clip(RoundedCornerShape(lerpFloat(24f, 130f, coverMorph).dp))
                )
                VinylRecord(
                    coverUrl = coverUrl,
                    // 1 vuelta y media mientras se abre + giro infinito después.
                    rotationDegrees = 540f * p + spin,
                    morph = p,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = discAlpha }
                )
            }
        }

        // Contenido: cabecera del artista + reseñas de la canción.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, dragShift) }
                .graphicsLayer { alpha = contentAlpha }
        ) {
            Spacer(modifier = Modifier.height(with(density) { vinylSlotPx.toDp() }))
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                ArtistProfileHeader(artist = artist, profile = artistProfile)
                SongReviewsSection(songId = songId, songTitle = title, source = reviewsSource)
            }
        }

        // Franja superior: deslizar hacia abajo aquí cierra el menú.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { vinylSlotPx.toDp() })
                .menuDismissDragGesture(
                    onProgress = { dismissDrag = it },
                    onDismiss = {
                        dismissDrag = 0f
                        onDismissRequest()
                    }
                )
                .testTag("artist_menu_top_zone"),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .size(width = 44.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF))
            )
        }

        IconButton(
            onClick = onDismissRequest,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0x33FFFFFF))
                .border(BorderStroke(1.dp, GlassBorder), CircleShape)
                .testTag("artist_menu_close")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cerrar menú del artista",
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─────────────────────────── CABECERA DEL ARTISTA ───────────────────────────

/**
 * Cabecera tipo perfil de creador: foto (o su hueco), nombre y descripción.
 * El nombre siempre está; la foto y la descripción llegan de fuera en la
 * segunda mitad, así que hoy se enseña un placeholder de carga sin inventar
 * ningún dato.
 */
@Composable
private fun ArtistProfileHeader(artist: String, profile: ArtistProfileUiState) {
    val available = profile as? ArtistProfileUiState.Available
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ArtistAvatar(
            photoUrl = available?.photoUrl,
            isLoading = profile is ArtistProfileUiState.Loading
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = artist.ifBlank { "Artista desconocido" },
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            when (profile) {
                is ArtistProfileUiState.Available -> {
                    if (!profile.description.isNullOrBlank()) {
                        Text(
                            text = profile.description,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "Todavía no hay descripción de este artista.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
                ArtistProfileUiState.Unavailable -> Text(
                    text = "Todavía no hay ficha de este artista.",
                    color = TextMuted,
                    fontSize = 12.sp
                )
                ArtistProfileUiState.Loading -> {
                    Text(
                        text = "Cargando ficha del artista…",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    SkeletonBar(modifier = Modifier.fillMaxWidth(0.92f))
                    SkeletonBar(modifier = Modifier.fillMaxWidth(0.62f))
                }
            }
        }
    }
}

@Composable
private fun ArtistAvatar(photoUrl: String?, isLoading: Boolean) {
    val pulse = rememberSkeletonAlpha(if (isLoading) 0.45f else 0f)
    Box(
        modifier = Modifier
            .size(76.dp)
            .graphicsLayer { alpha = if (isLoading) pulse else 1f }
            .clip(CircleShape)
            .background(BgCard)
            .border(BorderStroke(1.dp, GlassBorderStrong), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

// ───────────────────────── RESEÑAS DE OTRAS PERSONAS ─────────────────────────

@Composable
private fun SongReviewsSection(songId: String?, songTitle: String, source: SongReviewsSource) {
    val state = rememberSongReviewsState(songId = songId, source = source)
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Reseñas de otras personas",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            val count = (state as? SongReviewsUiState.Content)?.reviews?.size
            if (count != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "$count", color = TextMuted, fontSize = 12.sp)
            }
        }
        if (songTitle.isNotBlank()) {
            Text(
                text = songTitle,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        when (state) {
            SongReviewsUiState.Loading -> {
                SkeletonReviewCard()
                SkeletonReviewCard()
            }
            SongReviewsUiState.Empty -> EmptyReviewsCard()
            is SongReviewsUiState.Error -> Text(
                text = state.message,
                color = TextSecondary,
                fontSize = 12.sp
            )
            is SongReviewsUiState.Content -> state.reviews.forEach { review ->
                ReviewCard(review)
            }
        }
    }
}

/** Todavía no hay ninguna reseña de otras personas: estado vacío cuidado. */
@Composable
private fun EmptyReviewsCard() {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, GlassBorder),
        modifier = Modifier.fillMaxWidth().testTag("artist_menu_reviews_empty")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AccentDim),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Album,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = "Nadie ha reseñado esta canción todavía",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Cuando otra persona deje su reseña de esta canción, aparecerá aquí. " +
                    "Las reseñas compartidas se conectarán próximamente; mientras tanto solo se " +
                    "ven las tuyas en tu perfil.",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SkeletonReviewCard() {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, GlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GlassBorderStrong)
                )
                Spacer(modifier = Modifier.width(10.dp))
                SkeletonBar(modifier = Modifier.fillMaxWidth(0.5f))
            }
            SkeletonBar(modifier = Modifier.fillMaxWidth(0.95f))
            SkeletonBar(modifier = Modifier.fillMaxWidth(0.7f))
        }
    }
}

@Composable
private fun ReviewCard(review: PublicSongReview) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, GlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(AccentDim)
                        .border(BorderStroke(1.dp, GlassBorder), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!review.authorPhotoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = review.authorPhotoUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = review.authorName.trim().take(1).uppercase().ifBlank { "?" },
                            color = Accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.authorName,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = reviewDateLabel(review.createdAt),
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = ratingStarsLabel(review.rating),
                    color = Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (review.comment.isNotBlank()) {
                Text(text = review.comment, color = TextSecondary, fontSize = 13.sp)
            }
        }
    }
}

// ──────────────────────────────── AYUDANTES ────────────────────────────────

@Composable
private fun rememberSongReviewsState(songId: String?, source: SongReviewsSource): SongReviewsUiState {
    val flow = remember(songId, source) {
        if (songId.isNullOrBlank()) flowOf(emptyList<PublicSongReview>())
        else source.observeReviews(songId)
    }
    val reviews by flow.collectAsState(initial = null)
    return reviewsUiStateOf(reviews)
}

/** Barra gris que late mientras se espera contenido (foto y descripción del artista, reseñas). */
@Composable
private fun SkeletonBar(modifier: Modifier = Modifier) {
    val alpha = rememberSkeletonAlpha(1f)
    Box(
        modifier = modifier
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .graphicsLayer { this.alpha = alpha }
            .background(GlassBorderStrong)
    )
}

@Composable
private fun rememberSkeletonAlpha(target: Float): Float {
    if (target <= 0f) return 1f
    val transition = rememberInfiniteTransition(label = "skeletonPulse")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonPulseAlpha"
    )
    return alpha
}

private val reviewDateFormat = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("es"))

private fun reviewDateLabel(createdAt: Long): String =
    if (createdAt <= 0L) "" else reviewDateFormat.format(Date(createdAt))

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction.coerceIn(0f, 1f)

/** 0 antes de [start], 1 después de [end], suave en medio. */
private fun smoothStep(value: Float, start: Float, end: Float): Float {
    if (end <= start) return if (value >= end) 1f else 0f
    val t = ((value - start) / (end - start)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/**
 * Gesto de **subir la portada** en el reproductor maximizado.
 *
 * Solo actúa sobre la portada: allí donde se pone este modificador.
 * - Arrastrar hacia arriba consume el gesto (el reproductor no se cierra con
 *   ese movimiento) y avisa del recorrido en píxeles por [onPullProgress].
 * - Al pasar de [triggerPx] píxeles, dispara [onTriggered].
 * - Arrastrar hacia abajo **no** se consume: el gesto de cerrar el reproductor
 *   sigue funcionando igual sobre la portada.
 */
fun Modifier.coverPullUpGesture(
    triggerPx: Float,
    onPullProgress: (Float) -> Unit,
    onTriggered: () -> Unit
): Modifier = pointerInput(triggerPx) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var pulled = 0f
        var triggered = false
        do {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.pressed }
            if (change != null) {
                val deltaY = change.positionChange().y
                if (deltaY < 0f && !triggered) {
                    pulled += -deltaY
                    onPullProgress(pulled)
                    change.consume()
                } else if (pulled > 0f && !triggered) {
                    pulled = (pulled + deltaY).coerceAtLeast(0f)
                    onPullProgress(pulled)
                    change.consume()
                }
                if (!triggered && pulled >= triggerPx) {
                    triggered = true
                    onTriggered()
                }
            }
        } while (event.changes.any { it.pressed })
        if (!triggered) onPullProgress(0f)
    }
}

/**
 * Gesto de **cerrar el menú del artista** deslizando hacia abajo.
 * Solo se pone en la franja superior (la del vinilo), así no pelea con el
 * desplazamiento de las reseñas ni con el cierre del reproductor.
 */
private fun Modifier.menuDismissDragGesture(
    onProgress: (Float) -> Unit,
    onDismiss: () -> Unit
): Modifier = pointerInput(Unit) {
    val threshold = MENU_DISMISS_TRIGGER.toPx()
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var dragged = 0f
        var dismissed = false
        do {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.pressed }
            if (change != null) {
                val deltaY = change.positionChange().y
                if (deltaY > 0f) {
                    dragged += deltaY
                    onProgress(dragged)
                    change.consume()
                    if (!dismissed && dragged >= threshold) {
                        dismissed = true
                        onDismiss()
                    }
                } else if (dragged > 0f) {
                    dragged = (dragged + deltaY).coerceAtLeast(0f)
                    onProgress(dragged)
                    change.consume()
                }
            }
        } while (event.changes.any { it.pressed })
        if (!dismissed) onProgress(0f)
    }
}
