package su.afk.yummy.tv.domain.update.repository

import java.io.File

interface ApkDownloadRepository {
    suspend fun download(url: String, onProgress: suspend (Float) -> Unit): File
}
