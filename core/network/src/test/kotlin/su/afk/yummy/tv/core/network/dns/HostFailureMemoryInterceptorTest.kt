package su.afk.yummy.tv.core.network.dns

import io.mockk.every
import io.mockk.mockk
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLHandshakeException

class HostFailureMemoryInterceptorTest {

    private var now = 1_000_000L
    private val interceptor = HostFailureMemoryInterceptor(
        isTarget = { url -> isImageCdnRequest(url, staticHost = "") },
        failureThreshold = 2,
        downMillis = 60_000,
        nowMillis = { now },
    )

    private fun request(url: String) = Request.Builder().url(url).build()

    private fun ok(request: Request) =
        Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK").build()

    /** Выполняет [url] через интерцептор; [result] имитирует сеть: ответ или исключение. Возвращает исключение или null. */
    private fun call(url: String, canceled: Boolean = false, result: (Request) -> Response): IOException? {
        val request = request(url)
        val callMock = mockk<Call>()
        every { callMock.isCanceled() } returns canceled
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.call() } returns callMock
        every { chain.proceed(request) } answers { result(request) }
        return try {
            interceptor.intercept(chain)
            null
        } catch (e: IOException) {
            e
        }
    }

    private fun timeout(url: String) = call(url) { throw SocketTimeoutException("Read timed out") }
    private fun success(url: String) = call(url) { ok(it) }
    private val dead = "https://imgproxy.yani.tv/posters/mega/1.avif"

    @Test
    fun `single failure does not mark the host down`() {
        assertTrue(timeout(dead) is SocketTimeoutException)

        assertEquals(null, success(dead))
    }

    @Test
    fun `second failure in a row marks the host down and fails fast`() {
        timeout(dead)
        timeout(dead)

        var reachedNetwork = false
        val error = call(dead) { reachedNetwork = true; ok(it) }

        assertTrue(error is HostTemporarilyDownException)
        assertFalse(reachedNetwork)
    }

    @Test
    fun `success between failures resets the counter`() {
        timeout(dead)
        success(dead)
        timeout(dead)

        assertEquals(null, success(dead))
    }

    @Test
    fun `other hosts are not affected by a down host`() {
        timeout(dead)
        timeout(dead)

        assertEquals(null, success("https://static.yani.tv/posters/mega/1.avif"))
    }

    @Test
    fun `host gets one probe after the down period and recovers on success`() {
        timeout(dead)
        timeout(dead)
        now += 60_001

        assertEquals(null, success(dead))
        assertEquals(null, success(dead))
    }

    @Test
    fun `failed probe extends the down period`() {
        timeout(dead)
        timeout(dead)
        now += 60_001

        assertTrue(timeout(dead) is SocketTimeoutException)
        assertTrue(success(dead) is HostTemporarilyDownException)
        now += 60_001
        assertEquals(null, success(dead))
    }

    @Test
    fun `parallel request waits out the probe in flight`() {
        timeout(dead)
        timeout(dead)
        now += 60_001

        var second: IOException? = null
        call(dead) {
            second = success(dead)
            ok(it)
        }

        assertTrue(second is HostTemporarilyDownException)
    }

    @Test
    fun `connect refusal and tls failure also count`() {
        call(dead) { throw ConnectException("refused") }
        call(dead) { throw SSLHandshakeException("handshake") }

        assertTrue(success(dead) is HostTemporarilyDownException)
    }

    @Test
    fun `connection abort and canceled calls do not count`() {
        call(dead) { throw SocketException("Software caused connection abort") }
        call(dead) { throw SocketException("Software caused connection abort") }
        call(dead, canceled = true) { throw SocketTimeoutException("timeout") }
        call(dead, canceled = true) { throw SocketTimeoutException("timeout") }

        assertEquals(null, success(dead))
    }

    @Test
    fun `non image requests to the same domain are never blocked`() {
        val page = "https://alloha.yani.tv/?token=1"
        repeat(3) { call(page) { throw SocketTimeoutException("timeout") } }

        assertEquals(null, success(page))
    }

    @Test
    fun `api host is never blocked`() {
        val api = "https://api.yani.tv/feed"
        repeat(3) { call(api) { throw SocketTimeoutException("timeout") } }

        assertEquals(null, success(api))
    }
}
