package su.afk.yummy.tv.domain.update.repository

import java.io.File

interface ApkInstallRepository {
    suspend fun install(apkFile: File)
}
