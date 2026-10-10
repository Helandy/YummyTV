package su.afk.yummy.tv.data.player.repository

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.data.player.extractor.PlayerStreamExtractor
import su.afk.yummy.tv.data.player.extractor.SessionAwarePlayerStreamExtractor
import su.afk.yummy.tv.domain.player.model.AllohaStreamSession
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.repository.AllohaPlaybackSessionRepository

/**
 * Кэш resolve (3 минуты TTL, LRU на 8 записей) и выбор экстрактора. Истечение TTL не проверяем:
 * репозиторий читает `System.currentTimeMillis()` напрямую, без подменяемых часов.
 */
class DefaultPlayerStreamRepositoryTest : BaseUnitTest() {

    private val context: Context = mockk(relaxed = true)
    private val sessions: AllohaPlaybackSessionRepository = mockk(relaxed = true)

    private val plain = FakeExtractor(prefix = "https://plain/")
    private val alloha = FakeSessionExtractor(prefix = "https://alloha/")

    private fun repository(vararg extractors: PlayerStreamExtractor = arrayOf(plain, alloha)) =
        DefaultPlayerStreamRepository(context, linkedSetOf(*extractors), sessions)

    private fun request(
        url: String,
        label: String = "auto",
        forceRefresh: Boolean = false,
        reuse: Boolean = true,
    ) = PlayerStreamRequest(
        iframeUrl = url,
        autoQualityLabel = label,
        reusePlaybackSession = reuse,
        forceRefresh = forceRefresh,
    )

    private fun stream(url: String) = PlayerStreamResolveResult.Stream(url = url)

    @Test
    fun `url nobody supports is unsupported`() = runTest {
        val result = repository().resolve(request("https://other/video"))

        assertEquals(PlayerStreamResolveResult.Unsupported, result)
    }

    @Test
    fun `second resolve of the same source comes from the cache`() = runTest {
        plain.result = stream("a")
        val repository = repository()

        repository.resolve(request("https://plain/1"))
        val second = repository.resolve(request("https://plain/1"))

        assertEquals(stream("a"), second)
        assertEquals(1, plain.extractCalls)
    }

    @Test
    fun `cache key includes the auto quality label`() = runTest {
        plain.result = stream("a")
        val repository = repository()

        repository.resolve(request("https://plain/1", label = "auto"))
        repository.resolve(request("https://plain/1", label = "Авто"))

        assertEquals(2, plain.extractCalls)
    }

    @Test
    fun `only streams are cached`() = runTest {
        val repository = repository()
        val notCached = listOf(
            PlayerStreamResolveResult.Failed("x"),
            PlayerStreamResolveResult.Unavailable(),
            PlayerStreamResolveResult.KodikBlocked("m", 403),
        )

        notCached.forEachIndexed { index, result ->
            plain.result = result
            val url = "https://plain/$index"
            repository.resolve(request(url))
            repository.resolve(request(url))
        }

        assertEquals(notCached.size * 2, plain.extractCalls)
    }

    @Test
    fun `force refresh skips the cache and replaces the entry`() = runTest {
        plain.result = stream("old")
        val repository = repository()
        repository.resolve(request("https://plain/1"))

        plain.result = stream("new")
        val refreshed = repository.resolve(request("https://plain/1", forceRefresh = true))
        val afterwards = repository.resolve(request("https://plain/1"))

        assertEquals(stream("new"), refreshed)
        assertEquals(stream("new"), afterwards)
        assertEquals(2, plain.extractCalls)
    }

    @Test
    fun `least recently used entry is evicted after eight`() = runTest {
        plain.result = stream("a")
        val repository = repository()
        (1..8).forEach { repository.resolve(request("https://plain/$it")) }
        repository.resolve(request("https://plain/1")) // освежаем первую запись
        assertEquals(8, plain.extractCalls)

        repository.resolve(request("https://plain/9")) // вытесняет вторую
        repository.resolve(request("https://plain/1"))
        assertEquals(9, plain.extractCalls)
        repository.resolve(request("https://plain/2"))

        assertEquals(10, plain.extractCalls)
    }

