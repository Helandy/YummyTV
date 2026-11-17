package su.afk.yummy.tv.source.torrent.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.model.settings.TorrServerCheckResult
import su.afk.yummy.tv.core.model.settings.TorrentIndexer
import su.afk.yummy.tv.core.model.settings.TorrentIndexerCheckResult
import su.afk.yummy.tv.core.model.settings.TorrentSettings
import su.afk.yummy.tv.core.testing.BaseUnitTest
import java.io.IOException

/**
 * Проверка TorrServer и индексаторов из настроек. Ответы Jackett на `t=caps` (`<caps>` и `<error code="100">`)
 * взяты из формата Torznab, вживую с Jackett не сверены.
 */
class TorrServerCheckerImplTest : BaseUnitTest() {

    private val requests = mutableListOf<HttpRequestData>()

    @Test
    fun `jacred answering the search probe is available`() = runTest {
        val result = createChecker { respond("[]", HttpStatusCode.OK) }
            .checkIndexer(TorrentSettings(jacRedUrl = ""), TorrentIndexer.JACRED)

        assertEquals(TorrentIndexerCheckResult.Ok, result)
        val url = requests.single().url
        assertEquals("jac-red.ru", url.host)
        assertEquals("/api/v1.0/torrents", url.encodedPath)
        assertEquals("naruto", url.parameters["search"])
    }

    @Test
    fun `jacred server error is reported with its code`() = runTest {
        val result = createChecker { respond("", HttpStatusCode.BadGateway) }
            .checkIndexer(TorrentSettings(), TorrentIndexer.JACRED)

        assertEquals(TorrentIndexerCheckResult.UnexpectedResponse(502), result)
    }

    @Test
    fun `unreachable indexer is reported as unreachable`() = runTest {
        val result = createChecker { throw IOException("connection refused") }
            .checkIndexer(TorrentSettings(), TorrentIndexer.JACRED)

        assertEquals(TorrentIndexerCheckResult.Unreachable, result)
    }

    @Test
    fun `jackett caps answer with the key means available`() = runTest {
        val result = createChecker { respond(CAPS_BODY, HttpStatusCode.OK) }
            .checkIndexer(jackettSettings(), TorrentIndexer.JACKETT)

        assertEquals(TorrentIndexerCheckResult.Ok, result)
        val url = requests.single().url
        assertEquals("/api/v2.0/indexers/all/results/torznab/api", url.encodedPath)
        assertEquals(API_KEY, url.parameters["apikey"])
        assertEquals("caps", url.parameters["t"])
    }

    @Test
    fun `jackett torznab error is a rejected key`() = runTest {
        val result = createChecker { respond(INVALID_KEY_BODY, HttpStatusCode.OK) }
            .checkIndexer(jackettSettings(), TorrentIndexer.JACKETT)

        assertEquals(TorrentIndexerCheckResult.Unauthorized, result)
    }

    @Test
    fun `jackett http 401 is a rejected key`() = runTest {
        val result = createChecker { respond("", HttpStatusCode.Unauthorized) }
            .checkIndexer(jackettSettings(), TorrentIndexer.JACKETT)

        assertEquals(TorrentIndexerCheckResult.Unauthorized, result)
    }

    @Test
    fun `jackett answering something else is unexpected`() = runTest {
        val result = createChecker { respond("<html>login</html>", HttpStatusCode.OK) }
            .checkIndexer(jackettSettings(), TorrentIndexer.JACKETT)

        assertEquals(TorrentIndexerCheckResult.UnexpectedResponse(200), result)
    }

    @Test
    fun `jackett without an address is unreachable without a request`() = runTest {
        val result = createChecker { respond(CAPS_BODY, HttpStatusCode.OK) }
            .checkIndexer(jackettSettings().copy(jackettUrl = " "), TorrentIndexer.JACKETT)

        assertEquals(TorrentIndexerCheckResult.Unreachable, result)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `torrserver echo answers with its version`() = runTest {
        val result = createChecker { respond("MatriX.136 \n", HttpStatusCode.OK) }
            .check(TorrentSettings(torrServerUrl = "192.168.1.5:8090/"))

        assertEquals(TorrServerCheckResult.Ok("MatriX.136"), result)
        assertEquals("http://192.168.1.5:8090/echo", requests.single().url.toString())
    }

    @Test
    fun `torrserver asking for credentials is unauthorized`() = runTest {
        val result = createChecker { respond("", HttpStatusCode.Unauthorized) }
            .check(TorrentSettings(torrServerUrl = "http://ts.local"))

        assertEquals(TorrServerCheckResult.Unauthorized, result)
    }

    private fun createChecker(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): TorrServerCheckerImpl {
        val engine = MockEngine { request ->
            requests += request
            handler(request)
        }
        return TorrServerCheckerImpl(HttpClient(engine) { install(HttpTimeout) })
    }

    private fun jackettSettings() = TorrentSettings(
        jackettEnabled = true,
        jackettUrl = "http://192.168.1.10:9117",
        jackettApiKey = API_KEY,
    )

    private companion object {
        const val API_KEY = "abc123"
        const val CAPS_BODY = """<?xml version="1.0" encoding="UTF-8"?><caps><server title="Jackett" /></caps>"""
        const val INVALID_KEY_BODY =
            """<?xml version="1.0" encoding="UTF-8"?><error code="100" description="Invalid API Key" />"""
    }
}
