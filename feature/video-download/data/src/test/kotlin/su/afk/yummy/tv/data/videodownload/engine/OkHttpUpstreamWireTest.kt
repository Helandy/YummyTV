package su.afk.yummy.tv.data.videodownload.engine

import android.net.Uri
import android.text.TextUtils
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import com.sun.net.httpserver.HttpServer
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference

/** Проверка на проводе: что реально уходит на сервер из источника данных загрузки. */
@UnstableApi
class OkHttpUpstreamWireTest : BaseUnitTest() {

    private val received = AtomicReference<Map<String, String>>(emptyMap())
    private lateinit var server: HttpServer

    @Before
    fun startServer() {
        // Media3 читает ответ через android.text.TextUtils, которого в JVM-тесте нет.
        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } answers { firstArg<CharSequence?>().isNullOrEmpty() }
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/") { exchange ->
                received.set(exchange.requestHeaders.entries.associate { it.key.lowercase() to it.value.first() })
                exchange.sendResponseHeaders(200, 2)
                exchange.responseBody.use { it.write("ok".toByteArray()) }
            }
            start()
        }
    }

    @After
    fun stopServer() {
        server.stop(0)
    }

    private fun fetch(headers: Map<String, String>): Map<String, String> {
        val source = okHttpUpstreamFactory(OkHttpClient(), headers).createDataSource()
        try {
            // android.net.Uri в JVM-тесте не работает: источнику нужна только строка адреса.
            val uri = mockk<Uri>(relaxed = true)
            every { uri.toString() } returns "http://127.0.0.1:${server.address.port}/seg.ts"
            source.open(DataSpec(uri))
        } finally {
            source.close()
        }
        return received.get()
    }

    @Test
    fun `selected profile user agent and request headers reach the server`() {
        val wire = fetch(mapOf("User-Agent" to "Profile/Linux", "Referer" to "https://r.example/", "X-Test" to "1"))

        assertEquals("Profile/Linux", wire["user-agent"])
        assertEquals("https://r.example/", wire["referer"])
        assertEquals("1", wire["x-test"])
    }

    @Test
    fun `without a user agent the client sends the okhttp default`() {
        val wire = fetch(emptyMap())

        assertEquals(true, wire["user-agent"]?.startsWith("okhttp/") == true)
    }
}
