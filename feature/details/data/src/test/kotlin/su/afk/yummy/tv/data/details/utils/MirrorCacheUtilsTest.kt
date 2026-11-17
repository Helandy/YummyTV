package su.afk.yummy.tv.data.details.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.storage.anime.ANIME_DETAIL_NAMED_KIND_GENRE
import su.afk.yummy.tv.core.storage.anime.AnimeDetailNamedEntry
import su.afk.yummy.tv.core.storage.anime.AnimeDetailsCache
import su.afk.yummy.tv.core.storage.anime.AnimeDetailsEntry
import su.afk.yummy.tv.core.storage.anime.AnimeVideoCacheEntry
import su.afk.yummy.tv.core.storage.anime.AnimeVideoEntry
import su.afk.yummy.tv.core.storage.anime.AnimeVideosCache
import su.afk.yummy.tv.core.testing.BaseUnitTest

class MirrorCacheUtilsTest : BaseUnitTest() {

    private fun video(id: Int, watched: Int? = null, subscribed: Boolean = false, opening: Long? = null) = AnimeVideoEntry(
        animeId = 1, language = "ru", position = id, videoId = id, episode = "1", dubbing = "D", player = "P",
        iframeUrl = "https://x/$id", watchedEndTimeSeconds = watched, subscribed = subscribed, openingStartMs = opening,
    )

    private fun videos(vararg v: AnimeVideoEntry) = AnimeVideosCache(AnimeVideoCacheEntry(1, "ru", 0), v.toList())

    private fun details(description: String, genres: List<AnimeDetailNamedEntry>) = AnimeDetailsCache(
        entry = AnimeDetailsEntry(animeId = 1, language = "ru", animeUrl = "", title = "T", description = description, cachedAt = 0),
        otherTitles = emptyList(), genres = genres, creators = emptyList(), studios = emptyList(),
        viewingOrder = emptyList(), screenshots = emptyList(),
    )

    @Test
    fun `copy videos keep server progress, subscription and skips of the same video`() {
        val stored = videos(video(10, watched = 600, subscribed = true, opening = 5_000))
        val fromCopy = videos(video(10), video(11))

        val merged = fromCopy.withUserStateFrom(stored)

        assertEquals(listOf(10, 11), merged.videos.map { it.videoId })
        assertEquals(600, merged.videos[0].watchedEndTimeSeconds)
        assertTrue(merged.videos[0].subscribed)
        assertEquals(5_000L, merged.videos[0].openingStartMs)
        assertEquals(null, merged.videos[1].watchedEndTimeSeconds)
    }

    @Test
    fun `copy videos pass through when nothing is stored`() {
        val fromCopy = videos(video(10))

        assertEquals(fromCopy, fromCopy.withUserStateFrom(null))
    }

    @Test
    fun `details without description and genres are minimal`() {
        val genre = AnimeDetailNamedEntry(1, "ru", ANIME_DETAIL_NAMED_KIND_GENRE, 0, 5, "Экшен", null)

        assertTrue(details("", emptyList()).isMinimal())
        assertFalse(details("Описание", emptyList()).isMinimal())
        assertFalse(details("", listOf(genre)).isMinimal())
    }
}
