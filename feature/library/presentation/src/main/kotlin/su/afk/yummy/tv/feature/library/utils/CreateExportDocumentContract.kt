package su.afk.yummy.tv.feature.library.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import su.afk.yummy.tv.feature.library.model.CreateExportDocumentRequest

/**
 * Контракт SAF `ACTION_CREATE_DOCUMENT`, принимающий MIME-тип и имя файла во время вызова —
 * чтобы один launcher обслуживал и `.xml`, и `.json`.
 */
class CreateExportDocumentContract : ActivityResultContract<CreateExportDocumentRequest, Uri?>() {
    override fun createIntent(context: Context, input: CreateExportDocumentRequest): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(input.mimeType)
            .putExtra(Intent.EXTRA_TITLE, input.fileName)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        intent.takeIf { resultCode == Activity.RESULT_OK }?.data
}

/**
 * MIME-типы для SAF `OpenDocument`: включает `*&#47;*`, поскольку файловые менеджеры Android TV и
 * сторонних прошивок нередко отдают `.xml.gz` и `.json` как `application/octet-stream`.
 */
val IMPORT_LIST_DOCUMENT_MIME_TYPES: Array<String> = arrayOf("*/*")
