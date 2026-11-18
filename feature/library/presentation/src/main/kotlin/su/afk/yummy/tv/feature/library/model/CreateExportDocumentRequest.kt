package su.afk.yummy.tv.feature.library.model

data class CreateExportDocumentRequest(
    val fileName: String,
    val mimeType: String,
)
