package su.afk.yummy.tv.core.network.progress

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.common.player.PlayerStreamResolveStatus
import su.afk.yummy.tv.core.common.player.ResolveDetail
import su.afk.yummy.tv.core.common.player.ResolveProgress

class ResolveProgressPluginTest {

    private fun client(status: HttpStatusCode) = HttpClient(MockEngine { respond("ok", status) }) {
        install(ResolveProgressPlugin)
    }

    private fun steps(status: HttpStatusCode): List<PlayerStreamResolveStatus> = runBlocking {
        val seen = mutableListOf<PlayerStreamResolveStatus>()
        withContext(ResolveProgress(onStatus = { seen += it })) { client(status).get("https://example.org/") }
        seen
    }

    @Test
    fun `successful response goes fetching then parsing`() {
        assertEquals(
            listOf(PlayerStreamResolveStatus.FetchingPage, PlayerStreamResolveStatus.ParsingPage),
            steps(HttpStatusCode.OK),
        )
    }

    @Test
    fun `error response is reported as response error`() {
        assertEquals(
            listOf(PlayerStreamResolveStatus.FetchingPage, PlayerStreamResolveStatus.ResponseError),
            steps(HttpStatusCode.ServiceUnavailable),
        )
    }

    @Test
    fun `without progress in context nothing is reported and the call still works`() = runBlocking {
        assertEquals(HttpStatusCode.OK, client(HttpStatusCode.OK).get("https://example.org/").status)
    }

    @Test
    fun `detail carries host and http code`() = runBlocking {
        val details = mutableListOf<ResolveDetail>()
        withContext(ResolveProgress(onStatus = {}, onDetail = { details += it })) {
            client(HttpStatusCode.ServiceUnavailable).get("https://example.org/x")
        }
        assertEquals("example.org", details.single().host)
        assertEquals(503, details.single().httpCode)
    }

    @Test
    fun `network failure is reported as detail with error name and rethrown`() = runBlocking {
        val details = mutableListOf<ResolveDetail>()
        val failing = HttpClient(MockEngine { throw java.io.IOException("down") }) { install(ResolveProgressPlugin) }
        val thrown = runCatching {
            withContext(ResolveProgress(onStatus = {}, onDetail = { details += it })) {
                failing.get("https://example.org/")
            }
        }.exceptionOrNull()
        assertEquals("IOException", details.single().error)
        assertEquals(true, thrown is java.io.IOException)
    }

    @Test
    fun `own progress extractors get details but no step changes`() = runBlocking {
        val steps = mutableListOf<PlayerStreamResolveStatus>()
        val details = mutableListOf<ResolveDetail>()
        withContext(ResolveProgress(onStatus = { steps += it }, onDetail = { details += it }, emitsSteps = false)) {
            client(HttpStatusCode.OK).get("https://example.org/")
        }
        assertEquals(emptyList<PlayerStreamResolveStatus>(), steps)
        assertEquals(1, details.size)
    }
}
