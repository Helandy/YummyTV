package su.afk.yummy.tv.feature.player.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class QualityAdaptationPolicyTest : BaseUnitTest() {

    private val policy = QualityAdaptationPolicy()
    private val levels = listOf("360p", "480p", "720p", "1080p")

    private fun decide(current: String, now: Long, buffered: Long = 30_000L) =
        policy.evaluate(current, levels, buffered, now)

    /** Сеть с заданной полосой: оценка приходит один раз, дальше политика опрашивается раз в 5 с. */
    private fun network(
        bps: Long,
        current: String,
        from: Long,
        to: Long,
        target: QualityAdaptationPolicy = policy,
        available: List<String> = levels,
    ) {
        target.onBandwidth(bps)
        var t = from
        while (t <= to) {
            target.evaluate(current, available, 30_000L, t)
            t += 5_000L
        }
    }

    private fun goodNetwork(current: String, from: Long, to: Long) =
        network(50_000_000L, current, from, to)

    @Test
    fun `healthy playback keeps the quality`() {
        assertEquals(QualityAdaptationDecision.Keep, decide("1080p", now = 1_000L))
    }

    @Test
    fun `two stalls in a minute step down one level`() {
        policy.onStallStarted(10_000L)
        policy.onStallStarted(30_000L)

        assertEquals(
            QualityAdaptationDecision.Switch("720p", isDowngrade = true),
            decide("1080p", now = 31_000L),
        )
    }

    @Test
    fun `one long stall steps down without waiting for a second`() {
        policy.onStallStarted(10_000L)

        assertEquals(QualityAdaptationDecision.Keep, decide("1080p", now = 15_000L))
        assertEquals(
            QualityAdaptationDecision.Switch("720p", isDowngrade = true),
            decide("1080p", now = 19_000L),
        )
    }

    @Test
    fun `slow startup steps down only after the startup timeout`() {
        policy.onStallStarted(0L, startup = true)

        assertEquals(QualityAdaptationDecision.Keep, decide("1080p", now = 14_000L))
        assertEquals(
            QualityAdaptationDecision.Switch("720p", isDowngrade = true),
            decide("1080p", now = 15_000L),
        )
    }

    @Test
    fun `startup buffering is not counted as a repeated stall`() {
        policy.onStallStarted(0L, startup = true)
        policy.onStallEnded()
        policy.onStallStarted(20_000L)
        policy.onStallEnded()

        assertEquals(QualityAdaptationDecision.Keep, decide("1080p", now = 25_000L))
    }

    @Test
    fun `new stream after a switch gets time to load before the next downgrade`() {
        policy.onStallStarted(0L)
        assertEquals(
            QualityAdaptationDecision.Switch("720p", isDowngrade = true),
            decide("1080p", now = 10_000L),
        )
        policy.onQualityChanged(10_000L)

        assertEquals(QualityAdaptationDecision.Keep, decide("720p", now = 30_000L))
        assertEquals(
            QualityAdaptationDecision.Switch("480p", isDowngrade = true),
            decide("720p", now = 35_000L),
        )
    }

    @Test
    fun `a short stall that ended does not downgrade`() {
        policy.onStallStarted(10_000L)
        policy.onStallEnded()

        assertEquals(QualityAdaptationDecision.Keep, decide("1080p", now = 30_000L))
    }

    @Test
    fun `stalls spread over more than a minute do not downgrade`() {
        policy.onStallStarted(0L)
        policy.onStallStarted(70_000L)

        assertEquals(QualityAdaptationDecision.Keep, decide("1080p", now = 71_000L))
    }

    @Test
    fun `dropped frames step down and stay down`() {
        policy.onDroppedFrames(40, nowMs = 5_000L)

        val down = decide("1080p", now = 6_000L)
        assertEquals(QualityAdaptationDecision.Switch("720p", isDowngrade = true), down)

        policy.onQualityChanged(6_000L)
        goodNetwork("720p", from = 7_000L, to = 200_000L)

        assertEquals(QualityAdaptationDecision.Keep, decide("720p", now = 200_000L))
    }

    @Test
    fun `lowest level has nowhere to go`() {
        policy.onStallStarted(1_000L)
        policy.onStallStarted(2_000L)

        assertEquals(QualityAdaptationDecision.Keep, decide("360p", now = 3_000L))
    }

    @Test
    fun `recovered network goes back up after the stable period`() {
        policy.onStallStarted(1_000L)
        policy.onStallStarted(2_000L)
        assertEquals(
            QualityAdaptationDecision.Switch("720p", isDowngrade = true),
            decide("1080p", now = 3_000L),
        )
        policy.onQualityChanged(3_000L)
        policy.onStallEnded()

        // 1080p забанен на 2 минуты после падения по сети.
        goodNetwork("720p", from = 4_000L, to = 100_000L)
        assertEquals(QualityAdaptationDecision.Keep, decide("720p", now = 100_000L))

        goodNetwork("720p", from = 301_000L, to = 340_000L)
        assertEquals(
            QualityAdaptationDecision.Switch("1080p", isDowngrade = false),
            decide("720p", now = 340_000L),
        )
    }

    @Test
    fun `upgrade waits for the minimum interval since the last switch`() {
        policy.onQualityChanged(0L)
        goodNetwork("480p", from = 1_000L, to = 40_000L)

        assertEquals(QualityAdaptationDecision.Keep, decide("480p", now = 40_000L))

        goodNetwork("480p", from = 41_000L, to = 61_000L)
        assertEquals(
            QualityAdaptationDecision.Switch("720p", isDowngrade = false),
            decide("480p", now = 61_000L),
        )
    }

    @Test
    fun `upgrade needs a bandwidth margin and a buffer`() {
        policy.onQualityChanged(0L)
        // 720p требует ~2.5 Мбит/с × 1.5; 3 Мбит/с — недостаточно.
        network(3_000_000L, "480p", from = 1_000L, to = 100_000L)
        assertEquals(QualityAdaptationDecision.Keep, decide("480p", now = 100_000L))

        goodNetwork("480p", from = 101_000L, to = 140_000L)
        assertEquals(QualityAdaptationDecision.Keep, decide("480p", now = 140_000L, buffered = 2_000L))
    }

    @Test
    fun `never climbs above the ceiling`() {
        val capped = QualityAdaptationPolicy(ceilingHeight = 720)
        capped.onQualityChanged(0L)
        network(50_000_000L, "720p", from = 1_000L, to = 100_000L, target = capped)

        assertEquals(
            QualityAdaptationDecision.Keep,
            capped.evaluate("720p", levels, 30_000L, 100_000L),
        )
    }

    @Test
    fun `climbs to 4K only with enough bandwidth`() {
        val withUhd = listOf("720p", "1080p", "2160p")
        policy.onQualityChanged(0L)
        // 4K требует ~16 Мбит/с × 1.5; 20 Мбит/с — недостаточно.
        network(20_000_000L, "1080p", from = 1_000L, to = 100_000L, available = withUhd)
        assertEquals(
            QualityAdaptationDecision.Keep,
            policy.evaluate("1080p", withUhd, 30_000L, 100_000L),
        )

        network(50_000_000L, "1080p", from = 105_000L, to = 140_000L, available = withUhd)
        assertEquals(
            QualityAdaptationDecision.Switch("2160p", isDowngrade = false),
            policy.evaluate("1080p", withUhd, 30_000L, 140_000L),
        )
    }

    @Test
    fun `one good estimate is enough when no new ones arrive`() {
        policy.onQualityChanged(0L)
        policy.onBandwidth(50_000_000L)

        var decision: QualityAdaptationDecision = QualityAdaptationDecision.Keep
        var t = 5_000L
        while (t <= 70_000L && decision == QualityAdaptationDecision.Keep) {
            decision = decide("720p", now = t)
            t += 5_000L
        }

        assertEquals(QualityAdaptationDecision.Switch("1080p", isDowngrade = false), decision)
    }

    @Test
    fun `a later low estimate blocks the upgrade until a better one arrives`() {
        policy.onQualityChanged(0L)
        policy.onBandwidth(50_000_000L)
        network(50_000_000L, "720p", from = 5_000L, to = 40_000L)
        policy.onBandwidth(3_000_000L)

        assertEquals(QualityAdaptationDecision.Keep, decide("720p", now = 100_000L))
        assertEquals(QualityAdaptationDecision.Keep, decide("720p", now = 200_000L))
    }

    @Test
    fun `single quality source is never switched`() {
        policy.onStallStarted(1_000L)
        policy.onStallStarted(2_000L)

        assertEquals(
            QualityAdaptationDecision.Keep,
            policy.evaluate("auto", listOf("auto"), 30_000L, 3_000L),
        )
    }
}
