package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.roundToInt
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.PlaybackQueue
import com.example.data.model.PlaybackQueueItem
import com.example.data.model.RepeatMode
import com.example.ui.theme.Accent
import com.example.ui.theme.AccentDim
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgPrimary
import com.example.ui.theme.BgSecondary
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderStrong
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CosmicFullPlayerSheet(
    visible: Boolean,
    title: String,
    artist: String,
    /** Id de la canción que suena: con él se piden las reseñas de otras personas. */
    songId: String? = null,
    coverUrl: String?,
    isPlaying: Boolean,
    progress: Float,
    durationText: String,
    durationMs: Long = 0L,
    isStreamPreview: Boolean,
    qualityLabel: String? = "320 kbps Hi-Fi",
    playbackQueue: PlaybackQueue = PlaybackQueue.EMPTY,
    onPlayPauseToggle: () -> Unit,
    onSeek: (Float) -> Unit,
    onClose: () -> Unit,
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
    onOpenReviews: () -> Unit = {},
    onGoToArtist: (String) -> Unit = {},
    onDownloadTrack: () -> Unit = {},
    onClearQueue: () -> Unit = {},
    onRemoveQueueIndex: (Int) -> Unit = {},
    onPlayQueueIndex: (Int) -> Unit = {},
    onReorderQueue: (Int, Int) -> Unit = { _, _ -> },
    isShuffleActive: Boolean = false,
    repeatMode: RepeatMode = RepeatMode.OFF,
    playlistContextName: String? = null,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onToggleRepeat: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showQueueBottomSheet by remember { mutableStateOf(false) }
    var showMenuBottomSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val density = LocalDensity.current

    // ── Menú del artista (menú del vinilo) ────────────────────────────────
    // Se abre al subir la portada del reproductor maximizado.
    var artistMenuOpen by remember { mutableStateOf(false) }
    var coverPullPx by remember { mutableStateOf(0f) }
    var coverTopInRoot by remember { mutableStateOf(0f) }
    var sheetTopInRoot by remember { mutableStateOf(0f) }
    var vinylStartY by remember { mutableStateOf(0f) }
    // La portada acompaña al dedo al subirla y se levanta al abrirse el menú.
    val coverLiftPx by animateFloatAsState(
        targetValue = when {
            artistMenuOpen -> with(density) { -80.dp.toPx() }
            else -> -coverPullPx.coerceAtMost(with(density) { 72.dp.toPx() })
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "coverLift"
    )
    // Al cerrar el reproductor, el menú del artista se cierra con él.
    LaunchedEffect(visible) {
        if (!visible) {
            artistMenuOpen = false
            coverPullPx = 0f
        }
    }

    var dominantColor by remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(coverUrl) {
        if (!coverUrl.isNullOrEmpty()) {
            val loader = coil.ImageLoader(context)
            val model = when {
                coverUrl.startsWith("file://") -> File(coverUrl.removePrefix("file://"))
                coverUrl.startsWith("/") -> File(coverUrl)
                else -> coverUrl
            }
            val request = coil.request.ImageRequest.Builder(context)
                .data(model)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            if (result is coil.request.SuccessResult) {
                val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    var rSum = 0L
                    var gSum = 0L
                    var bSum = 0L
                    var count = 0L
                    val width = bitmap.width
                    val height = bitmap.height
                    val stepX = (width / 8).coerceAtLeast(1)
                    val stepY = (height / 8).coerceAtLeast(1)
                    for (x in 0 until width step stepX) {
                        for (y in 0 until height step stepY) {
                            val pixel = bitmap.getPixel(x, y)
                            rSum += android.graphics.Color.red(pixel)
                            gSum += android.graphics.Color.green(pixel)
                            bSum += android.graphics.Color.blue(pixel)
                            count++
                        }
                    }
                    if (count > 0) {
                        val r = (rSum / count).toInt().coerceIn(0, 255)
                        val g = (gSum / count).toInt().coerceIn(0, 255)
                        val b = (bSum / count).toInt().coerceIn(0, 255)
                        dominantColor = Color(r, g, b)
                    }
                }
            }
        } else {
            dominantColor = null
        }
    }

    val ambientGradient = remember(dominantColor) {
        if (dominantColor != null) {
            Brush.verticalGradient(
                listOf(
                    dominantColor!!.copy(alpha = 0.55f),
                    BgPrimary,
                    dominantColor!!.copy(alpha = 0.3f)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0x441A0A2E),
                    BgPrimary,
                    Color(0x882E0854)
                )
            )
        }
    }

    // Bottom Sheet de Cola de Reproducción con Drag and Drop fluido y canción actual bloqueada
    if (showQueueBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = BgSecondary,
            scrimColor = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp)
            ) {
                // Handle bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(TextMuted.copy(alpha = 0.4f))
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "REPRODUCIENDO DESDE",
                            color = Accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = playlistContextName ?: "Cola de reproducción",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (playbackQueue.items.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                showQueueBottomSheet = false
                                onClearQueue()
                            }
                        ) {
                            Text("Vaciar cola", color = Color(0xFFFF453A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider(color = GlassBorderStrong)
                Spacer(modifier = Modifier.height(12.dp))

                if (playbackQueue.items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("La cola de reproducción está vacía", color = TextSecondary, fontSize = 14.sp)
                    }
                } else {
                    Text(
                        text = "Mantén presionada una canción para moverla · desliza para quitarla",
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val haptic = LocalHapticFeedback.current
                    val queueListState = rememberLazyListState()
                    val reorderState = rememberReorderableLazyListState(queueListState) { from, to ->
                        onReorderQueue(from.index, to.index)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }

                    LazyColumn(
                        state = queueListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(playbackQueue.items, key = { _, item -> item.uid }) { idx, item ->
                            val isCurrent = idx == playbackQueue.currentIndex
                            val currentIdx by rememberUpdatedState(idx)

                            val itemTitle = when (item) {
                                is PlaybackQueueItem.Stream -> item.track.title
                                is PlaybackQueueItem.Local -> item.song.title
                            }
                            val itemArtist = when (item) {
                                is PlaybackQueueItem.Stream -> item.track.channelOrArtist
                                is PlaybackQueueItem.Local -> item.song.artist
                            }

                            ReorderableItem(reorderState, key = item.uid) { isDragging ->
                                val dismissState = rememberSwipeToDismissBoxState()
                                LaunchedEffect(dismissState.currentValue) {
                                    if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                                        onRemoveQueueIndex(currentIdx)
                                    }
                                }

                                SwipeToDismissBox(
                                    state = dismissState,
                                    gesturesEnabled = !isCurrent && !isDragging,
                                    backgroundContent = { QueueItemDeleteBackground(dismissState.dismissDirection) }
                                ) {
                                    QueueItemCard(
                                        index = idx,
                                        title = itemTitle,
                                        artist = itemArtist,
                                        isCurrent = isCurrent,
                                        isDragging = isDragging,
                                        modifier = Modifier.longPressDraggableHandle(
                                            onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                                        ),
                                        handleModifier = Modifier.draggableHandle(
                                            onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                                        ),
                                        onClick = {
                                            showQueueBottomSheet = false
                                            onPlayQueueIndex(currentIdx)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Bottom Sheet de Opciones de Pista en el Reproductor Minimalista
    if (showMenuBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMenuBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = BgSecondary,
            scrimColor = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp)
            ) {
                // Handle bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(TextMuted.copy(alpha = 0.4f))
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = artist, color = Accent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                HorizontalDivider(color = GlassBorderStrong)
                Spacer(modifier = Modifier.height(10.dp))

                data class SheetMenuOption(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val tint: Color, val action: () -> Unit)
                val playerMenuOptions = listOf(
                    SheetMenuOption("Añadir a playlist", Icons.Default.Add, Accent) {
                        showMenuBottomSheet = false
                        onAddToPlaylist()
                    },
                    SheetMenuOption("Descargar canción (320 kbps)", Icons.Default.CloudDownload, Accent) {
                        showMenuBottomSheet = false
                        onDownloadTrack()
                    },
                    SheetMenuOption("Ver reseñas y valorar (5★)", Icons.Default.Star, Color(0xFFFFB800)) {
                        showMenuBottomSheet = false
                        onOpenReviews()
                    },
                    SheetMenuOption("Ir al artista ($artist)", Icons.Default.Person, Accent) {
                        showMenuBottomSheet = false
                        onGoToArtist(artist)
                    }
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    playerMenuOptions.forEach { option ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BgCard),
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = option.action)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(AccentDim),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = option.icon, contentDescription = null, tint = option.tint, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(text = option.label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ) + fadeIn(animationSpec = tween(200)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ) + fadeOut(animationSpec = tween(200)),
        modifier = modifier
    ) {
        val scope = rememberCoroutineScope()
        val swipeOffsetY = remember { Animatable(0f) }
        val dismissVelocityPx = with(density) { 1000.dp.toPx() }

        LaunchedEffect(visible) {
            if (visible) swipeOffsetY.snapTo(0f)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, swipeOffsetY.value.roundToInt()) }
                .background(BgPrimary)
                .background(ambientGradient)
                .onGloballyPositioned { sheetTopInRoot = it.boundsInRoot().top }
                .pointerInput(artistMenuOpen) {
                    // Con el menú del artista abierto, el arrastre hacia abajo lo
                    // gestiona el menú (para cerrarse), no el reproductor.
                    if (artistMenuOpen) return@pointerInput
                    val velocityTracker = VelocityTracker()
                    var totalDrag = 0f
                    detectVerticalDragGestures(
                        onDragStart = {
                            velocityTracker.resetTracking()
                            totalDrag = swipeOffsetY.value
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            // El Box se desplaza con el dedo, así que se mide el arrastre acumulado
                            // en vez de la posición local para obtener una velocidad real.
                            totalDrag = (totalDrag + dragAmount).coerceAtLeast(0f)
                            velocityTracker.addPosition(change.uptimeMillis, Offset(0f, totalDrag))
                            scope.launch { swipeOffsetY.snapTo(totalDrag) }
                        },
                        onDragEnd = {
                            val velocityY = velocityTracker.calculateVelocity().y
                            val sheetHeight = size.height.toFloat()
                            val shouldDismiss = totalDrag > sheetHeight * 0.25f || velocityY > dismissVelocityPx
                            scope.launch {
                                if (shouldDismiss) {
                                    swipeOffsetY.animateTo(
                                        targetValue = sheetHeight,
                                        animationSpec = tween(durationMillis = 220, easing = LinearOutSlowInEasing),
                                        initialVelocity = velocityY.coerceAtLeast(0f)
                                    )
                                    onClose()
                                } else {
                                    swipeOffsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { swipeOffsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                        }
                    )
                }
                .testTag("cosmic_full_player_sheet")
        ) {
            // Capa 2: CONTENIDO del player (Minimalista)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = 40.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Barra superior
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .blockSheetSwipe()
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .border(1.dp, GlassBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Minimizar reproductor",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = if (!playlistContextName.isNullOrEmpty()) "PLAYLIST: ${playlistContextName.uppercase()}"
                                   else if (isStreamPreview) "STREAMING EN VIVO" else "BIBLIOTECA OFFLINE",
                            color = Accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Botón de Cola de Reproducción
                        IconButton(
                            onClick = { showQueueBottomSheet = true },
                            modifier = Modifier
                                .blockSheetSwipe()
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                                .border(1.dp, GlassBorder, CircleShape)
                                .testTag("full_player_queue_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = "Cola de reproducción",
                                tint = Accent,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Botón de 3 Puntos (Opciones)
                        IconButton(
                            onClick = { showMenuBottomSheet = true },
                            modifier = Modifier
                                .blockSheetSwipe()
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                                .border(1.dp, GlassBorder, CircleShape)
                                .testTag("full_player_menu_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Más opciones",
                                tint = TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Portada con efecto de halo y resplandor
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .aspectRatio(1f)
                        .onGloballyPositioned { coverTopInRoot = it.boundsInRoot().top }
                        .graphicsLayer { translationY = coverLiftPx }
                        // Zona del gesto: subir la portada abre el menú del vinilo.
                        // Bajar por aquí no se consume, así el reproductor se sigue
                        // cerrando como siempre.
                        .coverPullUpGesture(
                            triggerPx = with(density) { COVER_PULL_TRIGGER.toPx() },
                            onPullProgress = { coverPullPx = it },
                            onTriggered = {
                                // Se guarda dónde está la portada para que el vinilo
                                // nazca justo encima de ella.
                                vinylStartY = coverTopInRoot - sheetTopInRoot
                                coverPullPx = 0f
                                artistMenuOpen = true
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Fondo difuminado + portada principal. StableCoverArt mantiene
                    // la portada anterior hasta que la nueva esté cargada, así al
                    // cambiar de canción no se ve la portada predeterminada.
                    StableCoverArt(
                        coverUrl = coverUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(28.dp)
                    )

                    StableCoverArt(
                        coverUrl = coverUrl,
                        contentDescription = "Portada de $title",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp))
                            .border(BorderStroke(1.dp, GlassBorderStrong), RoundedCornerShape(24.dp))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Metadatos de la canción
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = Accent,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Barra de línea de tiempo
                Column(modifier = Modifier.fillMaxWidth().blockSheetSwipe()) {
                    Slider(
                        value = progress.coerceIn(0f, 1f),
                        onValueChange = onSeek,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Accent,
                            inactiveTrackColor = GlassBorderStrong
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val totalSeconds = if (durationMs > 0) {
                            durationMs / 1000
                        } else {
                            val parts = durationText.trim().split(":")
                            if (parts.size == 2) {
                                (parts[0].toLongOrNull() ?: 3L) * 60 + (parts[1].toLongOrNull() ?: 30L)
                            } else if (parts.size == 3) {
                                (parts[0].toLongOrNull() ?: 0L) * 3600 + (parts[1].toLongOrNull() ?: 0L) * 60 + (parts[2].toLongOrNull() ?: 0L)
                            } else {
                                210L
                            }
                        }
                        val currentSeconds = (progress * totalSeconds).toLong().coerceIn(0L, totalSeconds)
                        val currentText = "%d:%02d".format(currentSeconds / 60, currentSeconds % 60)
                        val totalFormatted = "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)

                        Text(
                            text = currentText,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = totalFormatted,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Controles principales
                Row(
                    modifier = Modifier.fillMaxWidth().blockSheetSwipe(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleShuffle,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = if (isShuffleActive) "Modo aleatorio activado" else "Modo aleatorio desactivado",
                            tint = if (isShuffleActive) Accent else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Anterior",
                            tint = TextPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 12.dp,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onPlayPauseToggle)
                            .testTag("full_player_play_pause")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                tint = BgPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onNext,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Siguiente",
                            tint = TextPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleRepeat,
                        modifier = Modifier.size(40.dp)
                    ) {
                        val (icon, tint, desc) = when (repeatMode) {
                            RepeatMode.OFF -> Triple(Icons.Default.Repeat, TextSecondary, "Repetición desactivada")
                            RepeatMode.ALL -> Triple(Icons.Default.Repeat, Accent, "Repetir lista")
                            RepeatMode.ONE -> Triple(Icons.Default.RepeatOne, Accent, "Repetir canción actual")
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = desc,
                            tint = tint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Menú del artista: detrás de la animación de la portada → vinilo.
            SongArtistMenuSheet(
                visible = artistMenuOpen && visible,
                songId = songId,
                title = title,
                artist = artist,
                coverUrl = coverUrl,
                vinylStartY = vinylStartY,
                onDismissRequest = { artistMenuOpen = false }
            )
        }
    }
}

/**
 * Evita que el gesto de deslizar-para-cerrar del reproductor arranque desde este elemento.
 * Solo consume el movimiento cuando supera media "touch slop" (antes que el gesto padre),
 * así los toques con un pequeño temblor siguen contando como clic.
 */
private fun Modifier.blockSheetSwipe(): Modifier = pointerInput(Unit) {
    val threshold = viewConfiguration.touchSlop / 2f
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var travelled = Offset.Zero
        do {
            val event = awaitPointerEvent()
            event.changes.forEach { change ->
                travelled += change.positionChange()
                if (travelled.getDistance() > threshold) change.consume()
            }
        } while (event.changes.any { it.pressed })
    }
}

@Composable
private fun QueueItemDeleteBackground(direction: SwipeToDismissBoxValue) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFF453A).copy(alpha = 0.85f))
            .padding(horizontal = 20.dp),
        contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Quitar de la cola",
            tint = Color.White
        )
    }
}

@Composable
private fun QueueItemCard(
    index: Int,
    title: String,
    artist: String,
    isCurrent: Boolean,
    isDragging: Boolean,
    modifier: Modifier,
    handleModifier: Modifier,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (isDragging) 1.03f else 1f, label = "queueItemScale")
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isDragging -> Color(0xFF2A1D45)
                isCurrent -> Color(0xFF221836)
                else -> BgCard
            }
        ),
        border = BorderStroke(1.dp, if (isDragging || isCurrent) Accent else GlassBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 12.dp else 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isCurrent) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Sonando",
                    tint = Accent,
                    modifier = Modifier
                        .width(24.dp)
                        .size(18.dp)
                )
            } else {
                Text(
                    text = "${index + 1}",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isCurrent) Accent else TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = artist,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isCurrent) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AccentDim
                ) {
                    Text(
                        text = "Actual",
                        color = Accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Arrastrar para reordenar",
                tint = if (isDragging) Accent else TextSecondary,
                modifier = handleModifier.size(24.dp)
            )
        }
    }
}