    @Test
    fun `invalidate drops every label of one source only`() = runTest {
        plain.result = stream("a")
        val repository = repository()
        repository.resolve(request("https://plain/1", label = "auto"))
        repository.resolve(request("https://plain/1", label = "Авто"))
        repository.resolve(request("https://plain/2"))
        assertEquals(3, plain.extractCalls)

        repository.invalidateResolveCache("https://plain/1")
        repository.resolve(request("https://plain/1", label = "auto"))
        repository.resolve(request("https://plain/1", label = "Авто"))
        repository.resolve(request("https://plain/2"))

        assertEquals(5, plain.extractCalls)
    }

    @Test
    fun `session aware extractor bypasses the cache`() = runTest {
        alloha.result = stream("a")
        val repository = repository()

        repository.resolve(request("https://alloha/1"))
        repository.resolve(request("https://alloha/1"))

        assertEquals(2, alloha.extractCalls)
    }

    @Test
    fun `first extractor that supports the url is used`() = runTest {
        val first = FakeExtractor(prefix = "https://plain/").apply { result = stream("first") }
        val second = FakeExtractor(prefix = "https://plain/").apply { result = stream("second") }

        val result = repository(first, second).resolve(request("https://plain/1"))

        assertEquals(stream("first"), result)
        assertEquals(0, second.extractCalls)
    }

    @Test
    fun `open session without a session aware extractor is null`() = runTest {
        assertNull(repository(plain).openAllohaSession(request("https://alloha/1")))
    }

    @Test
    fun `reusable session is returned without opening a new one`() = runTest {
        val existing: AllohaStreamSession = mockk()
        every { sessions.find("https://alloha/1") } returns existing

        val result = repository().openAllohaSession(request("https://alloha/1"))

        assertSame(existing, result)
        assertEquals(0, alloha.openCalls)
    }

    @Test
    fun `missing session is opened and activated`() = runTest {
        val opened: AllohaStreamSession = mockk()
        alloha.session = opened
        every { sessions.find(any()) } returns null
        every { sessions.activate(opened) } returns opened

        val result = repository().openAllohaSession(request("https://alloha/1"))

        assertSame(opened, result)
        verify { sessions.activate(opened) }
    }

    @Test
    fun `failed open returns null and activates nothing`() = runTest {
        alloha.session = null
        every { sessions.find(any()) } returns null

        val result = repository().openAllohaSession(request("https://alloha/1"))

        assertNull(result)
        verify(exactly = 0) { sessions.activate(any()) }
    }

    @Test
    fun `without reuse the session is opened outside the manager`() = runTest {
        val opened: AllohaStreamSession = mockk()
        alloha.session = opened

        val result = repository().openAllohaSession(request("https://alloha/1", reuse = false))

        assertSame(opened, result)
        verify(exactly = 0) { sessions.find(any()) }
        verify(exactly = 0) { sessions.activate(any()) }
    }

    private open class FakeExtractor(private val prefix: String) : PlayerStreamExtractor {
        var result: PlayerStreamResolveResult = PlayerStreamResolveResult.Failed("unset")
        var extractCalls = 0

        override fun supports(url: String): Boolean = url.startsWith(prefix)

        override suspend fun extract(
            request: PlayerStreamRequest,
            context: Context,
        ): PlayerStreamResolveResult {
            extractCalls++
            return result
        }
    }

    private class FakeSessionExtractor(prefix: String) :
        FakeExtractor(prefix), SessionAwarePlayerStreamExtractor {
        var session: AllohaStreamSession? = null
        var openCalls = 0

        override suspend fun openSession(
            request: PlayerStreamRequest,
            context: Context,
        ): AllohaStreamSession? {
            openCalls++
            return session
        }
    }
}
