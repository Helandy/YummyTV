package su.afk.yummy.tv.feature.details.model

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.testing.BaseUnitTest

class DetailsWatchProgressIndexMergeTest : BaseUnitTest() {

    private val local4k = AnimeWatchProgress(
        animeId = ANIME_ID,
        episode = "3",
        episodeUrl = "source://anime4k/1/1/3/AniLibria",
        positionMs = 100_000L,
        durationMs = 1_400_000L,
        updatedAt = 1_000_000L,
        playerName = "4K",
        dubbing = "AniLibria",
    )

    private fun serverVideo(
        dubbing: String = "AniLibria",
        positionSeconds: Int,
        dateSeconds: Long = 1_005L,
    ) = AnimeVideo(
        id = 77, episode = "3", dubbing = dubbing, player = "Aksor", playerId = null,
        iframeUrl = "https://aksor.example/v/77", durationSeconds = 1_400,
        watchedEndTimeSeconds = positionSeconds, watchedDateSeconds = dateSeconds,
    )

    @Test
    fun `server mirror of a 4K entry does not displace it`() {
        val merged = DetailsWatchProgressIndex.merge(
            ANIME_ID, listOf(local4k), listOf(serverVideo(positionSeconds = 98)),
        )

        val entry = merged.latestMeaningful(ANIME_ID)!!
        assertEquals("4K", entry.playerName)
        assertEquals(1_005_000L, entry.updatedAt)
    }

    @Test
    fun `server entry that moved ahead wins`() {
        val merged = DetailsWatchProgressIndex.merge(
            ANIME_ID, listOf(local4k), listOf(serverVideo(positionSeconds = 600)),
        )

        assertEquals("Aksor", merged.latestMeaningful(ANIME_ID)!!.playerName)
    }

    @Test
    fun `other dubbing newer on the server wins`() {
        val merged = DetailsWatchProgressIndex.merge(
            ANIME_ID, listOf(local4k), listOf(serverVideo(dubbing = "Shiza", positionSeconds = 98)),
        )

        assertEquals("Aksor", merged.latestMeaningful(ANIME_ID)!!.playerName)
    }

    private companion object {
        const val ANIME_ID = 166
    }
}
