package su.afk.yummy.tv.data.home.repository

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.storage.home.HomeFeedStorage
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStorage
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.data.home.network.YaniHomeApi

/** Только [YaniHomeFeedRepository.observeWatchedEpisodes]: остальные зависимости не участвуют. */
class YaniHomeFeedRepositoryWatchedEpisodesTest : BaseUnitTest() {

    private val api: YaniHomeApi = mockk()
    private val homeFeedStore: HomeFeedStorage = mockk()
    private val stringProvider: StringProvider = mockk()
    private val settingsStore: YaniAccountSettingsStore = mockk()
    private val watchProgressStore: WatchProgressStorage = mockk()
    private val analyticsTracker: AnalyticsTracker = mockk(relaxed = true)
    private val watched = mutableListOf<WatchProgressEntry>()

    @Before
    fun setUp() {
        every { watchProgressStore.observeWatchedProgress() } answers { flowOf(watched.toList()) }
    }

    @Test
    fun `episodes are grouped by title`() = runTest {
        watched += progress(animeId = 1, episode = "1")
        watched += progress(animeId = 1, episode = "2")
        watched += progress(animeId = 2, episode = "5")

        assertEquals(mapOf(1 to setOf(1, 2), 2 to setOf(5)), createRepository().observeWatchedEpisodes().first())
    }

    @Test
    fun `leading zeros collapse onto the same episode`() = runTest {
        watched += progress(animeId = 1, episode = "01")
        watched += progress(animeId = 1, episode = "1")

        assertEquals(mapOf(1 to setOf(1)), createRepository().observeWatchedEpisodes().first())
    }

    @Test
    fun `a special does not count as the neighbouring episode`() = runTest {
        // «7.5» — отдельный спецвыпуск, седьмую серию он просмотренной не делает.
        watched += progress(animeId = 1, episode = "7.5")

        assertEquals(mapOf(1 to emptySet<Int>()), createRepository().observeWatchedEpisodes().first())
    }

    @Test
    fun `non-numeric episodes are skipped`() = runTest {
        watched += progress(animeId = 1, episode = "OVA")
        watched += progress(animeId = 1, episode = "4")

        assertEquals(mapOf(1 to setOf(4)), createRepository().observeWatchedEpisodes().first())
    }

    private fun createRepository() = YaniHomeFeedRepository(
        api = api,
        homeFeedStore = homeFeedStore,
        stringProvider = stringProvider,
        settingsStore = settingsStore,
        watchProgressStore = watchProgressStore,
        analyticsTracker = analyticsTracker,
    )

    private fun progress(animeId: Int, episode: String) = WatchProgressEntry(
        animeId = animeId,
        episode = episode,
        episodeUrl = "",
        positionMs = 0L,
        durationMs = 0L,
    )
}
