package com.example.ui.screens

import com.example.data.model.SongRef
import com.example.ui.components.ReviewsJournalSection
import com.example.ui.components.SongPickTarget
import com.example.ui.components.SongPickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserProfile
import com.example.ui.components.CosmicMeshBackground
import com.example.ui.theme.Accent
import com.example.ui.theme.AccentDim
import com.example.ui.theme.AccentHover
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgInput
import com.example.ui.theme.BgSecondary
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.HoverBorder
import com.example.ui.theme.OfflineGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SearchAndDownloadViewModel

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
    var pickTarget by remember { mutableStateOf<SongPickTarget?>(null) }
    var isEditDialogOpen by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        CosmicMeshBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Perfil Header con información personalizable
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar dinámico
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Accent, AccentHover))
                        )
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

                Spacer(modifier = Modifier.width(16.dp))

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
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AccentDim,
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Text(
                            text = "Audiófilo VIP • ${profile.favoriteGenre}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Botón Editar Perfil
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AccentDim,
                    border = BorderStroke(1.dp, Accent),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { isEditDialogOpen = true }
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

            Spacer(modifier = Modifier.height(14.dp))

            // Biografía / Descripción del usuario
            if (profile.bio.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BgSecondary),
                    border = BorderStroke(1.dp, GlassBorder),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = profile.bio,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Setup de audio / Auriculares preferidos
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
                    Icon(
                        imageVector = Icons.Default.Speaker,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Equipo de Sonido Preferido",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = profile.audioSetup,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ReviewsJournalSection(
                reviews = allReviews,
                listenLater = listenLater,
                pinnedFavorites = pinnedFavorites,
                onOpenSong = onOpenSongReviews,
                onPick = { pickTarget = it },
                onUnpinFavorite = { viewModel.unpinFavorite(it) },
                onRemoveListenLater = { viewModel.removeFromListenLater(it) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Estadísticas Reales
            Text(
                text = "ESTADÍSTICAS DEL SISTEMA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Descargas",
                    value = "$downloadedCount",
                    unit = "Pistas 320k",
                    accentColor = OfflineGreen,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Playlists",
                    value = "$playlistsCount",
                    unit = "Listas Propias",
                    accentColor = Accent,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Nivel",
                    value = "5",
                    unit = "Maestro Hi-Fi",
                    accentColor = CyanAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Barra de Nivel de Melómano
            Card(
                colors = CardDefaults.cardColors(containerColor = BgSecondary),
                border = BorderStroke(1.dp, GlassBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Progreso Audiófilo",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${(downloadedCount * 150 + 750)} / 2000 XP",
                            color = Accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val progressRatio = ((downloadedCount * 150 + 750) / 2000f).coerceIn(0.1f, 1f)
                    LinearProgressIndicator(
                        progress = { progressRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = Accent,
                        trackColor = BgCard
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
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

        // Diálogo para personalizar perfil
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

@Composable
private fun StatCard(
    title: String,
    value: String,
    unit: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BgSecondary),
        border = BorderStroke(1.dp, GlassBorder),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = accentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = unit,
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
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

    val genres = listOf("Rock Clásico", "Electronic", "Rock Progresivo", "Synthwave", "Hip-Hop", "Jazz & Blues", "Metal")

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
                // Selector de avatar
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
    else -> Icons.Default.Person
}
