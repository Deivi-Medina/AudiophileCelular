package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ListenLaterEntity
import com.example.data.local.PinnedFavoriteEntity
import com.example.data.local.SongReviewEntity
import com.example.data.model.SongRef
import com.example.data.model.averageRating
import com.example.data.model.groupByMonth
import com.example.data.model.toSongRef
import com.example.ui.theme.Accent
import com.example.ui.theme.AccentDim
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgSecondary
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val PINNED_FAVORITES_COUNT = 5

sealed interface SongPickTarget {
    data object Review : SongPickTarget
    data object ListenLater : SongPickTarget
    data class Favorite(val position: Int) : SongPickTarget
}

private val esLocale: Locale = Locale.forLanguageTag("es")

/** Apartado tipo Letterboxd del perfil: estadísticas, 5 favoritas, diario y "Por escuchar". */
@Composable
fun ReviewsJournalSection(
    reviews: List<SongReviewEntity>,
    listenLater: List<ListenLaterEntity>,
    pinnedFavorites: List<PinnedFavoriteEntity>,
    onOpenSong: (SongRef) -> Unit,
    onPick: (SongPickTarget) -> Unit,
    onUnpinFavorite: (Int) -> Unit,
    onRemoveListenLater: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val months = remember(reviews) { reviews.groupByMonth() }
    val average = remember(reviews) { reviews.averageRating() }
    val songsCount = remember(reviews) { reviews.distinctBy { it.songId }.size }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("MI DIARIO MUSICAL", Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AccentDim,
                border = BorderStroke(1.dp, Accent),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onPick(SongPickTarget.Review) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.RateReview, contentDescription = null, tint = Accent, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reseñar", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            JournalStat("Reseñas", "${reviews.size}", Modifier.weight(1f))
            JournalStat("Canciones", "$songsCount", Modifier.weight(1f))
            JournalStat("Promedio", if (reviews.isEmpty()) "–" else "${formatRating(average)}★", Modifier.weight(1f))
            JournalStat("Por escuchar", "${listenLater.size}", Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(18.dp))
        SectionLabel("MIS $PINNED_FAVORITES_COUNT FAVORITAS")
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (position in 0 until PINNED_FAVORITES_COUNT) {
                val pinned = pinnedFavorites.firstOrNull { it.position == position }
                FavoriteSlot(
                    pinned = pinned,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (pinned == null) onPick(SongPickTarget.Favorite(position)) else onOpenSong(pinned.toSongRef())
                    },
                    onChange = { onPick(SongPickTarget.Favorite(position)) },
                    onRemove = { onUnpinFavorite(position) }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            JournalTab("Diario", selectedTab == 0) { selectedTab = 0 }
            JournalTab("Por escuchar (${listenLater.size})", selectedTab == 1) { selectedTab = 1 }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedTab == 0) {
            if (months.isEmpty()) {
                EmptyHint("Aún no has escrito reseñas. Toca «Reseñar» para buscar una canción.")
            }
            months.forEach { month ->
                Text(
                    text = month.label.uppercase(esLocale),
                    color = Accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
                )
                month.reviews.forEach { review ->
                    DiaryEntryRow(review, onClick = { onOpenSong(review.toSongRef()) })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = BgSecondary,
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onPick(SongPickTarget.ListenLater) }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Accent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Añadir canción", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (listenLater.isEmpty()) {
                EmptyHint("Guarda aquí canciones para reseñarlas después.")
            }
            listenLater.forEach { item ->
                ListenLaterRow(
                    item = item,
                    onReview = { onOpenSong(item.toSongRef()) },
                    onRemove = { onRemoveListenLater(item.songId) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = TextMuted,
        letterSpacing = 1.sp,
        modifier = modifier
    )
}

@Composable
private fun JournalStat(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BgSecondary),
        border = BorderStroke(1.dp, GlassBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
            Text(label, color = TextMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun JournalTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Accent else BgSecondary,
        border = BorderStroke(1.dp, if (selected) Accent else GlassBorder),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteSlot(
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
                .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { if (pinned != null) menuOpen = true }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (pinned == null) {
                Icon(Icons.Default.Add, contentDescription = "Fijar favorita", tint = TextMuted)
            } else {
                SongCover(coverUrl = pinned.coverUrl, title = pinned.songTitle, size = null, modifier = Modifier.fillMaxSize())
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
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
private fun DiaryEntryRow(review: SongReviewEntity, onClick: () -> Unit) {
    val date = Date(review.createdAt)
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, GlassBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.width(34.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(SimpleDateFormat("d", esLocale).format(date), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(SimpleDateFormat("EEE", esLocale).format(date), color = TextMuted, fontSize = 9.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            SongCover(coverUrl = review.coverUrl, title = review.songTitle, size = 46.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(review.songTitle, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(review.artist, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                RatingStars(rating = review.rating, starSize = 12.dp)
                if (review.comment.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(review.comment, color = TextSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun ListenLaterRow(item: ListenLaterEntity, onReview: () -> Unit, onRemove: () -> Unit) {
    Card(
        onClick = onReview,
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, GlassBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            SongCover(coverUrl = item.coverUrl, title = item.songTitle, size = 44.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.songTitle, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.artist, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Quitar de Por escuchar", tint = TextMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}
