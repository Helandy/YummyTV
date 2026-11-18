package su.afk.yummy.tv.feature.library.utils

import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.domain.library.model.AnimeListTransferStage
import su.afk.yummy.tv.domain.library.model.AnimeListTransferStageException
import su.afk.yummy.tv.domain.library.model.EmptyExportListException
import su.afk.yummy.tv.domain.library.model.ExternalListParseError
import su.afk.yummy.tv.domain.library.model.ExternalListParseException
import su.afk.yummy.tv.feature.library.presentation.R

internal data class ListTransferErrorUi(
    val context: UiText? = null,
    val message: UiText,
    val canRetry: Boolean = false,
)

internal fun Throwable.toParseErrorUi(): ListTransferErrorUi =
    when (this) {
        is ExternalListParseException -> ListTransferErrorUi(
            message = UiText.Res(
                when (error) {
                    ExternalListParseError.EMPTY_FILE -> R.string.library_transfer_error_empty_file
                    ExternalListParseError.FILE_TOO_LARGE -> R.string.library_transfer_error_file_too_large
                    ExternalListParseError.UNSUPPORTED_FORMAT -> R.string.library_transfer_error_unsupported_format
                    ExternalListParseError.MALFORMED_CONTENT -> R.string.library_transfer_error_malformed
                    ExternalListParseError.NO_VALID_ENTRIES -> R.string.library_transfer_error_no_entries
                },
            ),
            canRetry = false,
        )
        else -> ListTransferErrorUi(
            context = UiText.Res(R.string.library_transfer_error_read_failed),
            message = UiText.Failure(this, UiText.Res(R.string.library_transfer_error_read_failed)),
            canRetry = true,
        )
    }

internal fun Throwable.toImportErrorUi(): ListTransferErrorUi {
    val fallback = UiText.Res(R.string.library_transfer_error_import_failed)
    return when (this) {
        is AnimeListTransferStageException -> ListTransferErrorUi(
            context = toStageContextUiText(fallback),
            message = UiText.Failure(cause, fallback),
            canRetry = true,
        )
        else -> ListTransferErrorUi(
            context = fallback,
            message = UiText.Failure(this, fallback),
            canRetry = true,
        )
    }
}

internal fun Throwable.toExportErrorUi(): ListTransferErrorUi {
    val fallback = UiText.Res(R.string.library_transfer_error_export_failed)
    return when (this) {
        is EmptyExportListException -> ListTransferErrorUi(
            message = UiText.Res(R.string.library_transfer_error_empty_export),
            canRetry = false,
        )
        is AnimeListTransferStageException -> ListTransferErrorUi(
            context = toStageContextUiText(fallback),
            message = UiText.Failure(cause, fallback),
            canRetry = true,
        )
        else -> ListTransferErrorUi(
            context = fallback,
            message = UiText.Failure(this, fallback),
            canRetry = true,
        )
    }
}

internal fun Throwable.toSaveErrorUi(canRetry: Boolean): ListTransferErrorUi {
    val fallback = UiText.Res(R.string.library_transfer_error_save_failed)
    return ListTransferErrorUi(
        context = fallback,
        message = UiText.Failure(this, fallback),
        canRetry = canRetry,
    )
}

private fun AnimeListTransferStageException.toStageContextUiText(fallback: UiText): UiText =
    when (stage) {
        AnimeListTransferStage.RESOLVE_EXTERNAL_IDS -> UiText.Res(
            R.string.library_transfer_error_stage_resolve,
            listOf(totalBatches),
        )
        AnimeListTransferStage.LOAD_USER_LIBRARY -> UiText.Res(
            R.string.library_transfer_error_stage_load_library,
        )
        AnimeListTransferStage.ENRICH_GUEST_LIBRARY ->
            if (totalBatches > 0) {
                UiText.Res(
                    R.string.library_transfer_error_stage_guest_ids,
                    listOf(completedBatches, totalBatches),
                )
            } else {
                fallback
            }
    }
