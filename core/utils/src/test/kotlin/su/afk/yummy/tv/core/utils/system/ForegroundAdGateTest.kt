package su.afk.yummy.tv.core.utils.system

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class ForegroundAdGateTest : BaseUnitTest() {

    @Test
    fun `gate is open only inside the block`() = runBlocking {
        assertFalse(ForegroundAdGate.isShowing)
        ForegroundAdGate.around { assertTrue(ForegroundAdGate.isShowing) }
        assertFalse(ForegroundAdGate.isShowing)
    }

    @Test
    fun `nested blocks keep the gate open until the outer one ends`() = runBlocking {
        ForegroundAdGate.around {
            ForegroundAdGate.around { }
            assertTrue(ForegroundAdGate.isShowing)
        }
        assertFalse(ForegroundAdGate.isShowing)
    }

    @Test
    fun `gate closes when the block throws`() = runBlocking {
        runCatching { ForegroundAdGate.around { error("boom") } }
        assertFalse(ForegroundAdGate.isShowing)
    }
}
