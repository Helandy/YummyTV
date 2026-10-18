package su.afk.yummy.tv.core.utils.kodik

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.common.coroutines.AppClock
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.utils.network.BrowserUserAgentProvider

class ResolveKodikThumbnailUrlUseCaseTest : BaseUnitTest() {

    private var nowMs = 0L
    private var requests = 0
    private var failRequests = true
    private val clock = object : AppClock {
        override fun nowMillis(): Long = nowMs
    }
    private val userAgents: BrowserUserAgentProvider = mockk {
        every { userAgent } returns "ua"
    }
    private val httpClient = HttpClient(
        MockEngine {
            requests++
            if (failRequests) {
                respondError(HttpStatusCode.ServiceUnavailable)
            } else {
                respond("""<script>var thumb = "//i.kodik.biz/abc/thumb001.jpg";</script>""")
            }
        },
    ) { install(HttpTimeout) }

    private fun TestScope.createUseCase() =
        ResolveKodikThumbnailUrlUseCase(httpClient, backgroundScope, userAgents, clock)

    @Test
    fun `failure is retried after ttl`() = runTest {
        val useCase = createUseCase()

        assertNull(useCase(IFRAME))
        failRequests = false
        nowMs += 1_000L
        assertNull(useCase(IFRAME))
        assertEquals(1, requests)

        nowMs += 60_000L
        assertEquals("https://i.kodik.biz/abc/thumb001.jpg", useCase(IFRAME))
        assertEquals(2, requests)
    }

    @Test
    fun `success is cached`() = runTest {
        failRequests = false
        val useCase = createUseCase()

        useCase(IFRAME)
        nowMs += 3_600_000L
        assertEquals("https://i.kodik.biz/abc/thumb001.jpg", useCase(IFRAME))
        assertEquals(1, requests)
    }

    @Test
    fun `direct image url is returned without request`() = runTest {
        val useCase = createUseCase()

        assertEquals(
            "https://i.kodikres.com/screenshots/seria/1060462/1.jpg",
            useCase("//i.kodikres.com/screenshots/seria/1060462/1.jpg"),
        )
        assertEquals(0, requests)
    }

    private companion object {
        const val IFRAME = "//kodikplayer.com/season/1/a/720p?episode=1"
    }
}
