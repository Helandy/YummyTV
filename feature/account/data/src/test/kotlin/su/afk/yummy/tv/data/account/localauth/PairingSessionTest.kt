package su.afk.yummy.tv.data.account.localauth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Политика PIN отвечает за то, что перебор кода на ТВ конечен: после лимита неудач и по истечении
 * срока сопряжение обязано отказывать, иначе шестизначный код можно было бы подобрать.
 */
class PairingSessionTest {

    @Test
    fun `fresh session accepts requests`() {
        assertNull(session().rejection())
    }

    @Test
    fun `attempts run out and the session is rejected`() {
        val session = session(maxAttempts = 3)

        assertEquals(2, session.registerFailure())
        assertEquals(1, session.registerFailure())
        assertNull(session.rejection())

        assertEquals(0, session.registerFailure())
        assertEquals(PairingSession.Rejection.EXHAUSTED, session.rejection())
    }

    @Test
    fun `attempts counter never goes below zero`() {
        val session = session(maxAttempts = 1)

        session.registerFailure()
        session.registerFailure()

        assertEquals(0, session.attemptsLeft)
    }

    @Test
    fun `session is rejected once the pin has expired`() {
        var now = 0L
        val session = session(ttlMs = 1_000L) { now }

        now = 1_000L
        assertNull(session.rejection())

        now = 1_001L
        assertEquals(PairingSession.Rejection.EXPIRED, session.rejection())
    }

    @Test
    fun `expiry wins over remaining attempts`() {
        var now = 0L
        val session = session(ttlMs = 10L, maxAttempts = 5) { now }

        now = 100L
        assertEquals(PairingSession.Rejection.EXPIRED, session.rejection())
    }

    private fun session(
        ttlMs: Long = 60_000L,
        maxAttempts: Int = 5,
        now: () -> Long = { 0L },
    ) = PairingSession(pin = "123456", ttlMs = ttlMs, maxAttempts = maxAttempts, now = now)
}
