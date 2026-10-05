package com.example.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.ceil
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SongReviewEntity
import com.example.data.model.averageRating
import com.example.ui.theme.Accent
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgInput
import com.example.ui.theme.BgSecondary
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val StarGold = Color(0xFFFFB800)
private val DeleteRed = Color(0xFFFF453A)
private val SpanishLocale: Locale = Locale.forLanguageTag("es")

fun formatReviewDate(millis: Long): String =
    SimpleDateFormat("d MMM yyyy", SpanishLocale).format(Date(millis))

fun formatRating(rating: Float): String = String.format(SpanishLocale, "%.1f", rating)

/** Fila de 5 estrellas con soporte de medias estrellas (para promedios). */
@Composable
fun RatingStars(rating: Float, starSize: Dp, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..5) {
            val icon = when {
                rating >= i - 0.25f -> Icons.Default.Star
                rating >= i - 0.75f -> Icons.AutoMirrored.Filled.StarHalf
                else -> Icons.Default.StarBorder
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (icon == Icons.Default.StarBorder) TextMuted else StarGold,
                modifier = Modifier.size(starSize)
            )
        }
    }
}

/** Selector de 1/2 a 5 estrellas: toca o arrastra sobre las estrellas. */
@Composable
fun RatingPicker(rating: Float, onRatingChange: (Float) -> Unit, starSize: Dp = 34.dp) {
    val currentOnChange by rememberUpdatedState(onRatingChange)
    var widthPx by remember { mutableIntStateOf(1) }
    fun ratingAt(x: Float): Float = (ceil(x / widthPx * 10f) / 2f).coerceIn(0.5f, 5f)
    RatingStars(
        rating = rating,
        starSize = starSize,
        modifier = Modifier
            .onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
            .pointerInput(Unit) { detectTapGestures { currentOnChange(ratingAt(it.x)) } }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ -> currentOnChange(ratingAt(change.position.x)) }
            }
    )
}

@Composable
fun SongReviewDialog(
    visible: Boolean,
    songTitle: String,
    artist: String,
    existingReviews: List<SongReviewEntity>,
    onDismiss: () -> Unit,
    onSubmitReview: (rating: Float, comment: String) -> Unit,
    onUpdateReview: (reviewId: Long, rating: Float, comment: String) -> Unit,
    onDeleteReview: (reviewId: Long) -> Unit,
    isInListenLater: Boolean = false,
    onToggleListenLater: (() -> Unit)? = null
) {
    if (!visible) return

    var rating by remember { mutableFloatStateOf(5.0f) }
    var comment by remember { mutableStateOf("") }
    var isFormOpen by remember { mutableStateOf(false) }
    var editingReviewId by remember { mutableStateOf<Long?>(null) }
    var reviewPendingDelete by remember { mutableStateOf<SongReviewEntity?>(null) }

    fun openForm(review: SongReviewEntity?) {
        editingReviewId = review?.id
        rating = review?.rating ?: 5f
        comment = review?.comment ?: ""
        isFormOpen = true
    }

    fun closeForm() {
        isFormOpen = false
        editingReviewId = null
        rating = 5f
        comment = ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgSecondary,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Reseñas",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "$songTitle - $artist",
                        color = Accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2
                    )
                    if (existingReviews.isNotEmpty()) {
                        val average = existingReviews.averageRating()
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RatingStars(rating = average, starSize = 16.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${formatRating(average)} · " +
                                    if (existingReviews.size == 1) "1 reseña" else "${existingReviews.size} reseñas",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isFormOpen) {
                    Text(
                        text = if (editingReviewId != null) "Editar reseña" else "Nueva reseña",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RatingPicker(rating = rating, onRatingChange = { rating = it })
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = formatRating(rating),
                            color = StarGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Text("Toca o desliza; puedes poner medias estrellas", color = TextMuted, fontSize = 10.sp)

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Tu reseña", color = TextSecondary) },
                        placeholder = { Text("Comenta sobre la producción, masterización, escena sonora...", color = TextSecondary.copy(alpha = 0.5f)) },
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = BgInput,
                            unfocusedContainerColor = BgCard
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("review_comment_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { closeForm() }) {
                            Text("Cancelar", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val text = comment.trim()
                                if (text.isNotEmpty()) {
                                    val id = editingReviewId
                                    if (id != null) onUpdateReview(id, rating, text) else onSubmitReview(rating, text)
                                    closeForm()
                                }
                            },
                            enabled = comment.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Accent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (editingReviewId != null) "Guardar" else "Publicar",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onToggleListenLater != null) {
                            TextButton(onClick = onToggleListenLater) {
                                Icon(
                                    imageVector = if (isInListenLater) Icons.Default.BookmarkAdded else Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    tint = if (isInListenLater) Accent else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isInListenLater) "En Por escuchar" else "Por escuchar",
                                    color = if (isInListenLater) Accent else TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        } else {
                            Text(
                                text = if (existingReviews.isEmpty()) "Sin reseñas" else "Historial",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Button(
                            onClick = { openForm(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = Accent),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Escribir reseña", fontSize = 11.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (existingReviews.isEmpty()) {
                        Text(
                            text = "Aún no hay reseñas para esta canción. ¡Escribe la primera!",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(existingReviews, key = { it.id }) { review ->
                                ReviewCard(
                                    review = review,
                                    onEdit = { openForm(review) },
                                    onDelete = { reviewPendingDelete = review }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )

    reviewPendingDelete?.let { review ->
        AlertDialog(
            onDismissRequest = { reviewPendingDelete = null },
            containerColor = BgSecondary,
            title = { Text("¿Borrar reseña?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Esta acción no se puede deshacer.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteReview(review.id)
                    reviewPendingDelete = null
                }) {
                    Text("Borrar", color = DeleteRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { reviewPendingDelete = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun ReviewCard(
    review: SongReviewEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, GlassBorder),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(start = 12.dp, top = 8.dp, end = 4.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.authorName,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RatingStars(rating = review.rating, starSize = 13.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formatReviewDate(review.createdAt),
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar reseña",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Borrar reseña",
                        tint = DeleteRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = review.comment,
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}
