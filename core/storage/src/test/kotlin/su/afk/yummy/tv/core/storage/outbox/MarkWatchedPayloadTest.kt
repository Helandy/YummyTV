package su.afk.yummy.tv.core.storage.outbox

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class MarkWatchedPayloadTest : BaseUnitTest() {

    @Test
    fun `секунды кодируются диапазонами и читаются обратно`() {
        val payload = MarkWatchedPayload(7, 90, 1_400, times = listOf(0, 1, 2, 3, 10, 12, 13, 600))

        assertEquals("7|90|1400|0-3,10,12-13,600", payload.encode())
        assertEquals(payload, MarkWatchedPayload.decode(payload.encode()))
    }

    @Test
    fun `запись старого формата без секунд читается`() {
        val decoded = MarkWatchedPayload.decode("7|90|1400")

        assertEquals(MarkWatchedPayload(7, 90, 1_400), decoded)
    }

    @Test
    fun `пустой список секунд оставляет четвёртое поле пустым`() {
        val payload = MarkWatchedPayload(1, 2, 3)

        assertEquals("1|2|3|", payload.encode())
        assertEquals(payload, MarkWatchedPayload.decode(payload.encode()))
    }

    @Test
    fun `повторы и порядок секунд не влияют на кодирование`() {
        assertEquals("1-3", listOf(3, 1, 2, 2).toSecondRanges())
    }
}
