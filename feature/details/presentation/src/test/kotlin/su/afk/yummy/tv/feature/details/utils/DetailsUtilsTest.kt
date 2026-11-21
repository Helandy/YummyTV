package su.afk.yummy.tv.feature.details.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.feature.details.model.DetailsWatchProgressIndex

class DetailsUtilsTest : BaseUnitTest() {

    private val kodik = AnimeVideo(
        id = 10, episode = "3", dubbing = "AniLibria", player = "Kodik", playerId = 1,
        iframeUrl = "https://kodik.example/seria/10", durationSeconds = 1_400,
    )
    private val dreamCast = AnimeVideo(
        id = -500, episode = "3", dubbing = "Dream Cast", player = "Dream Cast", playerId = null,
        iframeUrl = DREAM_CAST_URL, durationSeconds = null,
    )

    private fun progress(videoId: Int, episodeUrl: String, playerName: String) = AnimeWatchProgress(
        animeId = ANIME_ID,
        episode = "3",
        videoId = videoId,
        episodeUrl = episodeUrl,
        positionMs = 600_000L,
        durationMs = 1_400_000L,
        updatedAt = 1_000_000L,
        playerName = playerName,
        dubbing = "Dream Cast",
    )

    @Test
    fun `continue picks extra source by url even when entry keeps a stale Kodik video id`() {
        val index = DetailsWatchProgressIndex(
            listOf(progress(videoId = kodik.id, episodeUrl = DREAM_CAST_URL, playerName = "Dream Cast")),
        )

        val target = resolveDetailsContinueTarget(ANIME_ID, listOf(kodik, dreamCast), index)

        assertEquals(DREAM_CAST_URL, target!!.video.iframeUrl)
    }

    @Test
    fun `continue still matches regular source by video id`() {
        val index = DetailsWatchProgressIndex(
            listOf(progress(videoId = kodik.id, episodeUrl = kodik.iframeUrl, playerName = "Kodik")),
        )

        val target = resolveDetailsContinueTarget(ANIME_ID, listOf(dreamCast, kodik), index)

        assertEquals(kodik.iframeUrl, target!!.video.iframeUrl)
    }

    private companion object {
        const val ANIME_ID = 1
        const val DREAM_CAST_URL = "source://dreamerscast/slug/3"
    }
}
