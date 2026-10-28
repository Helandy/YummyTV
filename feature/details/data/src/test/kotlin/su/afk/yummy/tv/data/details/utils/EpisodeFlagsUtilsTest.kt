package su.afk.yummy.tv.data.details.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeEpisodes

class EpisodeFlagsUtilsTest {

    private fun episodes(count: Int?, aired: Int?, next: Long? = 0L) =
        AnimeEpisodes(count = count, aired = aired, nextDateEpochSeconds = next, prevDateEpochSeconds = 0L)

    @Test
    fun `all episodes aired and no next date means finished`() {
        assertTrue(episodes(count = 148, aired = 148).isFullyAired())
    }

    @Test
    fun `episodes still to come mean ongoing`() {
        assertFalse(episodes(count = 12, aired = 5).isFullyAired())
        assertFalse(episodes(count = 12, aired = 12, next = 1_800_000_000L).isFullyAired())
    }

    @Test
    fun `unknown counts are treated as ongoing`() {
        assertFalse(episodes(count = null, aired = 3).isFullyAired())
        assertFalse(episodes(count = 0, aired = 0).isFullyAired())
        assertFalse((null as AnimeEpisodes?).isFullyAired())
    }
}
