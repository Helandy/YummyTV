package su.afk.yummy.tv.android.image

import coil3.network.CacheStrategy
import coil3.network.NetworkHeaders
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.request.Options
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import kotlin.time.Duration.Companion.days

class StaticAssetCacheStrategyTest : BaseUnitTest() {

    @Test
    fun `картинки любого формата считаются картинками`() {
        assertTrue(StaticAssetCacheStrategy.isImage("image/jpeg"))
        assertTrue(StaticAssetCacheStrategy.isImage(" Image/WEBP; charset=binary"))
        assertTrue(StaticAssetCacheStrategy.isImage("image/avif"))
    }

    @Test
    fun `не картинки и отсутствие типа не считаются картинками`() {
        assertFalse(StaticAssetCacheStrategy.isImage(null))
        assertFalse(StaticAssetCacheStrategy.isImage("application/json"))
        assertFalse(StaticAssetCacheStrategy.isImage("text/html"))
    }

    @Test
    fun `ожидаемая длина берётся из Content-Length только для несжатого тела`() {
        fun headers(vararg h: Pair<String, String>) = NetworkHeaders.Builder().apply { h.forEach { set(it.first, it.second) } }.build()
        assertEquals(91539L, StaticAssetCacheStrategy.expectedBodyLength(headers("Content-Length" to "91539")))
        assertEquals(10L, StaticAssetCacheStrategy.expectedBodyLength(headers("Content-Length" to "10", "Content-Encoding" to "identity")))
        assertEquals(null, StaticAssetCacheStrategy.expectedBodyLength(headers("Content-Length" to "10", "Content-Encoding" to "gzip")))
        assertEquals(null, StaticAssetCacheStrategy.expectedBodyLength(headers()))
        assertEquals(null, StaticAssetCacheStrategy.expectedBodyLength(headers("Content-Length" to "abc")))
    }

    @Test
    fun `свежесть статики и картинок — 30 дней`() {
        assertEquals(30.days, StaticAssetCacheStrategy.StaticFreshness)
    }

    @Test
    fun `собственная статика распознаётся по префиксу`() {
        assertTrue(StaticAssetCacheStrategy.isOwnStatic("https://x.io/static/a.webp", "https://x.io/static/"))
        assertFalse(StaticAssetCacheStrategy.isOwnStatic("https://cdn.io/a.webp", "https://x.io/static/"))
        assertFalse(StaticAssetCacheStrategy.isOwnStatic("https://x.io/static/a.webp", ""))
    }

    private val now = 1_000_000_000_000L
    private val options: Options = mockk(relaxed = true)
    private val delegated = CacheStrategy.ReadResult(mockk<NetworkRequest>())
    private val delegate: CacheStrategy = mockk()
    private val strategy = StaticAssetCacheStrategy({ "https://x.io/static/" }, delegate) { now }

    private fun cached(code: Int, type: String?, ageDays: Long): NetworkResponse {
        val headers = NetworkHeaders.Builder().apply { if (type != null) set("Content-Type", type) }.build()
        return NetworkResponse(code = code, requestMillis = 0, responseMillis = now - ageDays.days.inWholeMilliseconds, headers = headers)
    }

    private suspend fun read(response: NetworkResponse, url: String = "https://cdn.io/p.jpg"): CacheStrategy.ReadResult {
        coEvery { delegate.read(any(), any(), any()) } returns delegated
        return strategy.read(response, NetworkRequest(url), options)
    }

    @Test
    fun `свежая картинка из кэша отдаётся без обращения к стратегии по умолчанию`() = runTest {
        val response = cached(200, "image/jpeg", ageDays = 29)
        assertSame(response, read(response).response)
    }

    @Test
    fun `картинка старше 30 дней уходит в стратегию по умолчанию`() = runTest {
        assertSame(delegated, read(cached(200, "image/jpeg", ageDays = 31)))
    }

    @Test
    fun `не картинка и не статика уходит в стратегию по умолчанию`() = runTest {
        assertSame(delegated, read(cached(200, "application/json", ageDays = 1)))
    }

    @Test
    fun `не 200 уходит в стратегию по умолчанию`() = runTest {
        assertSame(delegated, read(cached(404, "image/jpeg", ageDays = 1)))
    }

    @Test
    fun `своя статика без Content-Type остаётся свежей`() = runTest {
        val response = cached(200, null, ageDays = 20)
        assertSame(response, read(response, "https://x.io/static/a.webp").response)
    }
}
