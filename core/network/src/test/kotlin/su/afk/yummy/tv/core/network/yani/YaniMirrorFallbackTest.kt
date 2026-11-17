package su.afk.yummy.tv.core.network.yani

import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.network.yummy.SiteApiConfig
import su.afk.yummy.tv.core.testing.BaseUnitTest
import java.io.IOException

class YaniMirrorFallbackTest : BaseUnitTest() {

    private val clientProvider = mockk<YaniHttpClientProvider>(relaxed = true)
    private val siteApiConfig = mockk<SiteApiConfig>(relaxed = true)
    private var clock = 0L
    private lateinit var fallback: YaniMirrorFallback
    private var primaryCalls = 0
    private var mirrorCalls = 0

    @Before
    fun setUp() {
        fallback = YaniMirrorFallback(clientProvider, siteApiConfig, emptySet()).also { it.now = { clock } }
    }

    private suspend fun run(primaryFails: Boolean = false, mirrorFails: Boolean = false, error: Exception = IOException("yani down")): String =
        fallback.call(
            primary = {
                primaryCalls++
                if (primaryFails) throw error
                "yani"
            },
            mirror = {
                mirrorCalls++
                if (mirrorFails) throw IllegalStateException("mirror down")
                "mirror"
            },
        )

    @Test
    fun `healthy yani never touches the mirror`() = runTest {
        assertEquals("yani", run())
        assertEquals(0, mirrorCalls)
    }

    @Test
    fun `yani outage is served from the mirror`() = runTest {
        assertEquals("mirror", run(primaryFails = true))
        assertEquals(1, primaryCalls)
    }

    @Test
    fun `when the mirror fails too the original yani error is thrown`() = runTest {
        val error = IOException("yani down")

        val thrown = runCatching { run(primaryFails = true, mirrorFails = true, error = error) }.exceptionOrNull()

        assertSame(error, thrown)
    }

    @Test
    fun `two outages open the breaker, then mirror goes first until it expires`() = runTest {
        run(primaryFails = true)
        run(primaryFails = true)
        primaryCalls = 0

        assertEquals("mirror", run(primaryFails = true))
        assertEquals(0, primaryCalls)

        clock += YaniMirrorFallback.OPEN_MS + 1
        assertEquals("yani", run())
        assertEquals(1, primaryCalls)
    }

    @Test
    fun `state shows the copy while yani is down and clears when yani answers`() = runTest {
        assertEquals(false, fallback.state.value.active)

        run(primaryFails = true)
        assertEquals(true, fallback.state.value.active)

        clock += YaniMirrorFallback.OPEN_MS + 1
        run()
        assertEquals(false, fallback.state.value.active)
    }

    @Test
    fun `trace tells repositories whether the answer came from the copy`() = runTest {
        val fromYani = withMirrorTrace { run() }
        val fromCopy = withMirrorTrace { run(primaryFails = true) }

        assertEquals(false, fromYani.fromMirror)
        assertEquals("mirror", fromCopy.value)
        assertEquals(true, fromCopy.fromMirror)
    }

    @Test
    fun `open breaker retries yani when the mirror cannot answer`() = runTest {
        run(primaryFails = true)
        run(primaryFails = true)
        primaryCalls = 0

        assertEquals("yani", run(mirrorFails = true))
        assertEquals(1, primaryCalls)
    }
}
