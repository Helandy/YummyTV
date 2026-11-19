package su.afk.yummy.tv.feature.details.episodes.utils

import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.feature.details.episodes.EpisodesState.EpisodeGroup
import su.afk.yummy.tv.feature.details.episodes.model.EpisodeRange

class EpisodeRangeUtilsTest {

    @Test
    fun `returns empty list when episode count does not exceed chunk size`() {
        assertTrue(buildEpisodeRanges(emptyList()).isEmpty())
        assertTrue(buildEpisodeRanges(groups(1..12)).isEmpty())
        assertTrue(buildEpisodeRanges(groups(1..24)).isEmpty())
        assertTrue(buildEpisodeRanges(groups(1..EPISODE_RANGE_CHUNK_SIZE)).isEmpty())
    }

    @Test
    fun `splits long episode list into 50-episode ranges with exact tail`() {
        val ranges = buildEpisodeRanges(groups(1..120))

        assertEquals(
            listOf(
                EpisodeRange(startIndex = 0, endIndex = 49, firstEpisode = "1", lastEpisode = "50"),
                EpisodeRange(startIndex = 50, endIndex = 99, firstEpisode = "51", lastEpisode = "100"),
                EpisodeRange(startIndex = 100, endIndex = 119, firstEpisode = "101", lastEpisode = "120"),
            ),
            ranges,
        )
    }

    @Test
    fun `handles single-episode trailing range and fractional episode numbers`() {
        val list = listOf(
            group("00"),
            group("01"),
            group("12.5"),
            group("13"),
            group("51"),
        )

        val ranges = buildEpisodeRanges(list, chunkSize = 2)

        assertEquals(
            listOf(
                EpisodeRange(startIndex = 0, endIndex = 1, firstEpisode = "0", lastEpisode = "1"),
                EpisodeRange(startIndex = 2, endIndex = 3, firstEpisode = "12.5", lastEpisode = "13"),
                EpisodeRange(startIndex = 4, endIndex = 4, firstEpisode = "51", lastEpisode = "51"),
            ),
            ranges,
        )
    }

    @Test
    fun `finds active range index and clamps out-of-bounds indices`() {
        val ranges = buildEpisodeRanges(groups(1..120))

        assertEquals(-1, emptyList<EpisodeRange>().rangeIndexForEpisodeIndex(0))
        assertEquals(0, ranges.rangeIndexForEpisodeIndex(-5))
        assertEquals(0, ranges.rangeIndexForEpisodeIndex(0))
        assertEquals(0, ranges.rangeIndexForEpisodeIndex(49))
        assertEquals(1, ranges.rangeIndexForEpisodeIndex(50))
        assertEquals(1, ranges.rangeIndexForEpisodeIndex(99))
        assertEquals(2, ranges.rangeIndexForEpisodeIndex(100))
        assertEquals(2, ranges.rangeIndexForEpisodeIndex(500))
    }

    private fun groups(range: IntRange): List<EpisodeGroup> =
        range.map { n -> group(n.toString()) }

    private fun group(episode: String) = EpisodeGroup(
        episode = episode,
        videos = persistentListOf(
            AnimeVideo(
                id = episode.hashCode(),
                episode = episode,
                dubbing = "Dub",
                player = "Kodik",
                playerId = 1,
                iframeUrl = "https://player/$episode",
                durationSeconds = 1_400,
            ),
        ),
    )
}
