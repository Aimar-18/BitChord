package com.music.bitchord.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoBrowsePagingTest {
    @Test
    fun `an empty list has no parts`() {
        assertTrue(autoSongParts(0).isEmpty())
        assertTrue(autoSongParts(-1).isEmpty())
    }

    @Test
    fun `543 songs split into two ordered parts covering every song once`() {
        val parts = autoSongParts(543)

        assertEquals(2, parts.size)
        assertEquals(0, parts.first().start)
        assertEquals(543, parts.last().endExclusive)
        assertEquals(543, parts.sumOf { it.count })
        parts.zipWithNext().forEach { (left, right) ->
            assertEquals(left.endExclusive, right.start)
        }
        parts.forEachIndexed { index, part ->
            assertEquals(index, part.index)
            assertTrue(part.count in 1..AUTO_SONGS_PER_PART)
        }
    }

    @Test
    fun `a list exactly at the part size stays one part`() {
        val parts = autoSongParts(AUTO_SONGS_PER_PART)

        assertEquals(1, parts.size)
        assertEquals(0, parts.single().start)
        assertEquals(AUTO_SONGS_PER_PART, parts.single().endExclusive)
    }

    @Test
    fun `the last part keeps the remainder instead of rounding up`() {
        val parts = autoSongParts(543)

        assertEquals(43, parts.last().count)
    }

    @Test
    fun `a list just over two host ceilings adds a small third part`() {
        val parts = autoSongParts(AUTO_MAX_ITEMS_PER_FOLDER * 2 + 1)

        assertEquals(3, parts.size)
        assertEquals(1, parts.last().count)
    }
}
