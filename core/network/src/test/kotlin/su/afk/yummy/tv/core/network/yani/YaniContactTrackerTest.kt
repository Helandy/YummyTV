package su.afk.yummy.tv.core.network.yani

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class YaniContactTrackerTest : BaseUnitTest() {

    private val tracker = YaniContactTracker()

    @Test
    fun `server errors are not contact, so purge keeps waiting`() = runTest {
        tracker.onResponse(502)
        tracker.onResponse(503)

        assertFalse(tracker.hasContact)
        assertEquals(null, withTimeoutOrNull(10) { tracker.awaitContact() })
    }

    @Test
    fun `any answer below 500 means yani is alive`() = runTest {
        tracker.onResponse(404)

        assertTrue(tracker.hasContact)
        tracker.awaitContact()
    }
}
