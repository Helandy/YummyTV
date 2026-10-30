package su.afk.yummy.tv.feature.player.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.feature.player.PlayerSourceBalancer
import su.afk.yummy.tv.feature.player.PlayerSourceDubbing
import su.afk.yummy.tv.feature.player.PlayerSourceEpisode
import su.afk.yummy.tv.feature.player.PlayerSourceGraph
import su.afk.yummy.tv.feature.player.model.PlayerEpisodeProgress

class PlayerEpisodeProgressUtilsTest : BaseUnitTest() {

    private val graph = PlayerSourceGraph(
        balancers = listOf(
            PlayerSourceBalancer(
                name = "Kodik",
                dubbings = listOf(
                    PlayerSourceDubbing(
                        name = "AniLibria",
                        episodes = listOf(
                            // Начата на другом устройстве: 11:24 из 24:00.
                            PlayerSourceEpisode(
                                id = 1,
                                number = "01",
                                watchedEndTimeSeconds = 684,
                                watchedDateSeconds = 100L,
                                durationSeconds = 1_440,
                            ),
                            PlayerSourceEpisode(id = 2, number = "2"),
                        ),
                    ),
                ),
            ),
        ),
    )

    @Test
    fun `server progress shows up without any local entry and matches 01 with 1`() {
        val merged = graph.mergedWatchProgress(ANIME_ID, emptyList())

        assertEquals(
            PlayerEpisodeProgress.InProgress(label = "11:24 / 24:00", progress = 684f / 1_440f),
            merged.episodeProgress("1"),
        )
        assertEquals(PlayerEpisodeProgress.None, merged.episodeProgress("2"))
    }

    @Test
    fun `newer local progress wins over server and watched shows full duration`() {
        val local = AnimeWatchProgress(
            animeId = ANIME_ID,
            episode = "1",
            episodeUrl = "",
            positionMs = 1_430_000L,
            durationMs = 1_440_000L,
            updatedAt = 200_000L,
        )

        val merged = graph.mergedWatchProgress(ANIME_ID, listOf(local))

        assertEquals(PlayerEpisodeProgress.Watched("24:00"), merged.episodeProgress("1"))
    }

    private companion object {
        const val ANIME_ID = 42
    }
}
