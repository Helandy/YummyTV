package su.afk.yummy.tv.feature.player.utils

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.network.connectivity.NetworkConnectivityMonitor
import su.afk.yummy.tv.core.testing.BaseUnitTest

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerConnectivityUtilsTest : BaseUnitTest() {

    private val online = MutableStateFlow(false)
    private var reachable = false
    private val connectivity: NetworkConnectivityMonitor = mockk {
        every { isOnline } returns online
        coEvery { isInternetReachable() } answers { reachable }
    }

    @Test
    fun `сеть уже есть - ожидания и пробы нет`() = runTest {
        online.value = true

        assertTrue(connectivity.awaitOnline(TIMEOUT_MS))
    }

    @Test
    fun `проба достучалась - система врёт про отсутствие сети, ждать не нужно`() = runTest {
        reachable = true

        assertTrue(connectivity.awaitOnline(TIMEOUT_MS))
    }

    @Test
    fun `без сети ждём её появления`() = runTest {
        var result: Boolean? = null
        backgroundScope.launch { result = connectivity.awaitOnline(TIMEOUT_MS) }
        runCurrent()
        assertEquals(null, result)

        online.value = true
        runCurrent()

        assertEquals(true, result)
    }

    @Test
    fun `сеть так и не появилась - по таймауту возвращаем false`() = runTest {
        var result: Boolean? = null
        backgroundScope.launch { result = connectivity.awaitOnline(TIMEOUT_MS) }

        advanceTimeBy(TIMEOUT_MS + 1)
        runCurrent()

        assertFalse(result!!)
    }

    private companion object {
        const val TIMEOUT_MS = 30_000L
    }
}
