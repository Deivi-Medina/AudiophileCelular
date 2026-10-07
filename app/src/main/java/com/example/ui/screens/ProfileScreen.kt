package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.Achievement
import com.example.data.local.AchievementsCatalog
import com.example.data.local.LevelCurve
import com.example.data.local.PinnedFavoriteEntity
import com.example.data.local.UserProfile
import com.example.data.local.XpSource
import com.example.data.model.SongRef
import com.example.data.model.toSongRef
import com.example.ui.components.CosmicMeshBackground
import com.example.ui.components.RatingsChartSection
import com.example.ui.components.ReviewsJournalSection
import com.example.ui.components.SongCover
import com.example.ui.components.SongPickTarget
import com.example.ui.components.SongPickerDialog
import com.example.ui.theme.Accent
import com.example.ui.theme.AccentDim
import com.example.ui.theme.AccentHover
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgInput
import com.example.ui.theme.BgSecondary
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.HoverBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SearchAndDownloadViewModel

/** Número de favoritas fijadas del perfil (igual que las que ya existían). */
private const val FAVORITE_SLOTS = 5

/**
 * Perfil estilo Instagram:
 *
 *  - Cabecera FIJA arriba (avatar, nombre, bio y contadores): no se va con el scroll.
 *  - Barra horizontal de pestañas con iconos (como IG) para elegir qué ver.
 *  - Pestaña 1 "Favoritas": las 5 favoritas fijadas + nivel con XP real + logros.
 *  - Pestaña 2 "Reseñas": el diario musical que ya existía (ReviewsJournalSection), la
 *    lista "Por escuchar" (que llena el dueño a mano) y la gráfica de las notas que
 *    más das (RatingsChartSection).
 */
@Composable
fun ProfileScreen(
    viewModel: SearchAndDownloadViewModel,
    downloadedCount: Int,
    playlistsCount: Int,
    onOpenSongReviews: (SongRef) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val profile by viewModel.userProfile.collectAsState()
    val allReviews by viewModel.allReviews.collectAsState()
    val listenLater by viewModel.listenLater.collectAsState()
    val pinnedFavorites by viewModel.pinnedFavorites.collectAsState()
    val localSongs by viewModel.localSongs.collectAsState()
    val reviewSearchResults by viewModel.reviewSearchResults.collectAsState()
    val isReviewSearching by viewModel.isReviewSearching.collectAsState()
    // Progreso real, persistido en las prefs del perfil
    val xp by viewModel.xp.collectAsState()
    val unlockedAchievements by viewModel.unlockedAchievements.collectAsState()
    // Contadores sociales: 0 hasta que el dueño conecte Firebase
    val followersCount by viewModel.followersCount.collectAsState()
    val followingCount by viewModel.followingCount.collectAsState()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var pickTarget by remember { mutableStateOf<SongPickTarget?>(null) }
    var isEditDialogOpen by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        CosmicMeshBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            // ─── CABECERA FIJA ───────────────────────────────────────────────
            ProfileHeader(
                profile = profile,
                followers = followersCount,
                following = followingCount,
                downloads = downloadedCount,
                playlists = playlistsCount,
                onEdit = { isEditDialogOpen = true }
            )

            // ─── PESTAÑAS CON ICONOS ─────────────────────────────────────────
            ProfileTabsBar(
                selectedTab = selectedTab,
                onSelect = { selectedTab = it }
            )

            // ─── CONTENIDO DE LA PESTAÑA ACTIVA (esto sí hace scroll) ────────
            when (selectedTab) {
                0 -> FavoritesLevelTab(
                    xp = xp,
                    unlockedAchievements = unlockedAchievements,
                    pinnedFavorites = pinnedFavorites,
                    onPick = { pickTarget = it },
                    onOpenSong = onOpenSongReviews,
                    onUnpinFavorite = { viewModel.unpinFavorite(it) }
                )

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                        .testTag("profile_tab_reviews_content")
                ) {
                    // 1) El diario de reseñas que ya existía (aquí tampoco se repiten
                    //    las 5 favoritas: viven en la pestaña 1).
                    ReviewsJournalSection(
                        reviews = allReviews,
                        listenLater = listenLater,
                        pinnedFavorites = pinnedFavorites,
                        onOpenSong = onOpenSongReviews,
                        onPick = { pickTarget = it },
                        onUnpinFavorite = { viewModel.unpinFavorite(it) },
                        onRemoveListenLater = { viewModel.removeFromListenLater(it) },
                        // Las 5 favoritas viven en la pestaña 1: aquí no se duplican.
                        showPinnedFavorites = false
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    // 2) La gráfica de las notas que más das (con la ♪ a la izquierda).
                    //    Sale de las reseñas reales guardadas en la base de datos.
                    RatingsChartSection(reviews = allReviews)

                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }

        pickTarget?.let { target ->
            val closePicker = {
                pickTarget = null
                viewModel.searchSongsForReview("")
            }
            SongPickerDialog(
                title = when (target) {
                    SongPickTarget.Review -> "¿Qué canción quieres reseñar?"
                    SongPickTarget.ListenLater -> "Añadir a Por escuchar"
                    is SongPickTarget.Favorite -> "Elige tu favorita #${target.position + 1}"
                },
                localSongs = localSongs,
                remoteResults = reviewSearchResults,
                isSearching = isReviewSearching,
                onQueryChange = { viewModel.searchSongsForReview(it) },
                onPick = { song ->
                    when (target) {
                        SongPickTarget.Review -> onOpenSongReviews(song)
                        SongPickTarget.ListenLater -> viewModel.addToListenLater(song)
                        is SongPickTarget.Favorite -> viewModel.pinFavorite(target.position, song)
                    }
                    closePicker()
                },
                onDismiss = closePicker
            )
        }

        if (isEditDialogOpen) {
            EditProfileDialog(
                currentProfile = profile,
                onDismiss = { isEditDialogOpen = false },
                onSave = { updated ->
                    viewModel.updateProfile(updated)
                    isEditDialogOpen = false
                }
            )
        }
    }
}

