package su.afk.yummy.tv.feature.update.handler

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.domain.update.model.UpdateDownloadException
import su.afk.yummy.tv.domain.update.model.UpdateDownloadState
import su.afk.yummy.tv.domain.update.repository.ApkInstallRepository
import su.afk.yummy.tv.domain.update.usecase.ObserveUpdateDownloadUseCase
import su.afk.yummy.tv.domain.update.usecase.StartUpdateDownloadUseCase
import su.afk.yummy.tv.feature.update.UpdateAnalytics
import java.io.File
import javax.inject.Inject

/** Starts the background update download, observes it and installs the downloaded APK. */
internal class UpdateInstallHandler @Inject constructor(
    private val startUpdateDownload: StartUpdateDownloadUseCase,
    private val observeUpdateDownload: ObserveUpdateDownloadUseCase,
    private val apkInstallRepository: ApkInstallRepository,
    private val analytics: UpdateAnalytics,
) {
    fun startDownload(apkUrl: String) = startUpdateDownload(apkUrl)

    fun observeDownload(apkUrl: String): Flow<UpdateDownloadState> = observeUpdateDownload(apkUrl)

    fun reportDownloadError(version: String?, message: String?) =
        analytics.eventDownloadError(version, UpdateDownloadException(message))

    suspend fun install(file: File, version: String?): UpdateInstallResult =
        runSuspendCatching {
            apkInstallRepository.install(file)
        }.fold(
            onSuccess = { UpdateInstallResult.Success(file) },
            onFailure = { error ->
                analytics.eventInstallError(version, error)
                UpdateInstallResult.Failure(error)
            },
        )
}

/** Outcome of installing an update APK. */
internal sealed interface UpdateInstallResult {
    data class Success(val file: File) : UpdateInstallResult
    data class Failure(val error: Throwable) : UpdateInstallResult
}
