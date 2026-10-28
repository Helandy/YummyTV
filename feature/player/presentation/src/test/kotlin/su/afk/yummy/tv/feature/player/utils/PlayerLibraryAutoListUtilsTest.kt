package su.afk.yummy.tv.feature.player.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.UserAnimeList

class PlayerLibraryAutoListUtilsTest : BaseUnitTest() {
    private val minute = 60_000L
    private val duration = 24 * minute

    private fun progress(episode: String, positionMs: Long) = AnimeWatchProgress(
        animeId = 1,
        episode = episode,
        episodeUrl = "url$episode",
        positionMs = positionMs,
        durationMs = duration,
        updatedAt = 0L,
    )

    private val half = progress("1", 13 * minute)
    private val full = 23 * minute

    @Test
    fun `one half-watched episode is not enough`() {
        assertNull(autoLibraryTarget(listOf(half), "released", 12))
    }

    @Test
    fun `two episodes over half move title to watching`() {
        val target = autoLibraryTarget(listOf(half, progress("2", 13 * minute)), "ongoing", 12)
        assertEquals(UserAnimeList.WATCHING, target)
    }

    @Test
    fun `episode under half does not count`() {
        val target = autoLibraryTarget(listOf(half, progress("2", 5 * minute)), "ongoing", 12)
        assertNull(target)
    }

    @Test
    fun `same episode in two dubbings counts once`() {
        val target = autoLibraryTarget(listOf(half, progress("01", 14 * minute)), "ongoing", 12)
        assertNull(target)
    }

    @Test
    fun `all episodes of released title move to completed`() {
        val all = listOf(progress("1", full), progress("2", full))
        assertEquals(UserAnimeList.COMPLETED, autoLibraryTarget(all, "released", 2))
    }

    @Test
    fun `ongoing title never completes`() {
        val all = listOf(progress("1", full), progress("2", full))
        assertEquals(UserAnimeList.WATCHING, autoLibraryTarget(all, "ongoing", 2))
    }

    @Test
    fun `released title with unfinished episodes stays watching`() {
        val partial = listOf(progress("1", full), progress("2", full))
        assertEquals(UserAnimeList.WATCHING, autoLibraryTarget(partial, "released", 12))
    }

    @Test
    fun `unknown episode count never completes`() {
        val all = listOf(progress("1", full), progress("2", full))
        assertEquals(UserAnimeList.WATCHING, autoLibraryTarget(all, "released", null))
    }

    @Test
    fun `automation only promotes lists`() {
        assertTrue(null.canAutoMoveTo(UserAnimeList.WATCHING))
        assertTrue(UserAnimeList.PLANNED.canAutoMoveTo(UserAnimeList.WATCHING))
        assertTrue(UserAnimeList.WATCHING.canAutoMoveTo(UserAnimeList.COMPLETED))
        assertFalse(UserAnimeList.WATCHING.canAutoMoveTo(UserAnimeList.WATCHING))
        assertFalse(UserAnimeList.COMPLETED.canAutoMoveTo(UserAnimeList.WATCHING))
        assertFalse(UserAnimeList.DROPPED.canAutoMoveTo(UserAnimeList.WATCHING))
        assertFalse(UserAnimeList.POSTPONED.canAutoMoveTo(UserAnimeList.COMPLETED))
    }
}
