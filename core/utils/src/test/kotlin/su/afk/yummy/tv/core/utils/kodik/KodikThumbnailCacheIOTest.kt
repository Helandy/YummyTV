package su.afk.yummy.tv.core.utils.kodik

import coil3.disk.DiskCache
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toOkioPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import su.afk.yummy.tv.core.testing.BaseUnitTest

class KodikThumbnailCacheIOTest : BaseUnitTest() {

    @get:Rule
    val tmp = TemporaryFolder()

    private val requested = mutableListOf<String>()
    private val httpClient = HttpClient(
        MockEngine { request ->
            requested += request.url.toString()
            when (request.url.host) {
                "blocked.example" -> respondError(HttpStatusCode.NotFound)
                "html.example" -> respond("<html>captcha</html>", headers = headersOf(HttpHeaders.ContentType, "text/html"))
                else -> respond(byteArrayOf(1, 2, 3), headers = headersOf(HttpHeaders.ContentType, "image/jpeg"))
            }
        },
    )

    private fun cacheIO() = KodikThumbnailCacheIO(
        diskCache = DiskCache.Builder().directory(tmp.newFolder().toOkioPath()).maxSizeBytes(1024 * 1024).build(),
        httpClient = httpClient,
    )

    @Test
    fun `next candidate is used when the first one cannot be downloaded`() = runTest {
        val result = cacheIO().fetch(
            cacheKey = "k",
            candidates = flowOf("https://blocked.example/a.jpg", "https://html.example/b.jpg", "https://ok.example/c.jpg"),
        )

        assertNotNull(result)
        assertEquals(
            listOf("https://blocked.example/a.jpg", "https://html.example/b.jpg", "https://ok.example/c.jpg"),
            requested,
        )
    }

    @Test
    fun `later candidates are not touched after a success`() = runTest {
        var reachedSecond = false
        val candidates = flow {
            emit("https://ok.example/first.jpg")
            reachedSecond = true
            emit("https://ok.example/second.jpg")
        }

        assertNotNull(cacheIO().fetch("k", candidates))

        assertEquals(false, reachedSecond)
        assertEquals(listOf("https://ok.example/first.jpg"), requested)
    }

    @Test
    fun `no downloadable candidate gives null`() = runTest {
        assertNull(cacheIO().fetch("k", flowOf("https://blocked.example/a.jpg")))
    }

    @Test
    fun `cached snapshot is served without touching candidates`() = runTest {
        val cacheIO = cacheIO()
        cacheIO.fetch("k", flowOf("https://ok.example/c.jpg"))
        requested.clear()
        val untouched = flow<String> { error("candidates must not be collected on a cache hit") }

        assertNotNull(cacheIO.fetch("k", untouched))
        assertEquals(emptyList<String>(), requested)
    }
}