// ─────────────────────────── CABECERA FIJA ───────────────────────────

@Composable
private fun ProfileHeader(
    profile: UserProfile,
    followers: Int,
    following: Int,
    downloads: Int,
    playlists: Int,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Accent, AccentHover)))
                    .border(2.dp, HoverBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getAvatarIcon(profile.avatarId),
                    contentDescription = "Avatar de ${profile.username}",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.username,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = profile.handle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Accent,
                    fontSize = 12.sp
                )
                if (profile.favoriteGenre.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AccentDim,
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Text(
                            text = profile.favoriteGenre,
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AccentDim,
                border = BorderStroke(1.dp, Accent),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onEdit() }
                    .testTag("edit_profile_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = Accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Editar",
                        color = Accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (profile.bio.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = profile.bio,
                color = TextSecondary,
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Contadores: seguidores y seguidos quedan en 0 hasta que haya Firebase.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_counters"),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ProfileCounter("Seguidores", followers, Modifier.weight(1f), "profile_followers_count")
            ProfileCounter("Seguidos", following, Modifier.weight(1f), "profile_following_count")
            ProfileCounter("Descargas", downloads, Modifier.weight(1f), "profile_downloads_count")
            ProfileCounter("Playlists", playlists, Modifier.weight(1f), "profile_playlists_count")
        }
    }
}

@Composable
private fun ProfileCounter(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    Column(
        modifier = if (testTag != null) modifier.testTag(testTag) else modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$value",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )
        Text(
            text = label,
            color = TextMuted,
            fontSize = 10.5.sp
        )
    }
}

// ─────────────────────── BARRA DE PESTAÑAS (ICONOS) ───────────────────────

@Composable
private fun ProfileTabsBar(
    selectedTab: Int,
    onSelect: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ProfileTab(
                icon = Icons.Default.Favorite,
                label = "Favoritas y nivel",
                selected = selectedTab == 0,
                testTag = "profile_tab_favorites",
                modifier = Modifier.weight(1f),
                onClick = { onSelect(0) }
            )
            ProfileTab(
                icon = Icons.Default.RateReview,
                label = "Reseñas",
                selected = selectedTab == 1,
                testTag = "profile_tab_reviews",
                modifier = Modifier.weight(1f),
                onClick = { onSelect(1) }
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(GlassBorder)
        )
    }
}

