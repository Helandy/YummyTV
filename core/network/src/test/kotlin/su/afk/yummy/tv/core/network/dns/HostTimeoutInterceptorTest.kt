package su.afk.yummy.tv.core.network.dns

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertSame
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
}
