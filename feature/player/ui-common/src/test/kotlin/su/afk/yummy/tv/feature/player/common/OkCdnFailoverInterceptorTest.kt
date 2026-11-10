package su.afk.yummy.tv.feature.player.common

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.common.player.OkCdnHostFailover
import su.afk.yummy.tv.core.testing.BaseUnitTest

/**
 * Узлы в тестах у каждого свои: реестр общий на процесс, а тесты не должны делить его состояние.
 */
class OkCdnFailoverInterceptorTest : BaseUnitTest() {

    private val codeByHost = mutableMapOf<String, Int>()
    private val requestedHosts = mutableListOf<String>()

    private fun client(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(OkCdnFailoverInterceptor())
        .addInterceptor(
            Interceptor { chain ->
                val request = chain.request()
                requestedHosts += request.url.host
                response(request, codeByHost[request.url.host] ?: 206)
            },
        )
        .build()

    private fun call(client: OkHttpClient, host: String): Response =
        client.newCall(Request.Builder().url("https://$host/video?sig=abc").build()).execute()

    private fun response(request: Request, code: Int): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("test")
        .body("x".toResponseBody())
        .build()

    @Test
    fun `host without registered failover is passed through untouched`() {
        codeByHost["plain-1.okcdn.ru"] = 400

        val response = call(client(), "plain-1.okcdn.ru")

        assertEquals(400, response.code)
        assertEquals(listOf("plain-1.okcdn.ru"), requestedHosts)
    }

    @Test
    fun `healthy primary host answers directly`() {
        OkCdnHostFailover.register("ok-primary.okcdn.ru", "ok-backup.okcdn.ru")

        val response = call(client(), "ok-primary.okcdn.ru")

        assertEquals(206, response.code)
        assertEquals(listOf("ok-primary.okcdn.ru"), requestedHosts)
    }

    @Test
    fun `rejected primary request is repeated on the failover host`() {
        OkCdnHostFailover.register("bad-primary.okcdn.ru", "bad-backup.okcdn.ru")
        codeByHost["bad-primary.okcdn.ru"] = 400

        val response = call(client(), "bad-primary.okcdn.ru")

        assertEquals(206, response.code)
        assertEquals(listOf("bad-primary.okcdn.ru", "bad-backup.okcdn.ru"), requestedHosts)
        assertEquals("https://bad-backup.okcdn.ru/video?sig=abc", response.request.url.toString())
    }

    @Test
    fun `forbidden answer also moves to the failover host`() {
        OkCdnHostFailover.register("forbidden-primary.okcdn.ru", "forbidden-backup.okcdn.ru")
        codeByHost["forbidden-primary.okcdn.ru"] = 403

        assertEquals(206, call(client(), "forbidden-primary.okcdn.ru").code)
    }

    @Test
    fun `later requests skip the host that rejected`() {
        OkCdnHostFailover.register("dead-primary.okcdn.ru", "dead-backup.okcdn.ru")
        codeByHost["dead-primary.okcdn.ru"] = 400
        val client = client()
        call(client, "dead-primary.okcdn.ru")
        requestedHosts.clear()

        call(client, "dead-primary.okcdn.ru")

        assertEquals(listOf("dead-backup.okcdn.ru"), requestedHosts)
    }

    @Test
    fun `server errors that are not node rejections are not rerouted`() {
        OkCdnHostFailover.register("error-primary.okcdn.ru", "error-backup.okcdn.ru")
        codeByHost["error-primary.okcdn.ru"] = 503

        val response = call(client(), "error-primary.okcdn.ru")

        assertEquals(503, response.code)
        assertEquals(listOf("error-primary.okcdn.ru"), requestedHosts)
    }

    @Test
    fun `failing failover answer is returned to the caller`() {
        OkCdnHostFailover.register("both-primary.okcdn.ru", "both-backup.okcdn.ru")
        codeByHost["both-primary.okcdn.ru"] = 400
        codeByHost["both-backup.okcdn.ru"] = 400

        val response = call(client(), "both-primary.okcdn.ru")

        assertEquals(400, response.code)
        assertEquals(listOf("both-primary.okcdn.ru", "both-backup.okcdn.ru"), requestedHosts)
    }

    @Test
    fun `dead host is retried first after the quarantine expires`() {
        OkCdnHostFailover.register("ttl-primary.okcdn.ru", "ttl-backup.okcdn.ru")
        OkCdnHostFailover.markDead("ttl-primary.okcdn.ru", nowNanos = 0L)

        assertTrue(OkCdnHostFailover.isDead("ttl-primary.okcdn.ru", nowNanos = 1_000L))
        assertTrue(!OkCdnHostFailover.isDead("ttl-primary.okcdn.ru", nowNanos = Long.MAX_VALUE))
    }
}