@Composable
private fun ProfileTab(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Accent else TextMuted,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (selected) TextPrimary else TextMuted,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(2.dp)
                .clip(CircleShape)
                .background(if (selected) Accent else Color.Transparent)
        )
    }
}

// ───────────────────── PESTAÑA 1: FAVORITAS + NIVEL ─────────────────────

@Composable
private fun FavoritesLevelTab(
    xp: Int,
    unlockedAchievements: Set<String>,
    pinnedFavorites: List<PinnedFavoriteEntity>,
    onPick: (SongPickTarget) -> Unit,
    onOpenSong: (SongRef) -> Unit,
    onUnpinFavorite: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("profile_tab_favorites_content")
    ) {
        // 5 favoritas fijadas (lo que ya existía)
        SectionLabel("MIS $FAVORITE_SLOTS FAVORITAS")
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (position in 0 until FAVORITE_SLOTS) {
                val pinned = pinnedFavorites.firstOrNull { it.position == position }
                PinnedFavoriteSlot(
                    position = position,
                    pinned = pinned,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (pinned == null) onPick(SongPickTarget.Favorite(position))
                        else onOpenSong(pinned.toSongRef())
                    },
                    onChange = { onPick(SongPickTarget.Favorite(position)) },
                    onRemove = { onUnpinFavorite(position) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Toca un hueco para fijar una canción · mantén pulsado para quitarla",
            color = TextMuted,
            fontSize = 10.5.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Nivel con XP real
        LevelCard(xp = xp)

        Spacer(modifier = Modifier.height(16.dp))

        // Logros
        AchievementsSection(unlockedAchievements = unlockedAchievements)

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun PinnedFavoriteSlot(
    position: Int,
    pinned: PinnedFavoriteEntity?,
    modifier: Modifier,
    onClick: () -> Unit,
    onChange: () -> Unit,
    onRemove: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(BgCard)
                .border(1.dp, if (pinned != null) HoverBorder else GlassBorder, RoundedCornerShape(10.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { if (pinned != null) menuOpen = true }
                )
                .testTag("pinned_favorite_slot_$position"),
            contentAlignment = Alignment.Center
        ) {
            if (pinned == null) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Fijar favorita ${position + 1}",
                    tint = TextMuted
                )
            } else {
                SongCover(
                    coverUrl = pinned.coverUrl,
                    title = pinned.songTitle,
                    size = null,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                            )
                        )
                )
                Text(
                    text = pinned.songTitle,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 10.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                )
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(text = { Text("Cambiar") }, onClick = { menuOpen = false; onChange() })
            DropdownMenuItem(text = { Text("Quitar") }, onClick = { menuOpen = false; onRemove() })
        }
    }
}

