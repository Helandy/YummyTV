package su.afk.yummy.tv.core.model.anime

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.model.settings.WatchedThresholds

class WatchedProgressRuleTest {

    @After
    fun resetRule() {
        WatchedEpisodeRule.update(WatchedThresholds())
    }

    @Test
    fun `short episode uses one minute by default`() {
        val duration = 8.min
        assertFalse(isWatchedProgress(duration - 90.sec, duration))
        assertTrue(isWatchedProgress(duration - 60.sec, duration))
    }

    @Test
    fun `regular episode keeps five minutes by default`() {
        val duration = 24.min
        assertFalse(isWatchedProgress(duration - 6.min, duration))
        assertTrue(isWatchedProgress(duration - 5.min, duration))
    }

    @Test
    fun `long episode uses ten minutes by default`() {
        val duration = 90.min
        assertFalse(isWatchedProgress(duration - 11.min, duration))
        assertTrue(isWatchedProgress(duration - 10.min, duration))
    }

    @Test
    fun `bucket boundaries are inclusive`() {
        assertFalse(isWatchedProgress(10.min - 2.min, 10.min))
        assertTrue(isWatchedProgress(10.min - 1.min, 10.min))
        assertFalse(isWatchedProgress(60.min - 6.min, 60.min))
        assertTrue(isWatchedProgress(60.min - 5.min, 60.min))
        assertTrue(isWatchedProgress(61.min - 10.min, 61.min))
    }

    @Test
    fun `very short video falls back to ninety percent`() {
        WatchedEpisodeRule.update(WatchedThresholds(shortMinutes = 5))
        val duration = 4.min
        assertFalse(isWatchedProgress((duration * 0.85).toLong(), duration))
        assertTrue(isWatchedProgress((duration * 0.9).toLong(), duration))
    }

    @Test
    fun `updated thresholds are applied`() {
        WatchedEpisodeRule.update(WatchedThresholds(mediumMinutes = 2))
        val duration = 24.min
        assertFalse(isWatchedProgress(duration - 3.min, duration))
        assertTrue(isWatchedProgress(duration - 2.min, duration))
    }

    @Test
    fun `out of range thresholds are coerced`() {
        WatchedEpisodeRule.update(WatchedThresholds(0, 100, 100))
        assertEquals(WatchedThresholds(1, 15, 30), WatchedEpisodeRule.thresholds)
    }

    private val Int.min: Long get() = this * 60_000L
    private val Int.sec: Long get() = this * 1_000L
}
