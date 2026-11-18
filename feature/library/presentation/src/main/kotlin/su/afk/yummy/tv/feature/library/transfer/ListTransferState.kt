package su.afk.yummy.tv.feature.library.transfer

import androidx.compose.runtime.Immutable
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.domain.library.model.AnimeListImportOptions
import su.afk.yummy.tv.domain.library.model.AnimeListImportProgress
import su.afk.yummy.tv.domain.library.model.ExternalListFormat
import su.afk.yummy.tv.domain.library.model.ImportConflictPolicy
import su.afk.yummy.tv.feature.library.model.ListTransferExportSummaryUi
import su.afk.yummy.tv.feature.library.model.ListTransferImportSummaryUi
import su.afk.yummy.tv.feature.library.model.ListTransferPreviewUi

interface ListTransferState {

    @Immutable
    data class State(
        val isSignedIn: Boolean = false,
        val exportFormat: ExternalListFormat = ExternalListFormat.MAL_XML,
        val isExporting: Boolean = false,
        val exportSummary: ListTransferExportSummaryUi? = null,
        val isParsingFile: Boolean = false,
        val importPreview: ListTransferPreviewUi? = null,
        val importOptions: AnimeListImportOptions = AnimeListImportOptions(),
        val isImporting: Boolean = false,
        val isCancelRequested: Boolean = false,
        val showExitImportDialog: Boolean = false,
        val importProgress: AnimeListImportProgress? = null,
        val importSummary: ListTransferImportSummaryUi? = null,
        val showUnmatchedDetails: Boolean = false,
        val errorContext: UiText? = null,
        val errorMessage: UiText? = null,
        val canRetry: Boolean = false,
    ) : UiState {
        val isBusy: Boolean
            get() = isParsingFile || isImporting || isExporting

        val keepScreenOn: Boolean
            get() = isImporting || isExporting
    }

    sealed interface Event : UiEvent {
        data object BackClicked : Event
        data object DismissExitImportDialog : Event
        data object ConfirmExitImportClicked : Event
        data object DismissError : Event
        data object RetryLastActionClicked : Event

        data object PickImportFileClicked : Event
        data object ImportPickerUnavailable : Event
        data class ImportFileSelected(val uriString: String) : Event
        data class ConflictPolicySelected(val policy: ImportConflictPolicy) : Event
        data class ImportScoresChanged(val enabled: Boolean) : Event
        data class ImportWatchedEpisodesChanged(val enabled: Boolean) : Event
        data object StartImportClicked : Event
        data object RetryFailedImportClicked : Event
        data object CancelImportClicked : Event
        data object ToggleUnmatchedDetails : Event

        data class ExportFormatSelected(val format: ExternalListFormat) : Event
        data object ExportSaveClicked : Event
        data object ExportShareClicked : Event
        data class ExportDestinationSelected(val uriString: String) : Event
        data object ExportDestinationCancelled : Event
        data object ExportSaverUnavailable : Event
    }

    sealed interface Effect : UiEffect {
        data object OpenImportFilePicker : Effect
        data class OpenExportFileSaver(
            val fileName: String,
            val mimeType: String,
        ) : Effect
        data class ShareExportFile(
            val uriString: String,
            val mimeType: String,
        ) : Effect
        data class ShowExportSavedToast(
            val savedPath: String? = null,
        ) : Effect
    }
}