@Composable
private fun LevelCard(xp: Int) {
    val level = LevelCurve.levelForXp(xp)
    val progress = LevelCurve.levelProgress(xp)
    val xpIntoLevel = LevelCurve.xpIntoLevel(xp)
    val xpForNext = LevelCurve.xpForNextLevel(level)

    Card(
        colors = CardDefaults.cardColors(containerColor = BgSecondary),
        border = BorderStroke(1.dp, GlassBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_level_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Accent, AccentHover))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$level",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Nivel $level",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = LevelCurve.titleForLevel(level),
                        color = Accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "$xp XP",
                    color = CyanAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = Accent,
                trackColor = BgCard
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$xpIntoLevel / $xpForNext XP para el nivel ${level + 1}",
                    color = TextMuted,
                    fontSize = 11.sp
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Los números de XP, visibles para que se entienda de dónde sale el nivel.
            Text(
                text = "CÓMO GANAR XP",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            XpSource.values().forEach { source ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = source.label,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "+${source.xp} XP",
                        color = Accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ───────────────────────────── LOGROS ─────────────────────────────

@Composable
private fun AchievementsSection(unlockedAchievements: Set<String>) {
    val catalog = AchievementsCatalog.all
    val unlockedCount = remember(catalog, unlockedAchievements) {
        catalog.count { unlockedAchievements.contains(it.id) }
    }

    Column(modifier = Modifier.fillMaxWidth().testTag("profile_achievements")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel("LOGROS")
            Text(
                text = if (catalog.isEmpty()) "0 desbloqueados"
                else "$unlockedCount de ${catalog.size}",
                color = TextMuted,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (catalog.isEmpty()) {
            // El dueño todavía no ha traído la lista de logros: se muestra un estado
            // limpio (nunca un hueco vacío raro). Se llenan en AchievementsCatalog.all.
            Card(
                colors = CardDefaults.cardColors(containerColor = BgSecondary),
                border = BorderStroke(1.dp, GlassBorder),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(BgCard)
                            .border(1.dp, GlassBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Todavía no hay logros",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Descarga, reseña y crea playlists: tus logros aparecerán aquí.",
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        } else {
            catalog.forEach { achievement ->
                AchievementRow(
                    achievement = achievement,
                    unlocked = unlockedAchievements.contains(achievement.id)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AchievementRow(achievement: Achievement, unlocked: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (unlocked) BgSecondary else BgCard),
        border = BorderStroke(1.dp, if (unlocked) HoverBorder else GlassBorder),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("achievement_${achievement.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (unlocked) AccentDim else BgInput)
                    .border(1.dp, if (unlocked) HoverBorder else GlassBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (unlocked) {
                    Text(text = achievement.emoji, fontSize = 18.sp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Bloqueado",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.title,
                    color = if (unlocked) TextPrimary else TextMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = achievement.description,
                    color = TextSecondary,
                    fontSize = 11.5.sp
                )
            }
            if (unlocked) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─────────────────────────────── UI BASE ───────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = TextMuted,
        letterSpacing = 1.sp
    )
}

@Composable
private fun EditProfileDialog(
    currentProfile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    var username by remember { mutableStateOf(currentProfile.username) }
    var handle by remember { mutableStateOf(currentProfile.handle) }
    var bio by remember { mutableStateOf(currentProfile.bio) }
    var favoriteGenre by remember { mutableStateOf(currentProfile.favoriteGenre) }
    var audioSetup by remember { mutableStateOf(currentProfile.audioSetup) }
    var avatarId by remember { mutableIntStateOf(currentProfile.avatarId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgSecondary,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Personalizar Perfil",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Elige tu avatar cósmico:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (id in 1..4) {
                        val isSelected = avatarId == id
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Accent else BgCard)
                                .border(1.dp, if (isSelected) AccentHover else GlassBorder, CircleShape)
                                .clickable { avatarId = id },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getAvatarIcon(id),
                                contentDescription = null,
                                tint = if (isSelected) Color.White else TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Nombre de Usuario", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = BgInput,
                        unfocusedContainerColor = BgCard
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("edit_username_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it },
                    label = { Text("Handle / Usuario (ej: @deivi)", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = BgInput,
                        unfocusedContainerColor = BgCard
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Biografía / Descripción", color = TextSecondary) },
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = BgInput,
                        unfocusedContainerColor = BgCard
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("edit_bio_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = favoriteGenre,
                    onValueChange = { favoriteGenre = it },
                    label = { Text("Género Favorito", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = BgInput,
                        unfocusedContainerColor = BgCard
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = audioSetup,
                    onValueChange = { audioSetup = it },
                    label = { Text("Equipo / Auriculares Preferidos", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = BgInput,
                        unfocusedContainerColor = BgCard
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("edit_setup_input")
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "El equipo de sonido ya no se muestra en la cabecera del perfil; se guarda aquí por si vuelve a hacer falta.",
                    color = TextMuted,
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (username.isNotBlank()) {
                        onSave(
                            currentProfile.copy(
                                username = username.trim(),
                                handle = handle.trim(),
                                bio = bio.trim(),
                                favoriteGenre = favoriteGenre.trim(),
                                audioSetup = audioSetup.trim(),
                                avatarId = avatarId
                            )
                        )
                    }
                },
                enabled = username.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text("Guardar Cambios", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}

private fun getAvatarIcon(id: Int): ImageVector = when (id) {
    1 -> Icons.Default.Headphones
    2 -> Icons.Default.Album
    3 -> Icons.Default.GraphicEq
    4 -> Icons.Default.Equalizer
    else -> Icons.Default.MusicNote
}
