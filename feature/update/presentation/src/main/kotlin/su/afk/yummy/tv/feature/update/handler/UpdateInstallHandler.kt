package su.afk.yummy.tv.feature.update.handler

import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.domain.update.repository.ApkDownloader
import su.afk.yummy.tv.domain.update.repository.ApkInstaller
import su.afk.yummy.tv.feature.update.UpdateAnalytics
import java.io.File
import javax.inject.Inject

/** Downloads and installs an update APK, mapping download/install failures to a result. */
internal class UpdateInstallHandler @Inject constructor(
    private val apkDownloader: ApkDownloader,
    private val apkInstaller: ApkInstaller,
    private val analytics: UpdateAnalytics,
) {
    suspend fun download(
        apkUrl: String,
        version: String?,
        onProgress: (Float) -> Unit,
    ): UpdateDownloadResult =
        runSuspendCatching {
            apkDownloader.download(apkUrl, onProgress)
        }.fold(
            onSuccess = { file -> UpdateDownloadResult.Success(file) },
            onFailure = { error ->
                analytics.eventDownloadError(version, error)
                UpdateDownloadResult.Failure(error)
            },
        )

    suspend fun install(file: File, version: String?): UpdateInstallResult =
        runSuspendCatching {
            apkInstaller.install(file)
        }.fold(
            onSuccess = { UpdateInstallResult.Success(file) },
            onFailure = { error ->
                analytics.eventInstallError(version, error)
                UpdateInstallResult.Failure(error)
            },
        )
}

/** Outcome of downloading an update APK. */
internal sealed interface UpdateDownloadResult {
    data class Success(val file: File) : UpdateDownloadResult
    data class Failure(val error: Throwable) : UpdateDownloadResult
}

/** Outcome of installing an update APK. */
internal sealed interface UpdateInstallResult {
    data class Success(val file: File) : UpdateInstallResult
    data class Failure(val error: Throwable) : UpdateInstallResult
}
