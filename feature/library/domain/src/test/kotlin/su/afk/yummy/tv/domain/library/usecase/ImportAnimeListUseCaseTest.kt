package su.afk.yummy.tv.domain.library.usecase

import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.common.coroutines.AppClock
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.repository.AnimeExtrasRepository
import su.afk.yummy.tv.domain.account.repository.UserListsRepository
import su.afk.yummy.tv.domain.library.model.AnimeListImportOptions
import su.afk.yummy.tv.domain.library.model.ExportableLibraryAnime
import su.afk.yummy.tv.domain.library.model.ExternalListEntry
import su.afk.yummy.tv.domain.library.model.ExternalListFormat
import su.afk.yummy.tv.domain.library.model.ImportConflictPolicy
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.domain.library.model.ParsedExternalList
import su.afk.yummy.tv.domain.library.model.ResolvedExternalAnime
import su.afk.yummy.tv.domain.library.repository.AnimeListTransferRepository
import su.afk.yummy.tv.domain.library.repository.LibraryRepository

class ImportAnimeListUseCaseTest : BaseUnitTest() {

    private val transferRepository: AnimeListTransferRepository = mockk(relaxed = true)
    private val libraryRepository: LibraryRepository = mockk(relaxed = true)
    private val userListsRepository: UserListsRepository = mockk(relaxed = true)
    private val animeExtrasRepository: AnimeExtrasRepository = mockk(relaxed = true)
    private val clock: AppClock = object : AppClock {
        override fun nowMillis(): Long = FIXED_NOW_MS
    }

    @Before
    fun setUp() {
        coEvery { transferRepository.currentUserId() } returns USER_ID
        coEvery { libraryRepository.getAll() } returns emptyList()
        coEvery { transferRepository.loadExportableLibrary() } returns emptyList()
    }

    private fun createUseCase() = ImportAnimeListUseCase(
        transferRepository = transferRepository,
        libraryRepository = libraryRepository,
        userListsRepository = userListsRepository,
        animeExtrasRepository = animeExtrasRepository,
        clock = clock,
    )

    @Test
    fun `skip existing adds new items skips existing preserves favorites and tracks unmatched`() = runTest {
        val parsed = ParsedExternalList(
            format = ExternalListFormat.MAL_XML,
            entries = listOf(
                ExternalListEntry(101, "Existing", UserAnimeList.COMPLETED, score = 9, watchedEpisodes = 12),
                ExternalListEntry(102, "Fav Only", UserAnimeList.WATCHING, score = 8, watchedEpisodes = 5),
                ExternalListEntry(103, "Missing", UserAnimeList.PLANNED, score = null, watchedEpisodes = 0),
            ),
        )
        coEvery { transferRepository.resolveByExternalIds(any(), any(), any()) } returns mapOf(
            101 to listOf(ResolvedExternalAnime(101, animeId = 1, title = "Existing")),
            102 to listOf(ResolvedExternalAnime(102, animeId = 2, title = "Fav Only")),
        )
        coEvery { transferRepository.loadExportableLibrary() } returns listOf(
            ExportableLibraryAnime(1, "Existing", UserAnimeList.PLANNED, false, 7, "tv", 101, 101),
            ExportableLibraryAnime(2, "Fav Only", null, true, null, "tv", 102, 102),
        )
        coEvery { libraryRepository.getAll() } returns listOf(
            LibraryItem(
                animeId = 2,
                title = "Fav Only",
                poster = null,
                rating = null,
                userRating = null,
                year = null,
                season = null,
                kind = null,
                addedAt = 1_000L,
                listId = -1,
                isFavorite = true,
            ),
        )

        val result = createUseCase()(
            parsed = parsed,
            options = AnimeListImportOptions(
                conflictPolicy = ImportConflictPolicy.SKIP_EXISTING,
                importScores = true,
                importWatchedEpisodesLocally = true,
            ),
            mutationDelayMs = 0L,
        )

        assertEquals(1, result.addedCount)
        assertEquals(0, result.updatedCount)
        assertEquals(1, result.skippedExistingCount)
        assertEquals(0, result.failedCount)
        assertEquals(listOf(103), result.unmatched.map { it.externalId })
        assertFalse(result.wasCancelled)

        coVerify(exactly = 0) { userListsRepository.setAnimeList(1, any()) }
        coVerify(exactly = 1) { userListsRepository.setAnimeList(2, UserAnimeList.WATCHING) }
        coVerify(exactly = 1) { animeExtrasRepository.setRating(2, 8) }
        coVerify(exactly = 1) {
            libraryRepository.add(
                match {
                    it.animeId == 2 &&
                        it.isFavorite &&
                        it.listId == UserAnimeList.WATCHING.id &&
                        it.userRating == 8
                },
            )
        }
        coVerify(exactly = 1) { transferRepository.markEpisodesWatchedLocally(2, "Fav Only", "", 5) }
        coVerify(exactly = 1) { libraryRepository.markSynced(USER_ID) }
    }

