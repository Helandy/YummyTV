package su.afk.yummy.tv.feature.details.episodes.utils

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.feature.details.episodes.EpisodesState.EpisodeGroup
import su.afk.yummy.tv.feature.details.episodes.model.EpisodeWatchFilter
import su.afk.yummy.tv.feature.details.model.DetailsWatchProgressIndex

class EpisodeFilterUtilsTest {

    private val groups = (1..3).map { group(it) }.toImmutableList()

    // 1 — досмотрена, 2 — в процессе, 3 — не начата.
    private val progress = DetailsWatchProgressIndex(
        listOf(
            entry(videoId = 1, episode = "1", positionMs = 1_400_000, durationMs = 1_400_000),
            entry(videoId = 2, episode = "2", positionMs = 300_000, durationMs = 1_400_000),
        ),
    )

    @Test
    fun `all keeps every episode`() {
        assertEquals(groups, filterEpisodeGroups(groups, progress, EpisodeWatchFilter.All))
    }

    @Test
    fun `watched keeps only finished episodes`() {
        val result = filterEpisodeGroups(groups, progress, EpisodeWatchFilter.Watched)
        assertEquals(listOf("1"), result.map { it.episode })
    }

    @Test
    fun `unwatched keeps started and untouched episodes`() {
        val result = filterEpisodeGroups(groups, progress, EpisodeWatchFilter.Unwatched)
        assertEquals(listOf("2", "3"), result.map { it.episode })
    }

    private fun group(n: Int) = EpisodeGroup(
        episode = n.toString(),
        videos = persistentListOf(
            AnimeVideo(
                id = n,
                episode = n.toString(),
                dubbing = "Dub",
                player = "Kodik",
                playerId = 1,
                iframeUrl = "https://player/$n",
                durationSeconds = 1_400,
            ),
        ),
    )

    private fun entry(videoId: Int, episode: String, positionMs: Long, durationMs: Long) = AnimeWatchProgress(
        animeId = 1,
        episode = episode,
        videoId = videoId,
        episodeUrl = "https://player/$videoId",
        positionMs = positionMs,
        durationMs = durationMs,
        updatedAt = 1L,
    )
}
