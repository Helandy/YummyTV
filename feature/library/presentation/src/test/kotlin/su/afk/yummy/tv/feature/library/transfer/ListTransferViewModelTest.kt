package su.afk.yummy.tv.feature.library.transfer

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
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
import su.afk.yummy.tv.domain.library.model.AnimeListTransferStage
import su.afk.yummy.tv.domain.library.model.AnimeListTransferStageException
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
import java.io.IOException

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
        coEvery { importAnimeList(parsed, any(), any(), any(), any(), any()) } returns AnimeListImportResult(
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
                pendingScoreExternalIds = emptySet(),
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
    fun `back during active import shows confirmation dialog and keeps screen awake until confirmed`() {
        val parsed = ParsedExternalList(
            format = ExternalListFormat.MAL_XML,
            entries = listOf(ExternalListEntry(1, "Frieren", UserAnimeList.WATCHING, score = 10, watchedEpisodes = 5)),
        )
        val blocker = CompletableDeferred<AnimeListImportResult>()
        coEvery { transferRepository.parseExternalListUri("content://list.xml") } returns parsed
        coEvery { importAnimeList(parsed, any(), any(), any(), any(), any()) } coAnswers { blocker.await() }

        val vm = createViewModel()
        vm.setEvent(Event.ImportFileSelected("content://list.xml"))
        vm.setEvent(Event.StartImportClicked)

        assertTrue(vm.currentState.isImporting)
        assertTrue(vm.currentState.keepScreenOn)

        vm.setEvent(Event.BackClicked)
        assertTrue(vm.currentState.showExitImportDialog)
        verify(exactly = 0) { navigator.back() }

        vm.setEvent(Event.DismissExitImportDialog)
        assertFalse(vm.currentState.showExitImportDialog)
        assertTrue(vm.currentState.isImporting)

        vm.setEvent(Event.BackClicked)
        vm.setEvent(Event.ConfirmExitImportClicked)
        assertFalse(vm.currentState.showExitImportDialog)
        assertFalse(vm.currentState.isImporting)
        assertFalse(vm.currentState.keepScreenOn)
        verify(exactly = 1) { navigator.back() }
    }

    @Test
    fun `stage error supports retry and partial import failures support targeted retry`() {
        val first = ExternalListEntry(1, "Ok", UserAnimeList.COMPLETED, score = 9, watchedEpisodes = 12)
        val second = ExternalListEntry(2, "Failed", UserAnimeList.PLANNED, score = 8, watchedEpisodes = 0)
        val parsed = ParsedExternalList(format = ExternalListFormat.MAL_XML, entries = listOf(first, second))
        coEvery { transferRepository.parseExternalListUri("content://list.xml") } returns parsed

        var exportAttempts = 0
        coEvery { exportAnimeList(ExternalListFormat.MAL_XML) } answers {
            exportAttempts++
            if (exportAttempts == 1) {
                throw AnimeListTransferStageException(
                    stage = AnimeListTransferStage.ENRICH_GUEST_LIBRARY,
                    cause = IOException("network lost"),
                    completedBatches = 1,
                    totalBatches = 3,
                )
            }
            AnimeListExportResult(
                format = ExternalListFormat.MAL_XML,
                content = "<xml/>",
                suggestedFileName = "yummytv-animelist-20260516-143045.xml",
                exportedCount = 10,
                skippedWithoutExternalIdCount = 0,
                skippedFavoriteOnlyCount = 0,
            )
        }

        val vm = createViewModel()
        vm.setEvent(Event.ExportSaveClicked)
        assertEquals(
            UiText.Res(R.string.library_transfer_error_stage_guest_ids, listOf(1, 3)),
            vm.currentState.errorContext,
        )
        assertTrue(vm.currentState.canRetry)

        vm.setEvent(Event.RetryLastActionClicked)
        assertEquals(2, exportAttempts)
        assertNull(vm.currentState.errorMessage)
        assertFalse(vm.currentState.canRetry)

        coEvery { importAnimeList(parsed, any(), any(), emptySet(), any(), any()) } returns AnimeListImportResult(
            addedCount = 1,
            updatedCount = 0,
            skippedExistingCount = 0,
            unmatched = emptyList(),
            failedCount = 1,
            lookupFailedCount = 1,
            failedEntries = listOf(second),
            pendingScoreExternalIds = setOf(2),
        )
        val failedOnlyList = ParsedExternalList(format = ExternalListFormat.MAL_XML, entries = listOf(second))
        coEvery { importAnimeList(failedOnlyList, any(), any(), setOf(2), any(), any()) } returns AnimeListImportResult(
            addedCount = 1,
            updatedCount = 0,
            skippedExistingCount = 0,
            unmatched = emptyList(),
            failedCount = 0,
        )

        vm.setEvent(Event.ImportFileSelected("content://list.xml"))
        vm.setEvent(Event.StartImportClicked)
        assertEquals(1, vm.currentState.importSummary?.lookupFailedCount)
        assertEquals(1, vm.currentState.importSummary?.retryableCount)

        vm.setEvent(Event.RetryFailedImportClicked)
        assertEquals(0, vm.currentState.importSummary?.failedCount)
        assertEquals(0, vm.currentState.importSummary?.retryableCount)
    }

    @Test
    fun `typed parse error sets localized message without retry and dismiss clears it`() {
        coEvery { transferRepository.parseExternalListUri("content://bad") } throws
            ExternalListParseException(ExternalListParseError.UNSUPPORTED_FORMAT)

        val vm = createViewModel()
        vm.setEvent(Event.ImportFileSelected("content://bad"))

        assertNull(vm.currentState.importPreview)
        assertFalse(vm.currentState.canRetry)
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
            suggestedFileName = "yummytv-shikimori-animes-20260516-143045.json",
            exportedCount = 15,
            skippedWithoutExternalIdCount = 1,
            skippedFavoriteOnlyCount = 2,
        )
        coEvery { exportAnimeList(ExternalListFormat.SHIKIMORI_JSON) } returns exportResult
        coEvery { transferRepository.saveExportToStorage(any(), any(), any()) } returns
            "Download/yummytv-shikimori-animes-20260516-143045.json"

        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.ExportFormatSelected(ExternalListFormat.SHIKIMORI_JSON))
        vm.setEvent(Event.ExportSaveClicked)
        assertEquals(
            Effect.OpenExportFileSaver(
                fileName = "yummytv-shikimori-animes-20260516-143045.json",
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
            "Download/yummytv-shikimori-animes-20260516-143045.json",
            vm.currentState.exportSummary?.savedPath,
        )
    }

    @Test
    fun `export share emits ShareExportFile and back navigates back`() = runTest {
        val exportResult = AnimeListExportResult(
            format = ExternalListFormat.MAL_XML,
            content = "<myanimelist/>",
            suggestedFileName = "yummytv-animelist-20260516-143045.xml",
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
