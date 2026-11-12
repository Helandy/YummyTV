package su.afk.yummy.tv.domain.home.usecase

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.home.repository.HomeFeedRepository

class ObserveWatchedEpisodesUseCaseTest : BaseUnitTest() {

    private val repository: HomeFeedRepository = mockk()
    private var local: Map<Int, Set<Int>> = emptyMap()
    private val server = mutableListOf<AnimeWatchProgress>()

    @Before
    fun setUp() {
        every { repository.observeWatchedEpisodes() } answers { flowOf(local) }
        every { repository.observeServerWatchProgress() } answers { flowOf(server.toList()) }
    }

    @Test
    fun `server watched episode counts as watched`() = runTest {
        server += progress(animeId = 1, episode = "6", positionMs = DURATION_MS)

        assertEquals(mapOf(1 to setOf(6)), createUseCase()().first())
    }

    @Test
    fun `partially watched server episode is ignored`() = runTest {
        server += progress(animeId = 1, episode = "6", positionMs = 5 * 60_000L)

        assertEquals(emptyMap<Int, Set<Int>>(), createUseCase()().first())
    }

    @Test
    fun `fractional special is not treated as a whole episode`() = runTest {
        server += progress(animeId = 1, episode = "7.5", positionMs = DURATION_MS)

        assertEquals(emptyMap<Int, Set<Int>>(), createUseCase()().first())
    }

    @Test
    fun `leading zero episode is normalized`() = runTest {
        server += progress(animeId = 1, episode = "06", positionMs = DURATION_MS)

        assertEquals(mapOf(1 to setOf(6)), createUseCase()().first())
    }

    @Test
    fun `local and server episodes are merged per title`() = runTest {
        local = mapOf(1 to setOf(4, 5), 2 to setOf(1))
        server += progress(animeId = 1, episode = "6", positionMs = DURATION_MS)
        server += progress(animeId = 3, episode = "2", positionMs = DURATION_MS)

        assertEquals(
            mapOf(1 to setOf(4, 5, 6), 2 to setOf(1), 3 to setOf(2)),
            createUseCase()().first(),
        )
    }

    private fun createUseCase() = ObserveWatchedEpisodesUseCase(repository)

    private fun progress(animeId: Int, episode: String, positionMs: Long) = AnimeWatchProgress(
        animeId = animeId,
        episode = episode,
        videoId = 100,
        episodeUrl = "https://example.com/$animeId/$episode",
        positionMs = positionMs,
        durationMs = DURATION_MS,
        updatedAt = 1_000L,
    )

    private companion object {
        const val DURATION_MS = 24 * 60_000L
    }
}
