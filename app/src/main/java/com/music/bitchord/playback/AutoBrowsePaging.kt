package com.music.bitchord.playback

/**
 * Android Auto does not paginate a browse node and silently drops the tail of
 * a list that is too long (see `onGetChildren`). Keep every directly returned
 * level under that ceiling, and split anything larger into browsable parts.
 *
 * 500 is the ceiling reported by other media apps and the reason a 543-song
 * playlist stops at 500 rows in the car. Parts use that same ceiling rather
 * than a smaller chunk: a playlist that already fits in one node is returned
 * whole, and one that does not is shown in as few parts as the host allows.
 * The app already sends a 500-song playlist as one list, so a 500-song part
 * is the same payload that path has always used.
 */
internal const val AUTO_MAX_ITEMS_PER_FOLDER = 500
internal const val AUTO_SONGS_PER_PART = AUTO_MAX_ITEMS_PER_FOLDER

/** Half-open range `[start, endExclusive)` of a playlist's song list. */
internal data class AutoSongPart(
    val index: Int,
    val start: Int,
    val endExclusive: Int,
) {
    val count: Int get() = endExclusive - start
}

/**
 * Splits [total] songs into consecutive parts that cover every index exactly
 * once, in order. Empty input has no parts; a list at or below [AUTO_SONGS_PER_PART]
 * still gets one part so callers can use the same path.
 */
internal fun autoSongParts(total: Int): List<AutoSongPart> {
    if (total <= 0) return emptyList()
    val parts = ArrayList<AutoSongPart>((total + AUTO_SONGS_PER_PART - 1) / AUTO_SONGS_PER_PART)
    var start = 0
    var index = 0
    while (start < total) {
        val end = minOf(start + AUTO_SONGS_PER_PART, total)
        parts += AutoSongPart(index = index, start = start, endExclusive = end)
        start = end
        index++
    }
    return parts
}
