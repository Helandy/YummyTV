package su.afk.yummy.tv.feature.update

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.update.handler.UpdateDownloadResult
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
            setState {
                copy(
                    status = UpdateState.State.Status.Available(
                        version = version,
                        changelog = changelog,
                        apkUrl = apkUrl,
                        required = required,
                        updatesCount = updatesCount,
                        isPrerelease = isPrerelease,
                    )
                )
            }
        }
    }

    private fun downloadAndInstall(apkUrl: String) {
        viewModelScope.launch {
            setState { copy(status = UpdateState.State.Status.Downloading(0f)) }

            val downloadResult = updateInstallHandler.download(
                apkUrl = apkUrl,
                version = currentUpdateVersion(),
                onProgress = { progress ->
                    setState { copy(status = UpdateState.State.Status.Downloading(progress)) }
                },
            )
            val file = when (downloadResult) {
                is UpdateDownloadResult.Success -> downloadResult.file
                is UpdateDownloadResult.Failure -> {
                    setUpdateError(downloadResult.error, apkUrl)
                    return@launch
                }
            }

            downloadedApk = file
            setState { copy(status = UpdateState.State.Status.Installing) }
            applyInstallResult(updateInstallHandler.install(file, currentUpdateVersion()), apkUrl)
        }
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
