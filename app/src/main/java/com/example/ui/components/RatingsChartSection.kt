package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SongReviewEntity
import com.example.data.model.RATING_BUCKETS
import com.example.data.model.mostUsedRating
import com.example.data.model.ratingBucketIndex
import com.example.data.model.ratingBucketLabel
import com.example.data.model.ratingHistogram
import com.example.data.model.ratingStarsLabel
import com.example.ui.theme.Accent
import com.example.ui.theme.AccentHover
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgSecondary
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

// ───────────────────────── MEDIDAS DE LA GRÁFICA ─────────────────────────
// La gráfica es un arco: la nota musical ♪ queda a la izquierda y las 10 filas
// (de ½★ a 5★) salen a su derecha. Los extremos del abanico se acercan a la nota
// siguiendo la circunferencia de radio [ARC_RADIUS], y cada fila se dibuja como
// una barra redondeada ligeramente curvada (bow de [ROW_BOW]).

/** Alto de cada una de las 10 filas. 10 x 21dp = 210dp de gráfica. */
private val ROW_HEIGHT = 21.dp

/** Grosor de las barras (y de la pista gris que va debajo). */
private val BAR_STROKE = 8.dp

/** Radio del arco al que se pegan las filas: más radio = arco más plano. */
private const val ARC_RADIUS_DP = 300f

/** Cuánto se curva cada fila (la mitad del levantamiento de su punto medio). */
private val ROW_BOW = 5.dp

/** Color de la pista apagada de cada fila. */
private val TrackColor = Color(0x1AFFFFFF)

/** Tamaño de la nota musical grande y del icono dentro de ella. */
private val NOTE_BADGE = 84.dp
private val NOTE_ICON = 42.dp

/**
 * Gráfica "las notas que más das" del perfil (pestaña 2).
 *
 *  - Nota musical ♪ grande a la izquierda.
 *  - A su derecha, 10 filas en arco, una por nota de ½ en ½ (½★, 1★, 1½★ … 5★).
 *  - Cada fila cuenta las reseñas con esa nota; la más usada va resaltada.
 *  - Debajo, "Tu nota más usada: N★".
 *
 * Con [reviews] vacío no se pinta una gráfica vacía: se pinta un estado cuidado
 * con la nota musical y una explicación.
 */
@Composable
fun RatingsChartSection(
    reviews: List<SongReviewEntity>,
    modifier: Modifier = Modifier
) {
    val histogram = remember(reviews) { reviews.ratingHistogram() }
    val mostUsed = remember(reviews) { reviews.mostUsedRating() }
    val maxCount = histogram.maxOrNull() ?: 0
    val mostUsedIndex = mostUsed?.let { ratingIndexForStars(it) } ?: -1

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "LAS NOTAS QUE MÁS DAS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = BgSecondary),
            border = BorderStroke(1.dp, GlassBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_ratings_chart")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (maxCount == 0) {
                    EmptyRatingsState()
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MusicNoteBadge()
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            // De 5★ arriba a ½★ abajo.
                            for (index in RATING_BUCKETS - 1 downTo 0) {
                                RatingRow(
                                    index = index,
                                    count = histogram[index],
                                    maxCount = maxCount,
                                    highlighted = index == mostUsedIndex
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tu nota más usada: ",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = mostUsed?.let { ratingStarsLabel(it) } ?: "–",
                            color = Accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        mostUsed?.let {
                            Spacer(modifier = Modifier.width(6.dp))
                            RatingStars(rating = it, starSize = 13.dp)
                        }
                    }
                }
            }
        }
    }
}

/** Índice de fila (0 = ½★) de una nota, para resaltar la fila correcta. */
private fun ratingIndexForStars(rating: Float): Int = ratingBucketIndex(rating)