    @Test
    fun `overwrite mutates only changed status or rating deltas`() = runTest {
        val parsed = ParsedExternalList(
            format = ExternalListFormat.SHIKIMORI_JSON,
            entries = listOf(
                ExternalListEntry(201, "Unchanged", UserAnimeList.COMPLETED, score = 10, watchedEpisodes = 24),
                ExternalListEntry(202, "Rating Only", UserAnimeList.COMPLETED, score = 9, watchedEpisodes = 12),
                ExternalListEntry(203, "Status Only", UserAnimeList.DROPPED, score = 6, watchedEpisodes = 3),
            ),
        )
        coEvery { transferRepository.resolveByExternalIds(any(), any(), any()) } returns mapOf(
            201 to listOf(ResolvedExternalAnime(201, animeId = 11, title = "Unchanged")),
            202 to listOf(ResolvedExternalAnime(202, animeId = 12, title = "Rating Only")),
            203 to listOf(ResolvedExternalAnime(203, animeId = 13, title = "Status Only")),
        )
        coEvery { transferRepository.loadExportableLibrary() } returns listOf(
            ExportableLibraryAnime(11, "Unchanged", UserAnimeList.COMPLETED, false, 10, "tv", 201, 201),
            ExportableLibraryAnime(12, "Rating Only", UserAnimeList.COMPLETED, false, 7, "tv", 202, 202),
            ExportableLibraryAnime(13, "Status Only", UserAnimeList.WATCHING, false, 6, "tv", 203, 203),
        )

        val result = createUseCase()(
            parsed = parsed,
            options = AnimeListImportOptions(
                conflictPolicy = ImportConflictPolicy.OVERWRITE,
                importScores = true,
                importWatchedEpisodesLocally = true,
            ),
            mutationDelayMs = 0L,
        )

        assertEquals(0, result.addedCount)
        assertEquals(2, result.updatedCount)
        assertEquals(1, result.skippedExistingCount)

        coVerify(exactly = 0) { userListsRepository.setAnimeList(11, any()) }
        coVerify(exactly = 0) { animeExtrasRepository.setRating(11, any()) }
        coVerify(exactly = 0) { userListsRepository.setAnimeList(12, any()) }
        coVerify(exactly = 1) { animeExtrasRepository.setRating(12, 9) }
        coVerify(exactly = 1) { userListsRepository.setAnimeList(13, UserAnimeList.DROPPED) }
        coVerify(exactly = 0) { animeExtrasRepository.setRating(13, any()) }
        // Completed titles never get synthetic local episode rows, while Dropped does.
        coVerify(exactly = 0) { transferRepository.markEpisodesWatchedLocally(12, any(), any(), any()) }
        coVerify(exactly = 1) { transferRepository.markEpisodesWatchedLocally(13, "Status Only", "", 3) }
    }

    @Test
    fun `retries on HTTP 429 with Retry-After and succeeds`() = runTest {
        val parsed = ParsedExternalList(
            format = ExternalListFormat.MAL_XML,
            entries = listOf(
                ExternalListEntry(301, "Rate Limited", UserAnimeList.PLANNED, score = null, watchedEpisodes = 0),
            ),
        )
        coEvery { transferRepository.resolveByExternalIds(any(), any(), any()) } returns mapOf(
            301 to listOf(ResolvedExternalAnime(301, animeId = 31, title = "Rate Limited")),
        )
        var attempts = 0
        coEvery { userListsRepository.setAnimeList(31, UserAnimeList.PLANNED) } answers {
            attempts++
            if (attempts == 1) {
                val response = mockk<HttpResponse> {
                    every { status } returns HttpStatusCode.TooManyRequests
                    every { headers } returns Headers.build { append(HttpHeaders.RetryAfter, "1") }
                }
                throw ResponseException(response, "Slow down")
            }
        }

        val result = createUseCase()(
            parsed = parsed,
            options = AnimeListImportOptions(),
            mutationDelayMs = 0L,
        )

        assertEquals(2, attempts)
        assertEquals(1, result.addedCount)
        assertEquals(0, result.failedCount)
    }

    @Test
    fun `cancellation stops gracefully with partial counts and guest mode skips remote mutations`() = runTest {
        coEvery { transferRepository.currentUserId() } returns 0
        val parsed = ParsedExternalList(
            format = ExternalListFormat.MAL_XML,
            entries = listOf(
                ExternalListEntry(401, "First", UserAnimeList.WATCHING, score = 8, watchedEpisodes = 2),
                ExternalListEntry(402, "Second", UserAnimeList.PLANNED, score = null, watchedEpisodes = 0),
            ),
        )
        coEvery { transferRepository.resolveByExternalIds(any(), any(), any()) } returns mapOf(
            401 to listOf(ResolvedExternalAnime(401, animeId = 41, title = "First")),
            402 to listOf(ResolvedExternalAnime(402, animeId = 42, title = "Second")),
        )

        var appliedItems = 0
        val result = createUseCase()(
            parsed = parsed,
            options = AnimeListImportOptions(),
            mutationDelayMs = 0L,
            isCancelled = { appliedItems >= 1 },
            onProgress = { progress ->
                if (progress.phase == su.afk.yummy.tv.domain.library.model.AnimeListImportPhase.APPLYING_CHANGES) {
                    appliedItems = progress.processed
                }
            },
        )

        assertTrue(result.wasCancelled)
        assertEquals(1, result.addedCount)
        coVerify(exactly = 0) { userListsRepository.setAnimeList(any(), any()) }
        coVerify(exactly = 0) { animeExtrasRepository.setRating(any(), any()) }
        coVerify(exactly = 1) { libraryRepository.add(match { it.animeId == 41 }) }
        coVerify(exactly = 0) { libraryRepository.add(match { it.animeId == 42 }) }
    }

    private companion object {
        const val USER_ID = 77
        const val FIXED_NOW_MS = 1_700_000_000_000L
    }
}
