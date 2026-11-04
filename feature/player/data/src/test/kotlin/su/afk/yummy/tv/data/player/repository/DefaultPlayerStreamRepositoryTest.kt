package su.afk.yummy.tv.data.player.repository

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.data.player.extractor.PlayerStreamExtractor
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult

/**
 * Кэш resolve (3 минуты TTL, LRU на 8 записей) и выбор экстрактора. Истечение TTL не проверяем:
 * репозиторий читает `System.currentTimeMillis()` напрямую, без подменяемых часов.
 */
class DefaultPlayerStreamRepositoryTest : BaseUnitTest() {

    private val context: Context = mockk(relaxed = true)

    private val plain = FakeExtractor(prefix = "https://plain/")
    private val alloha = FakeExtractor(prefix = "https://alloha/")

    private fun repository(vararg extractors: PlayerStreamExtractor = arrayOf(plain, alloha)) =
        DefaultPlayerStreamRepository(context, linkedSetOf(*extractors))

    private fun request(
        url: String,
        label: String = "auto",
        forceRefresh: Boolean = false,
    ) = PlayerStreamRequest(
        iframeUrl = url,
        autoQualityLabel = label,
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
    fun `extra module sources are never cached by the shared repository`() = runTest {
        val extra = FakeExtractor(prefix = "source://")
        extra.result = stream("a")
        val repository = repository(plain, extra)

        repository.resolve(request("source://anime4k/1/1/1/X"))
        repository.resolve(request("source://anime4k/1/1/1/X"))

        assertEquals(2, extra.extractCalls)
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
    fun `alloha is cached like any other source and force refresh bypasses the cache`() = runTest {
        alloha.result = stream("a")
        val repository = repository()

        repository.resolve(request("https://alloha/1"))
        repository.resolve(request("https://alloha/1"))
        assertEquals(1, alloha.extractCalls)

        repository.resolve(request("https://alloha/1", forceRefresh = true))
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

    private class FakeExtractor(private val prefix: String) : PlayerStreamExtractor {
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
}
