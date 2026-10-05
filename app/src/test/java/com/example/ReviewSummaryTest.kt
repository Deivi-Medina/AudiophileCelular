package com.example

import com.example.data.local.SongReviewEntity
import com.example.data.model.averageRating
import com.example.data.model.summarizeBySong
import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewSummaryTest {

    private fun review(songId: String, rating: Float, createdAt: Long, title: String = "T$songId") =
        SongReviewEntity(
            songId = songId,
            songTitle = title,
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
    fun `average rating`() {
        val reviews = listOf(review("1", 5f, 1), review("1", 4f, 2), review("1", 3f, 3))
        assertEquals(4f, reviews.averageRating(), 0.0001f)
    }

    @Test
    fun `summarize groups by song and orders by latest review`() {
        val reviews = listOf(
            review("a", 5f, 10, title = "Vieja"),
            review("b", 2f, 20),
            review("a", 3f, 30, title = "Nueva")
        )

        val summary = reviews.summarizeBySong()

        assertEquals(listOf("a", "b"), summary.map { it.songId })
        assertEquals(2, summary[0].reviewCount)
        assertEquals(4f, summary[0].averageRating, 0.0001f)
        assertEquals(30L, summary[0].lastReviewedAt)
        assertEquals("Nueva", summary[0].songTitle)
        assertEquals(1, summary[1].reviewCount)
    }
}
