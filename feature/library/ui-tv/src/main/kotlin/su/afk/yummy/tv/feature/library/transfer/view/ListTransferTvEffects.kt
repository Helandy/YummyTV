package su.afk.yummy.tv.feature.library.transfer.view

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.core.net.toUri
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.mvi.collectAsEffect
import su.afk.yummy.tv.feature.library.model.CreateExportDocumentRequest
import su.afk.yummy.tv.feature.library.presentation.R
import su.afk.yummy.tv.feature.library.transfer.ListTransferState
import su.afk.yummy.tv.feature.library.utils.CreateExportDocumentContract
import su.afk.yummy.tv.feature.library.utils.IMPORT_LIST_DOCUMENT_MIME_TYPES

@Composable
internal fun ListTransferTvEffects(
    effect: Flow<ListTransferState.Effect>,
    onEvent: (ListTransferState.Event) -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val importFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            onEvent(ListTransferState.Event.ImportFileSelected(uri.toString()))
        }
    }
    val exportFileSaver = rememberLauncherForActivityResult(
        contract = CreateExportDocumentContract(),
    ) { uri ->
        if (uri != null) {
            onEvent(ListTransferState.Event.ExportDestinationSelected(uri.toString()))
        } else {
            onEvent(ListTransferState.Event.ExportDestinationCancelled)
        }
    }

    effect.collectAsEffect { item ->
        when (item) {
            ListTransferState.Effect.OpenImportFilePicker -> {
                runCatching { importFilePicker.launch(IMPORT_LIST_DOCUMENT_MIME_TYPES) }
                    .onFailure { onEvent(ListTransferState.Event.ImportPickerUnavailable) }
            }

            is ListTransferState.Effect.OpenExportFileSaver -> {
                // На части ТВ-прошивок нет системного DocumentsUI — сохраняем в Загрузки напрямую.
                runCatching {
                    exportFileSaver.launch(
                        CreateExportDocumentRequest(
                            fileName = item.fileName,
                            mimeType = item.mimeType,
                        ),
                    )
                }.onFailure {
                    onEvent(ListTransferState.Event.ExportSaverUnavailable)
                }
            }

            is ListTransferState.Effect.ShareExportFile -> {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = item.mimeType
                    putExtra(Intent.EXTRA_STREAM, item.uriString.toUri())
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val canShare = context.packageManager.queryIntentActivities(sendIntent, 0).isNotEmpty()
                val shared = canShare && runCatching {
                    context.startActivity(
                        Intent.createChooser(sendIntent, null)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }.isSuccess
                if (!shared) {
                    onEvent(ListTransferState.Event.ExportSaveClicked)
                }
            }

            is ListTransferState.Effect.ShowExportSavedToast -> {
                val text = if (item.savedPath != null) {
                    resources.getString(R.string.library_transfer_export_saved_path, item.savedPath)
                } else {
                    resources.getString(R.string.library_transfer_file_saved_toast)
                }
                Toast.makeText(context, text, Toast.LENGTH_LONG).show()
            }
        }
    }
}
