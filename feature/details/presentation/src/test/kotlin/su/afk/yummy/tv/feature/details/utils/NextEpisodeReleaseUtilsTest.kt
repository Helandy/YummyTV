package su.afk.yummy.tv.feature.details.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeEpisodes
import java.time.ZoneId
import java.util.Locale

class NextEpisodeReleaseUtilsTest {

    @Test
    fun `next episode follows the aired count`() {
        assertEquals(8, episodes(aired = 7, count = 12).nextEpisodeNumber())
    }

    @Test
    fun `next episode is known without the total count`() {
        assertEquals(1, episodes(aired = 0, count = null).nextEpisodeNumber())
        assertEquals(5, episodes(aired = 4, count = 0).nextEpisodeNumber())
    }

    @Test
    fun `no next episode when everything aired or aired is unknown`() {
        assertNull(episodes(aired = 12, count = 12).nextEpisodeNumber())
        assertNull(episodes(aired = null, count = 12).nextEpisodeNumber())
    }

    @Test
    fun `near release is formatted as weekday and time`() {
        // 2026-10-16T15:00:00Z — пятница, 18:00 по Москве; до выхода меньше суток.
        val release = 1_792_162_800L
        val formatted = release.formatReleaseWeekdayTime(
            nowEpochSeconds = release - 3_600L,
            locale = Locale.forLanguageTag("ru"),
            zone = ZoneId.of("Europe/Moscow"),
        )

        assertEquals("пт, 18:00", formatted)
    }

    @Test
    fun `far release is formatted as date and time`() {
        // 2026-10-16T15:00:00Z, 96 дней до выхода — день недели без даты ввёл бы в заблуждение.
        val release = 1_792_162_800L
        val formatted = release.formatReleaseWeekdayTime(
            nowEpochSeconds = release - 96 * 86_400L,
            locale = Locale.forLanguageTag("ru"),
            zone = ZoneId.of("Europe/Moscow"),
        )

        assertEquals("16 окт, 18:00", formatted)
    }

    private fun episodes(aired: Int?, count: Int?) = AnimeEpisodes(
        count = count,
        aired = aired,
        nextDateEpochSeconds = null,
        prevDateEpochSeconds = null,
    )
}
