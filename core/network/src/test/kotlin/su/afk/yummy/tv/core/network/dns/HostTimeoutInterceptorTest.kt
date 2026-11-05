package su.afk.yummy.tv.core.network.dns

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class HostTimeoutInterceptorTest {

    private val interceptor = HostTimeoutInterceptor(setOf("image.tmdb.org"), connectTimeoutSeconds = 3, readTimeoutSeconds = 5)

    private fun response(request: Request) =
        Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK").build()

    @Test
    fun `listed host gets short connect and read timeouts`() {
        val request = Request.Builder().url("https://image.tmdb.org/t/p/w300/a.jpg").build()
        val shortened = mockk<Interceptor.Chain>()
        every { shortened.withReadTimeout(5, TimeUnit.SECONDS) } returns shortened
        every { shortened.proceed(request) } returns response(request)
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.withConnectTimeout(3, TimeUnit.SECONDS) } returns shortened

        interceptor.intercept(chain)

        verify { chain.withConnectTimeout(3, TimeUnit.SECONDS) }
        verify { shortened.withReadTimeout(5, TimeUnit.SECONDS) }
        verify(exactly = 0) { chain.proceed(any()) }
    }

    @Test
    fun `other hosts keep the client timeouts`() {
        val request = Request.Builder().url("https://kodikplayer.com/season/1").build()
        val expected = response(request)
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.proceed(request) } returns expected

        assertSame(expected, interceptor.intercept(chain))
        verify(exactly = 0) { chain.withConnectTimeout(any(), any()) }
        verify(exactly = 0) { chain.withReadTimeout(any(), any()) }
    }

    /** true, если [interceptor] сократил таймауты запроса на [url]; false, если пропустил запрос как есть. */
    private fun shortensTimeouts(interceptor: Interceptor, url: String): Boolean {
        val request = Request.Builder().url(url).build()
        val shortened = mockk<Interceptor.Chain>()
        every { shortened.withReadTimeout(5, TimeUnit.SECONDS) } returns shortened
        every { shortened.proceed(request) } returns response(request)
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.withConnectTimeout(3, TimeUnit.SECONDS) } returns shortened
        every { chain.proceed(request) } returns response(request)

        interceptor.intercept(chain)

        return try {
            verify { chain.withConnectTimeout(3, TimeUnit.SECONDS) }
            true
        } catch (_: AssertionError) {
            false
        }
    }

    @Test
    fun `own static host gets the same short timeouts as the tmdb cdn`() {
        val interceptor = imageHostTimeouts(staticHost = "static.example")

        assertTrue(shortensTimeouts(interceptor, "https://static.example/img/still/a.avif"))
        assertTrue(shortensTimeouts(interceptor, "https://image.tmdb.org/t/p/w300/a.jpg"))
        assertFalse(shortensTimeouts(interceptor, "https://kodikplayer.com/season/1"))
    }

    @Test
    fun `blank static host leaves only the tmdb cdn shortened`() {
        val interceptor = imageHostTimeouts(staticHost = "")

        assertTrue(shortensTimeouts(interceptor, "https://image.tmdb.org/t/p/w300/a.jpg"))
        assertFalse(shortensTimeouts(interceptor, "https://static.example/img/still/a.avif"))
    }
}
