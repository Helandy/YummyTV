package su.afk.yummy.tv.core.utils.formatting

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import java.util.Locale
import java.util.TimeZone

/**
 * Зона и локаль подменяются целиком: форматтер читает [Locale.getDefault] и зону по умолчанию,
 * иначе результат зависел бы от машины, на которой гоняются тесты.
 */
class FeedDateFormattersTest : BaseUnitTest() {

    private val defaultLocale: Locale = Locale.getDefault()
    private val defaultTimeZone: TimeZone = TimeZone.getDefault()

    @Before
    fun setUp() {
        Locale.setDefault(Locale.US)
        // Зона западнее Гринвича: именно на ней полночь UTC съезжала на предыдущий день.
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
    }

    @After
    fun tearDown() {
        Locale.setDefault(defaultLocale)
        TimeZone.setDefault(defaultTimeZone)
    }

    @Test
    fun `date-only air date keeps its calendar day west of Greenwich`() {
        assertEquals("6 Oct", UTC_MIDNIGHT.formatAirDate())
    }

    @Test
    fun `air date with a real time is shown in the device zone`() {
        assertEquals("6 Oct, 13:30", (UTC_MIDNIGHT + 17 * HOUR + 30 * MINUTE).formatAirDate())
    }

    @Test
    fun `a second past midnight counts as a known time`() {
        // Не ровная полночь UTC — значит время настоящее, показываем его в зоне устройства.
        assertEquals("5 Oct, 20:00", (UTC_MIDNIGHT + 1).formatAirDate())
    }

    private companion object {
        const val MINUTE = 60L
        const val HOUR = 60L * 60
        const val UTC_MIDNIGHT = 1_791_244_800L
    }
}
