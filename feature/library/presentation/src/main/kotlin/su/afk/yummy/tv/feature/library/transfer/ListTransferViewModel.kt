package su.afk.yummy.tv.feature.library.transfer

import dagger.hilt.android.lifecycle.HiltViewModel
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
import su.afk.yummy.tv.feature.library.utils.toExportErrorText
import su.afk.yummy.tv.feature.library.utils.toImportErrorText
import su.afk.yummy.tv.feature.library.utils.toParseErrorText
import su.afk.yummy.tv.feature.library.utils.toSaveErrorText
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
    private var pendingExport: AnimeListExportResult? = null
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
            ListTransferState.Event.BackClicked -> navigator.back()
            ListTransferState.Event.DismissError -> setState { copy(errorMessage = null) }

            ListTransferState.Event.PickImportFileClicked -> {
                if (!currentState.isBusy) {
                    setEffect(ListTransferState.Effect.OpenImportFilePicker)
                }
            }

            ListTransferState.Event.ImportPickerUnavailable -> {
                setState {
                    copy(errorMessage = UiText.Res(R.string.library_transfer_picker_unavailable))
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

            ListTransferState.Event.StartImportClicked -> startImport()

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
            is ListTransferState.Event.ExportDestinationSelected -> writePendingExport(event.uriString)
            ListTransferState.Event.ExportDestinationCancelled -> {
                pendingExport = null
            }
            ListTransferState.Event.ExportSaverUnavailable -> savePendingExportToFallbackStorage()
        }
    }

    private fun parseSelectedFile(uriString: String) {
        if (currentState.isBusy) return
        setState {
            copy(
                isParsingFile = true,
                importSummary = null,
                showUnmatchedDetails = false,
                errorMessage = null,
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
                            errorMessage = error.toParseErrorText(),
                        )
                    }
                }
        }
    }

    private fun startImport() {
        val parsed = parsedImportList ?: return
        if (currentState.isBusy) return
        val options = currentState.importOptions
        cancelImportRequested.set(false)
        setState {
            copy(
                isImporting = true,
                isCancelRequested = false,
                importProgress = AnimeListImportProgress(
                    phase = AnimeListImportPhase.RESOLVING_TITLES,
                    processed = 0,
                    total = parsed.entries.size,
                ),
                importSummary = null,
                showUnmatchedDetails = false,
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            runSuspendCatching {
                importAnimeList(
                    parsed = parsed,
                    options = options,
                    isCancelled = { cancelImportRequested.get() },
                    onProgress = { progress ->
                        setState { copy(importProgress = progress) }
                    },
                )
            }.onSuccess { result ->
                analytics.eventImportFinished(
                    format = parsed.format,
                    conflictPolicy = options.conflictPolicy,
                    result = result,
                )
                setState {
                    copy(
                        isImporting = false,
                        isCancelRequested = false,
                        importProgress = null,
                        importSummary = result.toSummaryUi(),
                    )
                }
            }.onFailure { error ->
                setState {
                    copy(
                        isImporting = false,
                        isCancelRequested = false,
                        importProgress = null,
                        errorMessage = error.toImportErrorText(),
                    )
                }
            }
        }
    }

    private fun runExport(action: ListTransferExportAction) {
        if (currentState.isBusy) return
        val format = currentState.exportFormat
        pendingExport = null
        setState {
            copy(
                isExporting = true,
                exportSummary = null,
                errorMessage = null,
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
                    setState {
                        copy(
                            isExporting = false,
                            errorMessage = error.toExportErrorText(),
                        )
                    }
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
                setState {
                    copy(
                        isExporting = false,
                        errorMessage = error.toSaveErrorText(),
                    )
                }
            }
    }

    private fun writePendingExport(uriString: String) {
        val export = pendingExport ?: return
        pendingExport = null
        setState { copy(isExporting = true, errorMessage = null) }
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
                setState {
                    copy(
                        isExporting = false,
                        errorMessage = error.toSaveErrorText(),
                    )
                }
            }
        }
    }

    private fun savePendingExportToFallbackStorage() {
        val export = pendingExport ?: return
        pendingExport = null
        setState { copy(isExporting = true, errorMessage = null) }
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
                    setState {
                        copy(
                            isExporting = false,
                            errorMessage = error.toSaveErrorText(),
                        )
                    }
                }
        }
    }
}
