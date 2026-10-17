package su.afk.yummy.tv.core.utils.coroutines

import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class TrustedAppClockTest : BaseUnitTest() {

    @Test
    fun `falls back to system time while trusted client is not ready`() {
        val clock = TrustedAppClock(mockk(relaxed = true))

        val before = System.currentTimeMillis()
        val now = clock.nowMillis()
        val after = System.currentTimeMillis()

        assertTrue(now in before..after)
    }
}
