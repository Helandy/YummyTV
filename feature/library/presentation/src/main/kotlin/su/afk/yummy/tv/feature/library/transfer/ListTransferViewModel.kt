package su.afk.yummy.tv.feature.library.transfer

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.domain.account.usecase.ObserveAccountSessionUseCase
import su.afk.yummy.tv.domain.library.model.AnimeListExportResult
import su.afk.yummy.tv.domain.library.model.AnimeListImportPhase
import su.afk.yummy.tv.domain.library.model.AnimeListImportProgress
import su.afk.yummy.tv.domain.library.model.ParsedExternalList
import su.afk.yummy.tv.domain.library.usecase.ExportAnimeListUseCase
import su.afk.yummy.tv.domain.library.usecase.ImportAnimeListUseCase
import su.afk.yummy.tv.domain.library.usecase.ParseExternalAnimeListUseCase
import su.afk.yummy.tv.domain.library.usecase.SaveExportedAnimeListUseCase
import su.afk.yummy.tv.domain.library.usecase.ShareExportedAnimeListUseCase
import su.afk.yummy.tv.feature.library.mapper.toPreviewUi
import su.afk.yummy.tv.feature.library.mapper.toSummaryUi
import su.afk.yummy.tv.feature.library.model.ListTransferExportAction
import su.afk.yummy.tv.feature.library.presentation.R
import su.afk.yummy.tv.feature.library.utils.ListTransferErrorUi
import su.afk.yummy.tv.feature.library.utils.toExportErrorUi
import su.afk.yummy.tv.feature.library.utils.toImportErrorUi
import su.afk.yummy.tv.feature.library.utils.toParseErrorUi
import su.afk.yummy.tv.feature.library.utils.toSaveErrorUi
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class ListTransferViewModel @Inject internal constructor(
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val navigator: INavigationManager,
    private val observeAccountSession: ObserveAccountSessionUseCase,
    private val parseExternalAnimeList: ParseExternalAnimeListUseCase,
    private val importAnimeList: ImportAnimeListUseCase,
    private val exportAnimeList: ExportAnimeListUseCase,
    private val saveExportedAnimeList: SaveExportedAnimeListUseCase,
    private val shareExportedAnimeList: ShareExportedAnimeListUseCase,
    private val analytics: ListTransferAnalytics,
) : BaseViewModel<ListTransferState.State, ListTransferState.Event, ListTransferState.Effect>() {

    private var parsedImportList: ParsedExternalList? = null
    private var failedImportList: ParsedExternalList? = null
    private var pendingScoreExternalIds: Set<Int> = emptySet()
    private var pendingExport: AnimeListExportResult? = null
    private var lastRetryAction: RetryAction? = null
    private var importJob: Job? = null
    private val cancelImportRequested = AtomicBoolean(false)

    override fun createInitialState(): ListTransferState.State = ListTransferState.State()

    init {
        analytics.eventScreenOpened()
        observeAccountSession()
            .onEach { session ->
                setState { copy(isSignedIn = session.isAuthorized && session.userId > 0) }
            }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: ListTransferState.Event) {
        when (event) {
            ListTransferState.Event.BackClicked -> onBackClicked()
            ListTransferState.Event.DismissExitImportDialog -> {
                setState { copy(showExitImportDialog = false) }
            }
            ListTransferState.Event.ConfirmExitImportClicked -> abortImportAndExit()
            ListTransferState.Event.DismissError -> clearError()
            ListTransferState.Event.RetryLastActionClicked -> retryLastAction()

            ListTransferState.Event.PickImportFileClicked -> {
                if (!currentState.isBusy) {
                    setEffect(ListTransferState.Effect.OpenImportFilePicker)
                }
            }

            ListTransferState.Event.ImportPickerUnavailable -> {
                lastRetryAction = null
                setState {
                    copy(
                        errorContext = null,
                        errorMessage = UiText.Res(R.string.library_transfer_picker_unavailable),
                        canRetry = false,
                    )
                }
            }

            is ListTransferState.Event.ImportFileSelected -> parseSelectedFile(event.uriString)

            is ListTransferState.Event.ConflictPolicySelected -> {
                if (!currentState.isImporting) {
                    setState {
                        copy(importOptions = importOptions.copy(conflictPolicy = event.policy))
                    }
                }
            }

            is ListTransferState.Event.ImportScoresChanged -> {
                if (!currentState.isImporting) {
                    setState {
                        copy(importOptions = importOptions.copy(importScores = event.enabled))
                    }
                }
            }

            is ListTransferState.Event.ImportWatchedEpisodesChanged -> {
                if (!currentState.isImporting) {
                    setState {
                        copy(
                            importOptions = importOptions.copy(
                                importWatchedEpisodesLocally = event.enabled,
                            ),
                        )
                    }
                }
            }

            ListTransferState.Event.StartImportClicked -> startImport(retryFailedOnly = false)
            ListTransferState.Event.RetryFailedImportClicked -> startImport(retryFailedOnly = true)

            ListTransferState.Event.CancelImportClicked -> {
                if (currentState.isImporting) {
                    cancelImportRequested.set(true)
                    setState { copy(isCancelRequested = true) }
                }
            }

            ListTransferState.Event.ToggleUnmatchedDetails -> {
                setState { copy(showUnmatchedDetails = !showUnmatchedDetails) }
            }

            is ListTransferState.Event.ExportFormatSelected -> {
                if (!currentState.isBusy) {
                    setState { copy(exportFormat = event.format) }
                }
            }

            ListTransferState.Event.ExportSaveClicked -> runExport(ListTransferExportAction.SAVE_TO_FILE)
            ListTransferState.Event.ExportShareClicked -> runExport(ListTransferExportAction.SHARE)
            is ListTransferState.Event.ExportDestinationSelected -> {
                val export = pendingExport ?: return
                pendingExport = null
                writePendingExport(uriString = event.uriString, export = export)
            }
            ListTransferState.Event.ExportDestinationCancelled -> {
                pendingExport = null
            }
            ListTransferState.Event.ExportSaverUnavailable -> {
                val export = pendingExport ?: return
                pendingExport = null
                saveExportToFallbackStorage(export)
            }
        }
    }

    private fun onBackClicked() {
        if (currentState.isImporting) {
            setState { copy(showExitImportDialog = true) }
        } else {
            navigator.back()
        }
    }

    private fun abortImportAndExit() {
        cancelImportRequested.set(true)
        importJob?.cancel()
        importJob = null
        setState {
            copy(
                isImporting = false,
                isCancelRequested = false,
                showExitImportDialog = false,
                importProgress = null,
            )
        }
        navigator.back()
    }

    private fun clearError() {
        lastRetryAction = null
        setState {
            copy(
                errorContext = null,
                errorMessage = null,
                canRetry = false,
            )
        }
    }

    private fun showError(errorUi: ListTransferErrorUi, retryAction: RetryAction?) {
        lastRetryAction = if (errorUi.canRetry) retryAction else null
        setState {
            copy(
                errorContext = errorUi.context,
                errorMessage = errorUi.message,
                canRetry = errorUi.canRetry && retryAction != null,
            )
        }
    }

    private fun retryLastAction() {
        if (currentState.isBusy) return
        val action = lastRetryAction ?: return
        lastRetryAction = null
        when (action) {
            is RetryAction.ParseFile -> parseSelectedFile(action.uriString)
            is RetryAction.Import -> startImport(retryFailedOnly = action.retryFailedOnly)
            is RetryAction.Export -> runExport(action.exportAction)
            is RetryAction.WriteExport -> writePendingExport(action.uriString, action.export)
            is RetryAction.SaveExportFallback -> saveExportToFallbackStorage(action.export)
        }
    }

    private fun parseSelectedFile(uriString: String) {
        if (currentState.isBusy) return
        lastRetryAction = null
        failedImportList = null
        pendingScoreExternalIds = emptySet()
        setState {
            copy(
                isParsingFile = true,
                importSummary = null,
                showUnmatchedDetails = false,
                errorContext = null,
                errorMessage = null,
                canRetry = false,
            )
        }
        viewModelScope.launch {
            runSuspendCatching { parseExternalAnimeList(uriString) }
                .onSuccess { parsed ->
                    parsedImportList = parsed
                    analytics.eventImportFileParsed(parsed.format, parsed.entries.size)
                    setState {
                        copy(
                            isParsingFile = false,
                            importPreview = parsed.toPreviewUi(),
                        )
                    }
                }
                .onFailure { error ->
                    parsedImportList = null
                    setState {
                        copy(
                            isParsingFile = false,
                            importPreview = null,
                        )
                    }
                    showError(
                        errorUi = error.toParseErrorUi(),
                        retryAction = RetryAction.ParseFile(uriString),
                    )
                }
        }
    }

    private fun startImport(retryFailedOnly: Boolean) {
        val targetList = if (retryFailedOnly) failedImportList else parsedImportList
        val parsed = targetList ?: return
        if (currentState.isBusy) return
        val options = currentState.importOptions
        val scoreIdsToRetry = if (retryFailedOnly) pendingScoreExternalIds else emptySet()
        lastRetryAction = null
        cancelImportRequested.set(false)
        setState {
            copy(
                isImporting = true,
                isCancelRequested = false,
                showExitImportDialog = false,
                importProgress = AnimeListImportProgress(
                    phase = AnimeListImportPhase.RESOLVING_TITLES,
                    processed = 0,
                    total = parsed.entries.size,
                ),
                importSummary = null,
                showUnmatchedDetails = false,
                errorContext = null,
                errorMessage = null,
                canRetry = false,
            )
        }
        importJob = viewModelScope.launch {
            runSuspendCatching {
                importAnimeList(
                    parsed = parsed,
                    options = options,
                    pendingScoreExternalIds = scoreIdsToRetry,
                    isCancelled = { cancelImportRequested.get() },
                    onProgress = { progress ->
                        setState { copy(importProgress = progress) }
                    },
                )
            }.onSuccess { result ->
                importJob = null
                failedImportList = result.failedEntries
                    .takeIf { it.isNotEmpty() }
                    ?.let { failed -> ParsedExternalList(format = parsed.format, entries = failed) }
                pendingScoreExternalIds = result.pendingScoreExternalIds
                analytics.eventImportFinished(
                    format = parsed.format,
                    conflictPolicy = options.conflictPolicy,
                    result = result,
                )
                setState {
                    copy(
                        isImporting = false,
                        isCancelRequested = false,
                        showExitImportDialog = false,
                        importProgress = null,
                        importSummary = result.toSummaryUi(),
                    )
                }
            }.onFailure { error ->
                importJob = null
                setState {
                    copy(
                        isImporting = false,
                        isCancelRequested = false,
                        showExitImportDialog = false,
                        importProgress = null,
                    )
                }
                showError(
                    errorUi = error.toImportErrorUi(),
                    retryAction = RetryAction.Import(retryFailedOnly = retryFailedOnly),
                )
            }
        }
    }

    private fun runExport(action: ListTransferExportAction) {
        if (currentState.isBusy) return
        val format = currentState.exportFormat
        pendingExport = null
        lastRetryAction = null
        setState {
            copy(
                isExporting = true,
                exportSummary = null,
                errorContext = null,
                errorMessage = null,
                canRetry = false,
            )
        }
        viewModelScope.launch {
            runSuspendCatching { exportAnimeList(format) }
                .onSuccess { export ->
                    when (action) {
                        ListTransferExportAction.SAVE_TO_FILE -> {
                            pendingExport = export
                            setState { copy(isExporting = false) }
                            setEffect(
                                ListTransferState.Effect.OpenExportFileSaver(
                                    fileName = export.suggestedFileName,
                                    mimeType = export.format.mimeType,
                                ),
                            )
                        }
                        ListTransferExportAction.SHARE -> shareExport(export)
                    }
                }
                .onFailure { error ->
                    setState { copy(isExporting = false) }
                    showError(
                        errorUi = error.toExportErrorUi(),
                        retryAction = RetryAction.Export(action),
                    )
                }
        }
    }

    private suspend fun shareExport(export: AnimeListExportResult) {
        runSuspendCatching { shareExportedAnimeList(export) }
            .onSuccess { uriString ->
                analytics.eventExportCompleted(
                    format = export.format,
                    exportedCount = export.exportedCount,
                    skippedWithoutIdCount = export.skippedWithoutExternalIdCount,
                )
                setState {
                    copy(
                        isExporting = false,
                        exportSummary = export.toSummaryUi(),
                    )
                }
                setEffect(
                    ListTransferState.Effect.ShareExportFile(
                        uriString = uriString,
                        mimeType = export.format.mimeType,
                    ),
                )
            }
            .onFailure { error ->
                setState { copy(isExporting = false) }
                showError(
                    errorUi = error.toSaveErrorUi(canRetry = true),
                    retryAction = RetryAction.Export(ListTransferExportAction.SHARE),
                )
            }
    }

    private fun writePendingExport(uriString: String, export: AnimeListExportResult) {
        lastRetryAction = null
        setState {
            copy(
                isExporting = true,
                errorContext = null,
                errorMessage = null,
                canRetry = false,
            )
        }
        viewModelScope.launch {
            runSuspendCatching {
                saveExportedAnimeList.saveToUri(uriString = uriString, export = export)
            }.onSuccess {
                analytics.eventExportCompleted(
                    format = export.format,
                    exportedCount = export.exportedCount,
                    skippedWithoutIdCount = export.skippedWithoutExternalIdCount,
                )
                setState {
                    copy(
                        isExporting = false,
                        exportSummary = export.toSummaryUi(),
                    )
                }
                setEffect(ListTransferState.Effect.ShowExportSavedToast())
            }.onFailure { error ->
                setState { copy(isExporting = false) }
                showError(
                    errorUi = error.toSaveErrorUi(canRetry = true),
                    retryAction = RetryAction.WriteExport(uriString = uriString, export = export),
                )
            }
        }
    }

    private fun saveExportToFallbackStorage(export: AnimeListExportResult) {
        lastRetryAction = null
        setState {
            copy(
                isExporting = true,
                errorContext = null,
                errorMessage = null,
                canRetry = false,
            )
        }
        viewModelScope.launch {
            runSuspendCatching { saveExportedAnimeList.saveToStorage(export) }
                .onSuccess { savedPath ->
                    analytics.eventExportCompleted(
                        format = export.format,
                        exportedCount = export.exportedCount,
                        skippedWithoutIdCount = export.skippedWithoutExternalIdCount,
                    )
                    setState {
                        copy(
                            isExporting = false,
                            exportSummary = export.toSummaryUi(savedPath = savedPath),
                        )
                    }
                    setEffect(ListTransferState.Effect.ShowExportSavedToast(savedPath = savedPath))
                }
                .onFailure { error ->
                    setState { copy(isExporting = false) }
                    showError(
                        errorUi = error.toSaveErrorUi(canRetry = true),
                        retryAction = RetryAction.SaveExportFallback(export),
                    )
                }
        }
    }

    private sealed interface RetryAction {
        data class ParseFile(val uriString: String) : RetryAction
        data class Import(val retryFailedOnly: Boolean) : RetryAction
        data class Export(val exportAction: ListTransferExportAction) : RetryAction
        data class WriteExport(
            val uriString: String,
            val export: AnimeListExportResult,
        ) : RetryAction
        data class SaveExportFallback(val export: AnimeListExportResult) : RetryAction
    }
}
