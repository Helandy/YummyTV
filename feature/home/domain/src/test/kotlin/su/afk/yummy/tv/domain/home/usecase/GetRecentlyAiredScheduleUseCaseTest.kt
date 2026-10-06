package su.afk.yummy.tv.domain.home.usecase

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction
import su.afk.yummy.tv.domain.schedule.model.AnimeScheduleDay
import su.afk.yummy.tv.domain.schedule.model.AnimeScheduleItem
import su.afk.yummy.tv.domain.schedule.repository.AnimeScheduleRepository
import su.afk.yummy.tv.domain.schedule.usecase.GetAnimeScheduleUseCase

class GetRecentlyAiredScheduleUseCaseTest : BaseUnitTest() {

    private val repository: AnimeScheduleRepository = mockk()
    private val schedule = mutableListOf<AnimeScheduleItem>()

    @Before
    fun setUp() {
        coEvery { repository.getSchedule() } answers {
            listOf(AnimeScheduleDay(title = "Monday", items = schedule.toList()))
        }
    }

    @Test
    fun `episodes inside the window are returned and the freshest comes first`() = runTest {
        schedule += scheduleItem(animeId = 1, previousDate = NOW - 10 * DAY)
        schedule += scheduleItem(animeId = 2, previousDate = NOW - DAY)

        val result = createUseCase()(NOW)

        assertEquals(listOf(2, 1), result.map { it.id })
    }

    @Test
    fun `episodes older than the window are dropped`() = runTest {
        schedule += scheduleItem(animeId = 1, previousDate = NOW - 15 * DAY)

        assertEquals(emptyList<Int>(), createUseCase()(NOW).map { it.id })
    }

    @Test
    fun `the window boundary is inclusive`() = runTest {
        schedule += scheduleItem(animeId = 1, previousDate = NOW - 14 * DAY)

        assertEquals(listOf(1), createUseCase()(NOW).map { it.id })
    }

    @Test
    fun `announcements never reach the result`() = runTest {
        schedule += scheduleItem(animeId = 1, airedEpisodes = null, previousDate = NOW - DAY)

        assertEquals(emptyList<Int>(), createUseCase()(NOW).map { it.id })
    }

    @Test
    fun `a title appears once even with several schedule entries`() = runTest {
        schedule += scheduleItem(animeId = 1, previousDate = NOW - 2 * DAY)
        schedule += scheduleItem(animeId = 1, previousDate = NOW - DAY)

        val result = createUseCase()(NOW)

        assertEquals(listOf(1), result.map { it.id })
        assertEquals(NOW - DAY, result.single().airedAtSeconds)
    }

    @Test
    fun `episode number and air date are carried over`() = runTest {
        schedule += scheduleItem(animeId = 7, airedEpisodes = 3, previousDate = NOW - DAY)

        val item = createUseCase()(NOW).single()

        assertEquals(3, item.episodeNumber)
        assertEquals(NOW - DAY, item.airedAtSeconds)
        assertEquals(HomeFeedItemAction.OpenSeries(7), item.action)
    }

    private fun createUseCase() = GetRecentlyAiredScheduleUseCase(GetAnimeScheduleUseCase(repository))

    private fun scheduleItem(
        animeId: Int,
        airedEpisodes: Int? = 2,
        previousDate: Long? = null,
    ) = AnimeScheduleItem(
        animeId = animeId,
        title = "Title $animeId",
        posterUrl = null,
        nextDateEpochSeconds = null,
        airedEpisodes = airedEpisodes,
        totalEpisodes = 12,
        previousDateEpochSeconds = previousDate,
    )

    private companion object {
        const val DAY = 24L * 60 * 60
        const val NOW = 1_791_000_000L
    }
}