/** La ♪ grande de la izquierda. Es el ancla visual de toda la gráfica. */
@Composable
private fun MusicNoteBadge() {
    Box(
        modifier = Modifier
            .size(NOTE_BADGE)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Accent, AccentHover)))
            .border(1.dp, GlassBorder, CircleShape)
            .testTag("profile_ratings_note"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = "Tus notas",
            tint = Color.White,
            modifier = Modifier.size(NOTE_ICON)
        )
    }
}

/**
 * Una fila del arco: etiqueta de la nota, barra curvada con la cuenta y el número.
 */
@Composable
private fun RatingRow(
    index: Int,
    count: Int,
    maxCount: Int,
    highlighted: Boolean
) {
    val middle = (RATING_BUCKETS - 1) / 2f
    val fraction = if (maxCount <= 0) 0f else count.toFloat() / maxCount.toFloat()
    // Arco: las filas de los extremos se acercan a la nota ♪ (0dp en el centro,
    // ~15dp en 5★ y ½★) siguiendo una circunferencia de radio ARC_RADIUS_DP.
    val distanceFromMiddleDp = (index - middle) * ROW_HEIGHT.value
    val startInset = (distanceFromMiddleDp * distanceFromMiddleDp / (2f * ARC_RADIUS_DP)).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .testTag("rating_row_$index"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = ratingBucketLabel(index),
            color = if (highlighted) Accent else TextMuted,
            fontSize = 10.5.sp,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.width(32.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .height(ROW_HEIGHT)
                .drawBehind {
                    drawArcRow(
                        startInset = startInset,
                        fraction = fraction,
                        highlighted = highlighted
                    )
                }
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "$count",
            color = if (highlighted) TextPrimary else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.End,
            modifier = Modifier.width(22.dp)
        )
    }
}

/**
 * Dibuja la fila: la pista completa en gris muy tenue y encima la barra real.
 * Las dos son arcos (curvados hacia arriba) y las filas de los extremos empiezan
 * un poco más a la derecha, así el conjunto sale del arco que rodea a la ♪.
 */
private fun DrawScope.drawArcRow(
    startInset: Dp,
    fraction: Float,
    highlighted: Boolean
) {
    val bow = ROW_BOW.toPx()
    val stroke = BAR_STROKE.toPx()

    val startX = startInset.toPx().coerceIn(0f, size.width / 3f)
    val endX = size.width
    val baseY = size.height * 0.58f

    fun bentPath(fromX: Float, toX: Float): Path = Path().apply {
        moveTo(fromX, baseY)
        quadraticBezierTo((fromX + toX) / 2f, baseY - 2f * bow, toX, baseY)
    }

    // Pista: siempre completa, para que se vea el 100% posible de cada nota.
    drawPath(
        path = bentPath(startX, endX),
        color = TrackColor,
        style = Stroke(width = stroke, cap = StrokeCap.Round)
    )

    if (fraction <= 0f) return

    val valueEnd = startX + (endX - startX) * fraction.coerceIn(0f, 1f)

    if (highlighted) {
        // Halo suave para marcar la nota más usada.
        drawPath(
            path = bentPath(startX, valueEnd),
            color = Accent.copy(alpha = 0.30f),
            style = Stroke(width = stroke * 2.4f, cap = StrokeCap.Round)
        )
    }

    drawPath(
        path = bentPath(startX, valueEnd),
        color = if (highlighted) AccentHover else Accent,
        style = Stroke(width = stroke, cap = StrokeCap.Round)
    )
}

/** Estado sin reseñas: la ♪ manda y se explica qué aparecerá aquí. */
@Composable
private fun EmptyRatingsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("profile_ratings_empty"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MusicNoteBadge()
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Todavía no le has puesto nota a nada",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Cuando reseñes tus canciones, aquí verás qué notas usas más " +
                "(de ½★ a 5★).",
            color = TextSecondary,
            fontSize = 11.5.sp,
            lineHeight = 15.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Toca «Reseñar» para empezar",
            color = Accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
