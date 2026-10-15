package su.afk.yummy.tv.data.player.extractor.extra

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository

class ExtraSourceExtractorTest : BaseUnitTest() {

    private val context: Context = mockk(relaxed = true)

    private class FakeRepository(private val prefix: String) : ExtraStreamRepository {
        val resolved = mutableListOf<String>()

        override fun supports(url: String): Boolean = url.startsWith(prefix)

        override suspend fun resolve(url: String): PlayerStreamResolveResult {
            resolved += url
            return PlayerStreamResolveResult.Stream(url = "direct:$url")
        }
    }

    private fun request(url: String) = PlayerStreamRequest(
        iframeUrl = url,
        autoQualityLabel = "auto",
        reusePlaybackSession = true,
        forceRefresh = false,
    )

    @Test
    fun `without modules nothing is supported`() = runTest {
        val extractor = ExtraSourceExtractor(emptySet())

        assertFalse(extractor.supports("source://anime4k/1/1/1/X"))
        assertEquals(
            PlayerStreamResolveResult.Unsupported,
            extractor.extract(request("source://anime4k/1/1/1/X"), context),
        )
    }

    @Test
    fun `url goes to the module that supports it`() = runTest {
        val anime4k = FakeRepository("source://anime4k/")
        val other = FakeRepository("source://other/")
        val extractor = ExtraSourceExtractor(linkedSetOf(anime4k, other))

        val result = extractor.extract(request("source://other/42"), context)

        assertTrue(extractor.supports("source://anime4k/1"))
        assertFalse(extractor.supports("https://kodik.info/x"))
        assertEquals(PlayerStreamResolveResult.Stream(url = "direct:source://other/42"), result)
        assertTrue(anime4k.resolved.isEmpty())
        assertEquals(listOf("source://other/42"), other.resolved)
    }
}
