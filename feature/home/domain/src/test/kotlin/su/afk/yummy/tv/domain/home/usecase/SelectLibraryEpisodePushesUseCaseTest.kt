package su.afk.yummy.tv.domain.home.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction
import su.afk.yummy.tv.domain.home.utils.LIBRARY_EPISODE_BASELINE_KEY
import su.afk.yummy.tv.domain.home.utils.libraryEpisodeKey

class SelectLibraryEpisodePushesUseCaseTest : BaseUnitTest() {

    private val useCase = SelectLibraryEpisodePushesUseCase()

    @Test
    fun `first run only records the baseline`() {
        val result = select(
            recentlyAired = listOf(item(1, episode = 5, airedAt = NOW - HOUR)),
            knownKeys = emptySet(),
        )

        assertTrue(result.pushes.isEmpty())
        assertEquals(setOf(LIBRARY_EPISODE_BASELINE_KEY, libraryEpisodeKey(1, 5)), result.knownKeys)
    }

    @Test
    fun `baseline is recorded even when nothing is fresh`() {
        val result = select(recentlyAired = emptyList(), knownKeys = emptySet())

        assertEquals(setOf(LIBRARY_EPISODE_BASELINE_KEY), result.knownKeys)
    }

    @Test
    fun `an unwatched fresh episode of a library title is pushed`() {
        val result = select(recentlyAired = listOf(item(1, episode = 5, airedAt = NOW - HOUR)))

        assertEquals(listOf(1), result.pushes.map { it.animeId })
        assertEquals(5, result.pushes.single().episodeNumber)
        assertTrue(libraryEpisodeKey(1, 5) in result.knownKeys)
    }

    @Test
    fun `an already known episode is not pushed again`() {
        val result = select(
            recentlyAired = listOf(item(1, episode = 5, airedAt = NOW - HOUR)),
            knownKeys = setOf(LIBRARY_EPISODE_BASELINE_KEY, libraryEpisodeKey(1, 5)),
        )

        assertTrue(result.pushes.isEmpty())
    }

    @Test
    fun `a watched episode is not pushed but is remembered`() {
        val result = select(
            recentlyAired = listOf(item(1, episode = 5, airedAt = NOW - HOUR)),
            watched = mapOf(1 to setOf(5)),
        )

        assertTrue(result.pushes.isEmpty())
        assertTrue(libraryEpisodeKey(1, 5) in result.knownKeys)
    }

    @Test
    fun `a title outside the library is ignored`() {
        val result = select(
            recentlyAired = listOf(item(2, episode = 5, airedAt = NOW - HOUR)),
            libraryIds = setOf(1),
        )

        assertTrue(result.pushes.isEmpty())
        assertEquals(setOf(LIBRARY_EPISODE_BASELINE_KEY), result.knownKeys)
    }

    @Test
    fun `a stale episode is silently marked known`() {
        val result = select(recentlyAired = listOf(item(1, episode = 5, airedAt = NOW - 3 * DAY)))

        assertTrue(result.pushes.isEmpty())
        assertTrue(libraryEpisodeKey(1, 5) in result.knownKeys)
    }

    @Test
    fun `a title pushed within a day is suppressed`() {
        val result = select(
            recentlyAired = listOf(item(1, episode = 5, airedAt = NOW - HOUR)),
            recentPushes = mapOf(1 to NOW - 2 * HOUR),
        )

        assertTrue(result.pushes.isEmpty())
        assertTrue(libraryEpisodeKey(1, 5) in result.knownKeys)
    }

    @Test
    fun `an old push of the same title does not suppress a new episode`() {
        val result = select(
            recentlyAired = listOf(item(1, episode = 6, airedAt = NOW - HOUR)),
            recentPushes = mapOf(1 to NOW - 7 * DAY),
        )

        assertEquals(listOf(6), result.pushes.map { it.episodeNumber })
    }

    @Test
    fun `an item without episode data is skipped`() {
        val result = select(recentlyAired = listOf(item(1, episode = null, airedAt = null)))

        assertTrue(result.pushes.isEmpty())
    }

    private fun select(
        recentlyAired: List<HomeFeedItem>,
        libraryIds: Set<Int> = setOf(1),
        watched: Map<Int, Set<Int>> = emptyMap(),
        knownKeys: Set<String> = setOf(LIBRARY_EPISODE_BASELINE_KEY),
        recentPushes: Map<Int, Long> = emptyMap(),
    ) = useCase(
        recentlyAired = recentlyAired,
        libraryAnimeIds = libraryIds,
        watchedEpisodes = watched,
        knownKeys = knownKeys,
        recentPushes = recentPushes,
        nowSeconds = NOW,
    )

    private fun item(id: Int, episode: Int?, airedAt: Long?) = HomeFeedItem(
        id = id,
        title = "Title $id",
        description = "",
        poster = null,
        rating = null,
        year = null,
        action = HomeFeedItemAction.OpenSeries(id),
        episodeNumber = episode,
        airedAtSeconds = airedAt,
    )

    private companion object {
        const val NOW = 1_800_000_000L
        const val HOUR = 3_600L
        const val DAY = 24 * HOUR
    }
}
