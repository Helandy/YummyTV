package su.afk.yummy.tv.feature.details.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.testing.BaseUnitTest

class Anime4kDubbingUtilsTest : BaseUnitTest() {

    private val videos = listOf(
        video(1, "AniLibria", views = 10),
        video(2, "AlexFilm", views = 50),
        video(3, "Shiza", views = 5),
    )

    @Test
    fun `remembered dubbing wins over target and popularity`() {
        assertEquals(3, pickBalancerVideo(videos, "Shiza", "AniLibria")?.id)
    }

    @Test
    fun `missing remembered dubbing falls back to the target dubbing`() {
        assertEquals(1, pickBalancerVideo(videos, "Other", "AniLibria")?.id)
    }

    @Test
    fun `no remembered and no target match falls back to the most viewed`() {
        assertEquals(2, pickBalancerVideo(videos, null, "Missing")?.id)
    }

    @Test
    fun `remembered dubbing is matched loosely by name`() {
        assertEquals(2, pickBalancerVideo(videos, "alexfilm", "Missing")?.id)
    }

    @Test
    fun `empty list yields null`() {
        assertNull(pickBalancerVideo(emptyList(), "Shiza", "AniLibria"))
    }

    private fun video(id: Int, dubbing: String, views: Int) = AnimeVideo(
        id = id, episode = "1", dubbing = dubbing, player = "4K", playerId = null,
        iframeUrl = "source://anime4k/1/1/1/$dubbing", durationSeconds = null, views = views,
    )
}
