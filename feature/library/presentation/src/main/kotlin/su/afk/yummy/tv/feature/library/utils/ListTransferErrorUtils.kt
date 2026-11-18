package su.afk.yummy.tv.feature.library.utils

import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.domain.library.model.EmptyExportListException
import su.afk.yummy.tv.domain.library.model.ExternalListParseError
import su.afk.yummy.tv.domain.library.model.ExternalListParseException
import su.afk.yummy.tv.feature.library.presentation.R

internal fun Throwable.toParseErrorText(): UiText =
    when (this) {
        is ExternalListParseException -> UiText.Res(
            when (error) {
                ExternalListParseError.EMPTY_FILE -> R.string.library_transfer_error_empty_file
                ExternalListParseError.FILE_TOO_LARGE -> R.string.library_transfer_error_file_too_large
                ExternalListParseError.UNSUPPORTED_FORMAT -> R.string.library_transfer_error_unsupported_format
                ExternalListParseError.MALFORMED_CONTENT -> R.string.library_transfer_error_malformed
                ExternalListParseError.NO_VALID_ENTRIES -> R.string.library_transfer_error_no_entries
            },
        )
        else -> UiText.Failure(this, UiText.Res(R.string.library_transfer_error_read_failed))
    }

internal fun Throwable.toExportErrorText(): UiText =
    when (this) {
        is EmptyExportListException -> UiText.Res(R.string.library_transfer_error_empty_export)
        else -> UiText.Failure(this, UiText.Res(R.string.library_transfer_error_export_failed))
    }

internal fun Throwable.toImportErrorText(): UiText =
    UiText.Failure(this, UiText.Res(R.string.library_transfer_error_import_failed))

internal fun Throwable.toSaveErrorText(): UiText =
    UiText.Failure(this, UiText.Res(R.string.library_transfer_error_save_failed))
