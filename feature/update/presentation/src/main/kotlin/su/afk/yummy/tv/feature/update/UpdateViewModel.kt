package su.afk.yummy.tv.feature.update

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.domain.update.model.UpdateDownloadException
import su.afk.yummy.tv.domain.update.model.UpdateDownloadState
import su.afk.yummy.tv.feature.update.handler.UpdateInstallHandler
import su.afk.yummy.tv.feature.update.handler.UpdateInstallResult
import su.afk.yummy.tv.feature.update.presentation.R
import java.io.File
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject internal constructor(
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val updateInstallHandler: UpdateInstallHandler,
    private val stringProvider: StringProvider,
    private val analytics: UpdateAnalytics,
) : BaseViewModel<UpdateState.State, UpdateState.Event, UpdateState.Effect>() {

    private var downloadedApk: File? = null
    private var downloadJob: Job? = null
    private var availableStatus: UpdateState.State.Status.Available? = null
    private var userStartedDownload = false
    private var installTriggered = false
    private var updateVersion: String? = null

    override fun createInitialState() = UpdateState.State()

    override fun onEvent(event: UpdateState.Event) {
        when (event) {
            is UpdateState.Event.Init -> initWithUpdateInfo(
                version = event.version,
                apkUrl = event.apkUrl,
                changelog = event.changelog,
                required = event.required,
                updatesCount = event.updatesCount,
                isPrerelease = event.isPrerelease,
            )

            UpdateState.Event.Dismiss -> {
                if ((currentState.status as? UpdateState.State.Status.Available)?.required == true) return
                analytics.eventDismiss(currentUpdateVersion())
                setState { copy(status = UpdateState.State.Status.Idle) }
                nav.back()
            }

            is UpdateState.Event.ConfirmUpdate -> {
                analytics.eventConfirm(currentUpdateVersion())
                downloadAndInstall(event.apkUrl)
            }

            is UpdateState.Event.RetryUpdate -> {
                analytics.eventRetry(currentUpdateVersion())
                retryInstall(event.apkUrl)
            }
        }
    }

    private fun initWithUpdateInfo(
        version: String,
        apkUrl: String,
        changelog: String,
        required: Boolean,
        updatesCount: Int,
        isPrerelease: Boolean,
    ) {
        if (currentState.status is UpdateState.State.Status.Idle) {
            updateVersion = version
            val available = UpdateState.State.Status.Available(
                version = version,
                changelog = changelog,
                apkUrl = apkUrl,
                required = required,
                updatesCount = updatesCount,
                isPrerelease = isPrerelease,
            )
            availableStatus = available
            setState { copy(status = available) }
            // Загрузка могла начаться раньше (диалог закрыли или приложение свернули) — подхватываем её.
            observeDownload(apkUrl)
        }
    }

    private fun downloadAndInstall(apkUrl: String) {
        userStartedDownload = true
        setState { copy(status = UpdateState.State.Status.Downloading(0f)) }
        updateInstallHandler.startDownload(apkUrl)
        observeDownload(apkUrl)
    }

    /**
     * Подписывается на фоновую загрузку этого APK. Сама загрузка живёт в WorkManager, поэтому
     * диалог можно закрыть или открыть заново (например, по уведомлению) и продолжить с того же места.
     */
    private fun observeDownload(apkUrl: String) {
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            updateInstallHandler.observeDownload(apkUrl).collect { download ->
                when (download) {
                    UpdateDownloadState.Idle -> restoreAvailableIfDownloading()
                    is UpdateDownloadState.Downloading -> setState {
                        copy(status = UpdateState.State.Status.Downloading(download.progress))
                    }

                    is UpdateDownloadState.Downloaded -> installDownloaded(download.file, apkUrl)
                    is UpdateDownloadState.Failed -> if (userStartedDownload) {
                        updateInstallHandler.reportDownloadError(currentUpdateVersion(), download.message)
                        setUpdateError(UpdateDownloadException(download.message), apkUrl)
                    }
                }
            }
        }
    }

    private suspend fun installDownloaded(file: File, apkUrl: String) {
        if (installTriggered) return
        installTriggered = true
        downloadedApk = file
        setState { copy(status = UpdateState.State.Status.Installing) }
        applyInstallResult(updateInstallHandler.install(file, currentUpdateVersion()), apkUrl)
    }

    /** Загрузку отменили извне (из уведомления): возвращаем диалог к предложению обновиться. */
    private fun restoreAvailableIfDownloading() {
        if (currentState.status !is UpdateState.State.Status.Downloading) return
        val available = availableStatus ?: return
        userStartedDownload = false
        setState { copy(status = available) }
    }

    private fun retryInstall(apkUrl: String) {
        val file = downloadedApk
        if (file == null || !file.exists()) {
            downloadAndInstall(apkUrl)
            return
        }

        viewModelScope.launch {
            setState { copy(status = UpdateState.State.Status.Installing) }
            applyInstallResult(updateInstallHandler.install(file, currentUpdateVersion()), apkUrl)
        }
    }

    private fun applyInstallResult(result: UpdateInstallResult, apkUrl: String) {
        when (result) {
            is UpdateInstallResult.Success -> Unit
            is UpdateInstallResult.Failure -> setUpdateError(result.error, apkUrl)
        }
    }

    private fun setUpdateError(error: Throwable, apkUrl: String) {
        setState {
            copy(
                status = UpdateState.State.Status.Error(
                    message = error.message
                        ?: stringProvider.get(R.string.update_error_fallback_message),
                    apkUrl = apkUrl,
                )
            )
        }
    }

    private fun currentUpdateVersion(): String? =
        updateVersion ?: (currentState.status as? UpdateState.State.Status.Available)?.version
}
