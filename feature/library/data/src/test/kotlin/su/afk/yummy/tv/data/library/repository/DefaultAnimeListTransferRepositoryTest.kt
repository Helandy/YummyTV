package su.afk.yummy.tv.data.library.repository

import android.content.Context
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.common.coroutines.AppClock
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.core.storage.library.LibraryEntry
import su.afk.yummy.tv.core.storage.library.LibraryStorage
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStorage
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.data.library.dto.YaniTransferListInfoDto
import su.afk.yummy.tv.data.library.dto.YaniTransferLookupAnimeDto
import su.afk.yummy.tv.data.library.dto.YaniTransferRemoteIdsDto
import su.afk.yummy.tv.data.library.dto.YaniTransferTypeDto
import su.afk.yummy.tv.data.library.dto.YaniTransferUserAnimeDto
import su.afk.yummy.tv.data.library.dto.YaniTransferUserListDto
import su.afk.yummy.tv.data.library.dto.YaniTransferUserStateDto
import su.afk.yummy.tv.data.library.network.YaniListTransferApi
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.library.model.ExternalListFormat

class DefaultAnimeListTransferRepositoryTest : BaseUnitTest() {

    private val context: Context = mockk(relaxed = true)
    private val api: YaniListTransferApi = mockk()
    private val settingsStore: YaniAccountSettingsStore = mockk()
    private val libraryStorage: LibraryStorage = mockk(relaxed = true)
    private val watchProgressStorage: WatchProgressStorage = mockk(relaxed = true)
    private val clock: AppClock = object : AppClock {
        override fun nowMillis(): Long = 123_456L
    }

    @Before
    fun setUp() {
        every { settingsStore.yaniUserId } returns flowOf(42)
        every { settingsStore.yaniNickname } returns flowOf("user42")
    }

    private fun createRepository() = DefaultAnimeListTransferRepository(
        context = context,
        api = api,
        settingsStore = settingsStore,
        libraryStorage = libraryStorage,
        watchProgressStorage = watchProgressStorage,
        clock = clock,
    )

    @Test
    fun `resolveByExternalIds batches by 50 paginates full pages and keeps duplicate candidates`() = runTest {
        val requestedIds = (1..60).toList()
        val firstChunk = (1..50).toList()
        val secondChunk = (51..60).toList()

        // First page returns 100 items (triggering offset=100 pagination).
        val firstPage = (1..50).flatMap { extId ->
            listOf(
                lookupDto(animeId = extId * 10, malId = extId, title = "Title $extId"),
                lookupDto(animeId = extId * 10 + 1, malId = extId, title = "Title $extId (режиссёрская версия)"),
            )
        }
        coEvery { api.lookupByMalIds(firstChunk, limit = 100, offset = 0) } returns firstPage
        coEvery { api.lookupByMalIds(firstChunk, limit = 100, offset = 100) } returns emptyList()
        coEvery { api.lookupByMalIds(secondChunk, limit = 100, offset = 0) } returns listOf(
            lookupDto(animeId = 510, malId = 51, title = "Title 51"),
        )

        val progressUpdates = mutableListOf<Pair<Int, Int>>()
        val resolved = createRepository().resolveByExternalIds(
            format = ExternalListFormat.MAL_XML,
            externalIds = requestedIds,
            onBatchProgress = { done, total -> progressUpdates += done to total },
        )

        assertEquals(listOf(50 to 60, 60 to 60), progressUpdates)
        assertEquals(51, resolved.size)
        assertEquals(listOf(10, 11), resolved.getValue(1).map { it.animeId })
        assertEquals(listOf(510), resolved.getValue(51).map { it.animeId })
    }

    @Test
    fun `loadExportableLibrary uses single getAllUserLists for signed in and batched ids for guest`() = runTest {
        coEvery { api.getAllUserLists(42) } returns listOf(
            YaniTransferUserAnimeDto(
                animeId = 7,
                title = "Signed In Anime",
                type = YaniTransferTypeDto(alias = "tv"),
                remoteIds = YaniTransferRemoteIdsDto(shikimoriId = 700, myAnimeListId = 700),
                user = YaniTransferUserStateDto(
                    list = YaniTransferUserListDto(
                        isFav = true,
                        list = YaniTransferListInfoDto(id = UserAnimeList.COMPLETED.id),
                    ),
                    rating = 9,
                ),
            ),
        )

        val signedInItems = createRepository().loadExportableLibrary()
        assertEquals(1, signedInItems.size)
        assertEquals(700, signedInItems.single().shikimoriId)
        assertEquals(9, signedInItems.single().userRating)
        coVerify(exactly = 0) { api.lookupByAnimeIds(any(), any(), any()) }

        every { settingsStore.yaniUserId } returns flowOf(0)
        coEvery { libraryStorage.getAll() } returns listOf(
            LibraryEntry(animeId = 8, title = "Guest Anime", listId = UserAnimeList.PLANNED.id, userRating = 8),
        )
        coEvery { api.lookupByAnimeIds(listOf(8), limit = 100, offset = 0) } returns listOf(
            lookupDto(animeId = 8, malId = 800, title = "Guest Anime"),
        )

        val guestItems = createRepository().loadExportableLibrary()
        assertEquals(1, guestItems.size)
        assertEquals(800, guestItems.single().myAnimeListId)
        assertEquals(8, guestItems.single().userRating)
    }

    @Test
    fun `markEpisodesWatchedLocally saves zero videoId rows without overwriting existing progress`() = runTest {
        coEvery { watchProgressStorage.get(10, "1") } returns WatchProgressEntry(
            animeId = 10,
            episode = "1",
            videoId = 555,
            episodeUrl = "https://example",
            positionMs = 500_000L,
            durationMs = 1_400_000L,
            updatedAt = 1L,
        )
        coEvery { watchProgressStorage.get(10, "2") } returns null
        coEvery { watchProgressStorage.get(10, "3") } returns null

        createRepository().markEpisodesWatchedLocally(
            animeId = 10,
            animeTitle = "Test",
            posterUrl = "",
            watchedEpisodes = 3,
        )

        coVerify(exactly = 0) {
            watchProgressStorage.save(10, "1", any(), any(), any(), any(), any(), any(), any())
        }
        coVerify(exactly = 1) {
            watchProgressStorage.save(10, "2", 0, "", any(), any(), 123_456L, "Test", "")
        }
        coVerify(exactly = 1) {
            watchProgressStorage.save(10, "3", 0, "", any(), any(), 123_456L, "Test", "")
        }
    }

    private fun lookupDto(animeId: Int, malId: Int, title: String) = YaniTransferLookupAnimeDto(
        animeId = animeId,
        title = title,
        type = YaniTransferTypeDto(name = "Сериал", shortName = "ТВ", alias = "tv"),
        remoteIds = YaniTransferRemoteIdsDto(shikimoriId = malId, myAnimeListId = malId),
    )
}
