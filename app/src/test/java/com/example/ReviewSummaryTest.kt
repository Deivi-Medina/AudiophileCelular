package com.example

import com.example.data.local.SongReviewEntity
import com.example.data.model.averageRating
import com.example.data.model.groupByMonth
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class ReviewSummaryTest {

    private fun at(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply { set(year, month, day, 12, 0, 0) }.timeInMillis

    private fun review(songId: String, rating: Float, createdAt: Long) =
        SongReviewEntity(
            songId = songId,
            songTitle = "T$songId",
            artist = "A",
            authorName = "Yo",
            rating = rating,
            comment = "c",
            createdAt = createdAt
        )

    @Test
    fun `average of empty list is zero`() {
        assertEquals(0f, emptyList<SongReviewEntity>().averageRating(), 0.0001f)
    }

    @Test
    fun `average supports half stars`() {
        val reviews = listOf(review("1", 4.5f, 1), review("1", 3.5f, 2))
        assertEquals(4f, reviews.averageRating(), 0.0001f)
    }

    @Test
    fun `groups diary by month, newest first`() {
        val reviews = listOf(
            review("a", 5f, at(2026, Calendar.SEPTEMBER, 3)),
            review("b", 2f, at(2026, Calendar.OCTOBER, 1)),
            review("c", 3f, at(2026, Calendar.OCTOBER, 4)),
            review("d", 1f, at(2025, Calendar.OCTOBER, 4))
        )

        val months = reviews.groupByMonth()

        assertEquals(listOf("Octubre 2026", "Septiembre 2026", "Octubre 2025"), months.map { it.label })
        assertEquals(listOf("c", "b"), months[0].reviews.map { it.songId })
    }
}
