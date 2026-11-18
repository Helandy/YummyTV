package su.afk.yummy.tv.feature.library.transfer

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.testing.collectEmissions
import su.afk.yummy.tv.domain.account.model.AccountSession
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.repository.AccountRepository
import su.afk.yummy.tv.domain.account.usecase.ObserveAccountSessionUseCase
import su.afk.yummy.tv.domain.library.model.AnimeListExportResult
import su.afk.yummy.tv.domain.library.model.AnimeListImportResult
import su.afk.yummy.tv.domain.library.model.ExternalListEntry
import su.afk.yummy.tv.domain.library.model.ExternalListFormat
import su.afk.yummy.tv.domain.library.model.ExternalListParseError
import su.afk.yummy.tv.domain.library.model.ExternalListParseException
import su.afk.yummy.tv.domain.library.model.ImportConflictPolicy
import su.afk.yummy.tv.domain.library.model.ParsedExternalList
import su.afk.yummy.tv.domain.library.model.UnmatchedExternalAnime
import su.afk.yummy.tv.domain.library.repository.AnimeListTransferRepository
import su.afk.yummy.tv.domain.library.usecase.ExportAnimeListUseCase
import su.afk.yummy.tv.domain.library.usecase.ImportAnimeListUseCase
import su.afk.yummy.tv.domain.library.usecase.ParseExternalAnimeListUseCase
import su.afk.yummy.tv.domain.library.usecase.SaveExportedAnimeListUseCase
import su.afk.yummy.tv.domain.library.usecase.ShareExportedAnimeListUseCase
import su.afk.yummy.tv.feature.library.presentation.R
import su.afk.yummy.tv.feature.library.transfer.ListTransferState.Effect
import su.afk.yummy.tv.feature.library.transfer.ListTransferState.Event

class ListTransferViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val navigator: INavigationManager = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk()
    private val transferRepository: AnimeListTransferRepository = mockk(relaxed = true)
    private val importAnimeList: ImportAnimeListUseCase = mockk()
    private val exportAnimeList: ExportAnimeListUseCase = mockk()
    private val analytics: ListTransferAnalytics = mockk(relaxed = true)

    private val sessionFlow = MutableStateFlow(AccountSession(isAuthorized = true, userId = 42))

    @Before
    fun setUp() {
        every { accountRepository.observeSession() } returns sessionFlow
    }

    private fun createViewModel() = ListTransferViewModel(
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        navigator = navigator,
        observeAccountSession = ObserveAccountSessionUseCase(accountRepository),
        parseExternalAnimeList = ParseExternalAnimeListUseCase(transferRepository),
        importAnimeList = importAnimeList,
        exportAnimeList = exportAnimeList,
        saveExportedAnimeList = SaveExportedAnimeListUseCase(transferRepository),
        shareExportedAnimeList = ShareExportedAnimeListUseCase(transferRepository),
        analytics = analytics,
    )

    @Test
    fun `parsing file populates preview and running import records summary and unmatched toggle`() {
        val parsed = ParsedExternalList(
            format = ExternalListFormat.MAL_XML,
            entries = listOf(
                ExternalListEntry(1, "Frieren", UserAnimeList.COMPLETED, score = 10, watchedEpisodes = 28),
                ExternalListEntry(2, "Missing OVA", UserAnimeList.PLANNED, score = null, watchedEpisodes = 0),
            ),
            skippedNonAnimeCount = 1,
        )
        coEvery { transferRepository.parseExternalListUri("content://list.xml") } returns parsed
        coEvery { importAnimeList(parsed, any(), any(), any(), any()) } returns AnimeListImportResult(
            addedCount = 1,
            updatedCount = 0,
            skippedExistingCount = 0,
            failedCount = 0,
            unmatched = listOf(UnmatchedExternalAnime(2, "Missing OVA", UserAnimeList.PLANNED)),
            wasCancelled = false,
        )

        val vm = createViewModel()
        assertTrue(vm.currentState.isSignedIn)
        verify(exactly = 1) { analytics.eventScreenOpened() }

        vm.setEvent(Event.ImportFileSelected("content://list.xml"))
        val preview = vm.currentState.importPreview
        assertNotNull(preview)
        assertEquals(2, preview!!.totalCount)
        assertEquals(1, preview.ratedCount)
        assertEquals(1, preview.withWatchedEpisodesCount)
        assertEquals(1, preview.skippedNonAnimeCount)

        vm.setEvent(Event.ConflictPolicySelected(ImportConflictPolicy.OVERWRITE))
        vm.setEvent(Event.ImportScoresChanged(false))
        vm.setEvent(Event.StartImportClicked)

        coVerify(exactly = 1) {
            importAnimeList(
                parsed = parsed,
                options = match {
                    it.conflictPolicy == ImportConflictPolicy.OVERWRITE && !it.importScores
                },
                mutationDelayMs = any(),
                isCancelled = any(),
                onProgress = any(),
            )
        }
        val summary = vm.currentState.importSummary
        assertNotNull(summary)
        assertEquals(1, summary!!.addedCount)
        assertEquals(1, summary.unmatched.size)
        assertFalse(vm.currentState.showUnmatchedDetails)

        vm.setEvent(Event.ToggleUnmatchedDetails)
        assertTrue(vm.currentState.showUnmatchedDetails)
    }

    @Test
    fun `typed parse error sets localized message and dismiss clears it`() {
        coEvery { transferRepository.parseExternalListUri("content://bad") } throws
            ExternalListParseException(ExternalListParseError.UNSUPPORTED_FORMAT)

        val vm = createViewModel()
        vm.setEvent(Event.ImportFileSelected("content://bad"))

        assertNull(vm.currentState.importPreview)
        assertEquals(
            UiText.Res(R.string.library_transfer_error_unsupported_format),
            vm.currentState.errorMessage,
        )

        vm.setEvent(Event.DismissError)
        assertNull(vm.currentState.errorMessage)
    }

    @Test
    fun `export save writes via SAF and falls back to storage when saver is unavailable`() = runTest {
        val exportResult = AnimeListExportResult(
            format = ExternalListFormat.SHIKIMORI_JSON,
            content = "[]",
            suggestedFileName = "yummytv-shikimori-animes-20260516.json",
            exportedCount = 15,
            skippedWithoutExternalIdCount = 1,
            skippedFavoriteOnlyCount = 2,
        )
        coEvery { exportAnimeList(ExternalListFormat.SHIKIMORI_JSON) } returns exportResult
        coEvery { transferRepository.saveExportToStorage(any(), any(), any()) } returns
            "Download/yummytv-shikimori-animes-20260516.json"

        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.ExportFormatSelected(ExternalListFormat.SHIKIMORI_JSON))
        vm.setEvent(Event.ExportSaveClicked)
        assertEquals(
            Effect.OpenExportFileSaver(
                fileName = "yummytv-shikimori-animes-20260516.json",
                mimeType = "application/json",
            ),
            effects.last(),
        )

        vm.setEvent(Event.ExportDestinationSelected("content://saved.json"))
        coVerify(exactly = 1) { transferRepository.writeExportToUri("content://saved.json", "[]") }
        assertEquals(15, vm.currentState.exportSummary?.exportedCount)
        assertEquals(Effect.ShowExportSavedToast(), effects.last())

        // Fallback branch when Android TV firmware lacks ACTION_CREATE_DOCUMENT.
        vm.setEvent(Event.ExportSaveClicked)
        vm.setEvent(Event.ExportSaverUnavailable)
        assertEquals(
            "Download/yummytv-shikimori-animes-20260516.json",
            vm.currentState.exportSummary?.savedPath,
        )
    }

    @Test
    fun `export share emits ShareExportFile and back navigates back`() = runTest {
        val exportResult = AnimeListExportResult(
            format = ExternalListFormat.MAL_XML,
            content = "<myanimelist/>",
            suggestedFileName = "yummytv-animelist-20260516.xml",
            exportedCount = 4,
            skippedWithoutExternalIdCount = 0,
            skippedFavoriteOnlyCount = 0,
        )
        coEvery { exportAnimeList(ExternalListFormat.MAL_XML) } returns exportResult
        coEvery { transferRepository.createShareUri(any(), any()) } returns "content://share/export.xml"

        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.PickImportFileClicked)
        assertEquals(Effect.OpenImportFilePicker, effects.last())

        vm.setEvent(Event.ExportShareClicked)
        assertEquals(
            Effect.ShareExportFile(
                uriString = "content://share/export.xml",
                mimeType = "application/xml",
            ),
            effects.last(),
        )

        vm.setEvent(Event.BackClicked)
        verify(exactly = 1) { navigator.back() }
    }
}
