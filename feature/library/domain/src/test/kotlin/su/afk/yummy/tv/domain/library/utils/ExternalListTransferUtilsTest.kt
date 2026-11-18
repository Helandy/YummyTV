package su.afk.yummy.tv.domain.library.utils

import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.library.model.ExternalListFormat
import su.afk.yummy.tv.domain.library.model.ResolvedExternalAnime

class ExternalListTransferUtilsTest : BaseUnitTest() {

    @Test
    fun `selectPrimaryCandidate prefers existing library id then non directors cut then lowest id`() {
        val directorsCut = ResolvedExternalAnime(
            externalId = 31240,
            animeId = 100,
            title = "Re:Zero (режиссёрская версия)",
        )
        val regularNewerId = ResolvedExternalAnime(
            externalId = 31240,
            animeId = 250,
            title = "Re:Zero",
        )
        val regularLowestId = ResolvedExternalAnime(
            externalId = 31240,
            animeId = 150,
            title = "Re:Zero",
        )
        val candidates = listOf(directorsCut, regularNewerId, regularLowestId)

        assertEquals(150, candidates.selectPrimaryCandidate(existingAnimeIds = emptySet())?.animeId)
        assertEquals(100, candidates.selectPrimaryCandidate(existingAnimeIds = setOf(100))?.animeId)
    }

    @Test
    fun `resolveExportedWatchedEpisodes respects planned completed movie and local counts`() {
        assertEquals(0, resolveExportedWatchedEpisodes(UserAnimeList.COMPLETED, "tv", localWatchedCount = 0))
        assertEquals(24, resolveExportedWatchedEpisodes(UserAnimeList.COMPLETED, "tv", localWatchedCount = 24))
        assertEquals(0, resolveExportedWatchedEpisodes(UserAnimeList.PLANNED, "tv", localWatchedCount = 5))
        assertEquals(7, resolveExportedWatchedEpisodes(UserAnimeList.WATCHING, "tv", localWatchedCount = 7))
        assertEquals(1, resolveExportedWatchedEpisodes(UserAnimeList.COMPLETED, "movie", localWatchedCount = 0))
    }

    @Test
    fun `retry delay uses Retry-After header when present and exponential backoff otherwise`() {
        val rateLimitedWithHeader = httpError(HttpStatusCode.TooManyRequests, retryAfter = "7")
        assertEquals(7_000L, rateLimitedWithHeader.computeImportRetryDelayMs(attempt = 0))

        val rateLimitedWithoutHeader = httpError(HttpStatusCode.TooManyRequests)
        assertEquals(2_500L, rateLimitedWithoutHeader.computeImportRetryDelayMs(attempt = 0))
        assertEquals(5_000L, rateLimitedWithoutHeader.computeImportRetryDelayMs(attempt = 1))

        val serverError = httpError(HttpStatusCode.BadGateway)
        assertEquals(1_000L, serverError.computeImportRetryDelayMs(attempt = 0))
    }

    @Test
    fun `buildExportFileName formats utc date for both formats`() {
        val epochMs = 1_778_889_600_000L // 2026-05-16T00:00:00Z
        assertEquals(
            "yummytv-animelist-20260516.xml",
            buildExportFileName(ExternalListFormat.MAL_XML, epochMs),
        )
        assertEquals(
            "yummytv-shikimori-animes-20260516.json",
            buildExportFileName(ExternalListFormat.SHIKIMORI_JSON, epochMs),
        )
    }

    private fun httpError(status: HttpStatusCode, retryAfter: String? = null): ResponseException {
        val response = mockk<HttpResponse> {
            every { this@mockk.status } returns status
            every { headers } returns if (retryAfter != null) {
                Headers.build { append(HttpHeaders.RetryAfter, retryAfter) }
            } else {
                Headers.Empty
            }
        }
        return ResponseException(response, "cached")
    }
}
