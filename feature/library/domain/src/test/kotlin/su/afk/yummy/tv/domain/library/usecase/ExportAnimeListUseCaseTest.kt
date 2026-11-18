package su.afk.yummy.tv.domain.library.usecase

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.common.coroutines.AppClock
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.library.model.EmptyExportListException
import su.afk.yummy.tv.domain.library.model.ExportableLibraryAnime
import su.afk.yummy.tv.domain.library.model.ExternalListEntry
import su.afk.yummy.tv.domain.library.model.ExternalListFormat
import su.afk.yummy.tv.domain.library.repository.AnimeListTransferRepository

class ExportAnimeListUseCaseTest : BaseUnitTest() {

    private val repository: AnimeListTransferRepository = mockk()
    private val clock: AppClock = object : AppClock {
        override fun nowMillis(): Long = FIXED_NOW_MS
    }

    @Before
    fun setUp() {
        coEvery { repository.currentNickname() } returns "helandy"
        coEvery { repository.loadLocalWatchedEpisodeCounts() } returns mapOf(1 to 12)
        every { repository.formatExternalList(any(), any(), any()) } returns "<exported/>"
    }

    @Test
    fun `exports library items counts skipped entries without cross fallback and attaches local watched counts`() =
        runTest {
            val capturedEntries = slot<List<ExternalListEntry>>()
            every {
                repository.formatExternalList(
                    format = ExternalListFormat.MAL_XML,
                    userName = "helandy",
                    entries = capture(capturedEntries),
                )
            } returns "<xml/>"

            coEvery { repository.loadExportableLibrary() } returns listOf(
                ExportableLibraryAnime(
                    animeId = 1,
                    title = "Frieren",
                    list = UserAnimeList.WATCHING,
                    isFavorite = true,
                    userRating = 10,
                    typeAlias = "tv",
                    shikimoriId = 52991,
                    myAnimeListId = 52991,
                ),
                ExportableLibraryAnime(
                    animeId = 2,
                    title = "Your Name",
                    list = UserAnimeList.COMPLETED,
                    isFavorite = false,
                    userRating = 9,
                    typeAlias = "movie",
                    shikimoriId = null,
                    myAnimeListId = 32281,
                ),
                ExportableLibraryAnime(
                    animeId = 3,
                    title = "Favorite only",
                    list = null,
                    isFavorite = true,
                    userRating = null,
                    typeAlias = "tv",
                    shikimoriId = 999,
                    myAnimeListId = 999,
                ),
                ExportableLibraryAnime(
                    animeId = 4,
                    title = "Shiki ID only without MAL ID",
                    list = UserAnimeList.PLANNED,
                    isFavorite = false,
                    userRating = null,
                    typeAlias = "ona",
                    shikimoriId = 77777,
                    myAnimeListId = null,
                ),
            )

            val result = ExportAnimeListUseCase(repository, clock)(ExternalListFormat.MAL_XML)

            assertEquals(2, result.exportedCount)
            assertEquals(1, result.skippedFavoriteOnlyCount)
            assertEquals(1, result.skippedWithoutExternalIdCount)
            assertEquals("application/xml", result.format.mimeType)
            assertEquals("yummytv-animelist-20260516-000000.xml", result.suggestedFileName)

            val entries = capturedEntries.captured
            assertEquals(2, entries.size)
            assertEquals(52991, entries[0].externalId)
            assertEquals(12, entries[0].watchedEpisodes)
            assertEquals(10, entries[0].score)
            assertEquals("TV", entries[0].seriesType)

            assertEquals(32281, entries[1].externalId)
            assertEquals(1, entries[1].watchedEpisodes)
            assertEquals("Movie", entries[1].seriesType)
        }

    @Test
    fun `throws EmptyExportListException when nothing can be exported`() = runTest {
        coEvery { repository.loadExportableLibrary() } returns emptyList()

        try {
            ExportAnimeListUseCase(repository, clock)(ExternalListFormat.SHIKIMORI_JSON)
            fail("Expected EmptyExportListException")
        } catch (_: EmptyExportListException) {
            // expected
        }
    }

    private companion object {
        const val FIXED_NOW_MS = 1_778_889_600_000L
    }
}
