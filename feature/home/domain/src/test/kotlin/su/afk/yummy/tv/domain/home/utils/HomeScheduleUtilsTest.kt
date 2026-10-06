package su.afk.yummy.tv.domain.home.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.schedule.model.AnimeScheduleItem

class HomeScheduleUtilsTest : BaseUnitTest() {

    @Test
    fun `announcement without aired episodes has no air date`() {
        // У анонсов prev_date приходит датой-заглушкой, общей для целой пачки тайтлов.
        val item = scheduleItem(airedEpisodes = null, previousDate = NOW - DAY)

        assertNull(item.lastAiredSeconds(NOW))
    }

    @Test
    fun `past previous date is the air date`() {
        val item = scheduleItem(previousDate = NOW - DAY)

        assertEquals(NOW - DAY, item.lastAiredSeconds(NOW))
    }

    @Test
    fun `past next date is used when previous date is missing`() {
        // Сервер не передвинул next_date после выхода серии — это и есть её дата.
        val item = scheduleItem(previousDate = null, nextDate = NOW - DAY)

        assertEquals(NOW - DAY, item.lastAiredSeconds(NOW))
    }

    @Test
    fun `the latest of two past dates wins`() {
        val item = scheduleItem(previousDate = NOW - 2 * DAY, nextDate = NOW - DAY)

        assertEquals(NOW - DAY, item.lastAiredSeconds(NOW))
    }

    @Test
    fun `future previous date is discarded`() {
        val item = scheduleItem(previousDate = NOW + DAY, nextDate = NOW - DAY)

        assertEquals(NOW - DAY, item.lastAiredSeconds(NOW))
    }

    @Test
    fun `nothing in the past means no air date`() {
        val item = scheduleItem(previousDate = NOW + DAY, nextDate = NOW + 2 * DAY)

        assertNull(item.lastAiredSeconds(NOW))
    }

    @Test
    fun `zero dates are ignored`() {
        val item = scheduleItem(previousDate = 0L, nextDate = 0L)

        assertNull(item.lastAiredSeconds(NOW))
    }

    private fun scheduleItem(
        airedEpisodes: Int? = 2,
        previousDate: Long? = null,
        nextDate: Long? = null,
    ) = AnimeScheduleItem(
        animeId = ANIME_ID,
        title = "Title",
        posterUrl = null,
        nextDateEpochSeconds = nextDate,
        airedEpisodes = airedEpisodes,
        totalEpisodes = 12,
        previousDateEpochSeconds = previousDate,
    )

    private companion object {
        const val ANIME_ID = 42
        const val DAY = 24L * 60 * 60
        const val NOW = 1_791_000_000L
    }
}
