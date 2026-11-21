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
    fun `groups zero, fractional and leading-zero numbers by episode number`() {
        val list = listOf(
            group("00"),
            group("01"),
            group("4"),
            group("12.5"),
            group("13"),
            group("51"),
        )

        val ranges = buildEpisodeRanges(list, chunkSize = 5)

        assertEquals(
            listOf(
                EpisodeRange(startIndex = 0, endIndex = 2, firstEpisode = "0", lastEpisode = "4"),
                EpisodeRange(startIndex = 3, endIndex = 4, firstEpisode = "12.5", lastEpisode = "13"),
                EpisodeRange(startIndex = 5, endIndex = 5, firstEpisode = "51", lastEpisode = "51"),
            ),
            ranges,
        )
    }

    @Test
    fun `double episodes do not shift range boundaries`() {
        // Как у «Наруто: Ураганные хроники»: часть озвучек публикует сдвоенные серии как отдельные записи.
        val list = buildList {
            for (n in 1..100) {
                add(group(n.toString()))
                if (n == 57) add(group("57-58"))
                if (n == 100) add(group("100-101"))
            }
        }

        val ranges = buildEpisodeRanges(list)

        assertEquals(
            listOf(
                EpisodeRange(startIndex = 0, endIndex = 49, firstEpisode = "1", lastEpisode = "50"),
                EpisodeRange(startIndex = 50, endIndex = 101, firstEpisode = "51", lastEpisode = "100"),
            ),
            ranges,
        )
    }

    @Test
    fun `gaps in numbering produce ranges by number`() {
        val list = groups(1..30) + groups(101..140)

        val ranges = buildEpisodeRanges(list)

        assertEquals(
            listOf(
                EpisodeRange(startIndex = 0, endIndex = 29, firstEpisode = "1", lastEpisode = "30"),
                EpisodeRange(startIndex = 30, endIndex = 69, firstEpisode = "101", lastEpisode = "140"),
            ),
            ranges,
        )
    }

    @Test
    fun `episodes without a number go to a trailing range`() {
        val list = groups(1..4) + group("special")

        val ranges = buildEpisodeRanges(list, chunkSize = 3)

        assertEquals(
            listOf(
                EpisodeRange(startIndex = 0, endIndex = 2, firstEpisode = "1", lastEpisode = "3"),
                EpisodeRange(startIndex = 3, endIndex = 3, firstEpisode = "4", lastEpisode = "4"),
                EpisodeRange(startIndex = 4, endIndex = 4, firstEpisode = "special", lastEpisode = "special"),
            ),
            ranges,
        )
    }

    @Test
    fun `returns empty list when all episodes fall into a single number block`() {
        val list = buildList {
            for (n in 1..EPISODE_RANGE_CHUNK_SIZE) {
                add(group(n.toString()))
                if (n == 10) add(group("10-11"))
            }
        }

        assertEquals(EPISODE_RANGE_CHUNK_SIZE + 1, list.size)
        assertTrue(buildEpisodeRanges(list).isEmpty())
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

    @Test
    fun `finds range index on boundaries and single-episode uneven tail with chunkSize 2`() {
        val ranges = buildEpisodeRanges(groups(1..5), chunkSize = 2)

        assertEquals(0, ranges.rangeIndexForEpisodeIndex(-1))
        assertEquals(0, ranges.rangeIndexForEpisodeIndex(0))
        assertEquals(0, ranges.rangeIndexForEpisodeIndex(1))
        assertEquals(1, ranges.rangeIndexForEpisodeIndex(2))
        assertEquals(1, ranges.rangeIndexForEpisodeIndex(3))
        assertEquals(2, ranges.rangeIndexForEpisodeIndex(4))
        assertEquals(2, ranges.rangeIndexForEpisodeIndex(10))
    }

    @Test
    fun `selects trailing single-episode range when list is scrolled to bottom`() {
        // 101 серия: диапазоны [0..49], [50..99], [100..100]; у нижней границы firstVisibleItemIndex ~ 93.
        val ranges = buildEpisodeRanges(groups(1..101))

        assertEquals(
            2,
            ranges.resolveVisibleRangeIndex(
                firstVisibleItemIndex = 93,
                canScrollForward = false,
                canScrollBackward = true,
            ),
        )
        assertEquals(
            1,
            ranges.resolveVisibleRangeIndex(
                firstVisibleItemIndex = 92,
                canScrollForward = true,
                canScrollBackward = true,
            ),
        )
    }

    @Test
    fun `honors pinned range until cleared and handles non-scrollable viewport`() {
        val ranges = buildEpisodeRanges(groups(1..101))

        assertEquals(-1, emptyList<EpisodeRange>().resolveVisibleRangeIndex(0, canScrollForward = false))

        // На высоком экране прыжок на 51–100 тоже может упереться в конец списка — должен гореть нажатый чип.
        assertEquals(
            1,
            ranges.resolveVisibleRangeIndex(
                firstVisibleItemIndex = 49,
                canScrollForward = false,
                canScrollBackward = true,
                pinnedRangeIndex = 1,
            ),
        )
        // Невалидный pinnedRangeIndex игнорируется и откатывается к положению скролла.
        assertEquals(
            2,
            ranges.resolveVisibleRangeIndex(
                firstVisibleItemIndex = 93,
                canScrollForward = false,
                canScrollBackward = true,
                pinnedRangeIndex = 99,
            ),
        )
        // Если весь список поместился без прокрутки, по умолчанию активен первый диапазон.
        assertEquals(
            0,
            ranges.resolveVisibleRangeIndex(
                firstVisibleItemIndex = 0,
                canScrollForward = false,
                canScrollBackward = false,
            ),
        )
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
