package su.afk.yummy.tv.core.update

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.update.apk.ApkDownloader
import su.afk.yummy.tv.core.update.apk.ApkInstaller
import java.io.File
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject internal constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val apkDownloader: ApkDownloader,
    private val apkInstaller: ApkInstaller,
    private val stringProvider: StringProvider,
    private val analytics: UpdateAnalytics,
) : BaseViewModelNew<UpdateState.State, UpdateState.Event, UpdateState.Effect>(savedStateHandle) {

    private var downloadedApk: File? = null
    private var updateVersion: String? = null

    override fun createInitialState() = UpdateState.State()

    fun initWithUpdateInfo(
        version: String,
        apkUrl: String,
        changelog: String,
        required: Boolean = false,
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
                    )
                )
            }
        }
    }

    override fun onEvent(event: UpdateState.Event) {
        when (event) {
            UpdateState.Event.Dismiss -> {
                if ((currentState.status as? UpdateState.State.Status.Available)?.required == true) return
                analytics.eventDismiss(currentUpdateVersion())
                setState { copy(status = UpdateState.State.Status.Idle) }
                setEffect(UpdateState.Effect.NavigateBack)
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

    private fun downloadAndInstall(apkUrl: String) {
        viewModelScope.launch {
            setState { copy(status = UpdateState.State.Status.Downloading(0f)) }

            val file = runCatching {
                apkDownloader.download(apkUrl) { progress ->
                    setState { copy(status = UpdateState.State.Status.Downloading(progress)) }
                }
            }.getOrElse { e ->
                analytics.eventDownloadError(currentUpdateVersion(), e)
                setUpdateError(e, apkUrl)
                return@launch
            }

            downloadedApk = file
            setState { copy(status = UpdateState.State.Status.Installing) }
            runCatching {
                apkInstaller.install(file)
            }.onFailure { e ->
                analytics.eventInstallError(currentUpdateVersion(), e)
                setUpdateError(e, apkUrl)
            }
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
            runCatching {
                apkInstaller.install(file)
            }.onFailure { e ->
                analytics.eventInstallError(currentUpdateVersion(), e)
                setUpdateError(e, apkUrl)
            }
        }
    }

    private fun setUpdateError(error: Throwable, apkUrl: String) {
        setState {
            copy(
                status = UpdateState.State.Status.Error(
                    message = error.message ?: stringProvider.get(R.string.update_error_title),
                    apkUrl = apkUrl,
                )
            )
        }
    }

    private fun currentUpdateVersion(): String? =
        updateVersion ?: (currentState.status as? UpdateState.State.Status.Available)?.version
}
