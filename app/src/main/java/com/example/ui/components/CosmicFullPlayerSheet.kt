package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.zIndex
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
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(playbackQueue.items) { idx, item ->
                            val isCurrent = idx == playbackQueue.currentIndex
                            var itemOffset by remember { mutableStateOf(0f) }
                            var isDragging by remember { mutableStateOf(false) }

                            val itemTitle = when (item) {
                                is PlaybackQueueItem.Stream -> item.track.title
                                is PlaybackQueueItem.Local -> item.song.title
                            }
                            val itemArtist = when (item) {
                                is PlaybackQueueItem.Stream -> item.track.channelOrArtist
                                is PlaybackQueueItem.Local -> item.song.artist
                            }

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        isDragging -> Accent.copy(alpha = 0.35f)
                                        isCurrent -> Accent.copy(alpha = 0.15f)
                                        else -> BgCard
                                    }
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    when {
                                        isDragging -> Accent
                                        isCurrent -> Accent
                                        else -> GlassBorder
                                    }
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 16.dp else 0.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .zIndex(if (isDragging) 10f else 1f)
                                    .graphicsLayer {
                                        translationY = if (isDragging) itemOffset else 0f
                                        scaleX = if (isDragging) 1.02f else 1f
                                        scaleY = if (isDragging) 1.02f else 1f
                                        shadowElevation = if (isDragging) 16f else 0f
                                    }
                                    .pointerInput(idx, isCurrent) {
                                        if (!isCurrent) {
                                            detectVerticalDragGestures(
                                                onDragStart = { isDragging = true },
                                                onDragEnd = {
                                                    isDragging = false
                                                    itemOffset = 0f
                                                },
                                                onDragCancel = {
                                                    isDragging = false
                                                    itemOffset = 0f
                                                },
                                                onVerticalDrag = { change, dragAmount ->
                                                    change.consume()
                                                    itemOffset += dragAmount
                                                    val itemHeightPx = 65f * density.density
                                                    if (itemOffset > itemHeightPx && idx < playbackQueue.items.lastIndex) {
                                                        onReorderQueue(idx, idx + 1)
                                                        itemOffset = 0f
                                                    } else if (itemOffset < -itemHeightPx && idx > 0) {
                                                        onReorderQueue(idx, idx - 1)
                                                        itemOffset = 0f
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    .clickable {
                                        showQueueBottomSheet = false
                                        onPlayQueueIndex(idx)
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
                                            text = "${idx + 1}",
                                            color = TextMuted,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = itemTitle,
                                            color = if (isCurrent) Accent else TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = itemArtist,
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (!isCurrent) {
                                        IconButton(
                                            onClick = { onRemoveQueueIndex(idx) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Eliminar de la cola",
                                                tint = TextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        Icon(
                                            imageVector = Icons.Default.DragHandle,
                                            contentDescription = "Arrastrar",
                                            tint = if (isDragging) Accent else TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
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
                                    }
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
        var swipeOffsetY by remember { mutableStateOf(0f) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, swipeOffsetY.coerceAtLeast(0f).toInt()) }
                .background(BgPrimary)
                .background(ambientGradient)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            swipeOffsetY = (swipeOffsetY + dragAmount).coerceAtLeast(0f)
                        },
                        onDragEnd = {
                            if (swipeOffsetY > 140f) {
                                onClose()
                            }
                            swipeOffsetY = 0f
                        },
                        onDragCancel = {
                            swipeOffsetY = 0f
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
                val coverModel = remember(coverUrl) {
                    when {
                        coverUrl.isNullOrEmpty() -> null
                        coverUrl.startsWith("file://") -> File(coverUrl.removePrefix("file://"))
                        coverUrl.startsWith("/") -> File(coverUrl)
                        else -> coverUrl
                    }
                }

                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(coverModel)
                            .crossfade(true)
                            .build(),
                        placeholder = painterResource(id = R.drawable.audiophiles_default_cover),
                        error = painterResource(id = R.drawable.audiophiles_default_cover),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(28.dp)
                    )

                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(coverModel)
                            .crossfade(true)
                            .build(),
                        placeholder = painterResource(id = R.drawable.audiophiles_default_cover),
                        error = painterResource(id = R.drawable.audiophiles_default_cover),
                        contentDescription = "Portada de $title",
                        contentScale = ContentScale.Crop,
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
                Column(modifier = Modifier.fillMaxWidth()) {
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
                    modifier = Modifier.fillMaxWidth(),
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
        }
    }
}
