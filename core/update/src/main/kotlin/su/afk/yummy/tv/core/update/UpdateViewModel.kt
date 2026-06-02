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
import su.afk.yummy.tv.core.utils.PlatformCapabilities
import java.io.File
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val apkDownloader: ApkDownloader,
    private val apkInstaller: ApkInstaller,
    private val stringProvider: StringProvider,
) : BaseViewModelNew<UpdateState.State, UpdateState.Event, UpdateState.Effect>(savedStateHandle) {

    private var downloadedApk: File? = null

    override fun createInitialState() = UpdateState.State()

    fun initWithUpdateInfo(version: String, apkUrl: String, changelog: String) {
        if (currentState.status is UpdateState.State.Status.Idle) {
            setState {
                copy(
                    status = UpdateState.State.Status.Available(
                        version = version,
                        changelog = changelog,
                        apkUrl = apkUrl,
                        installSupported = PlatformCapabilities.supportsInAppUpdateInstall,
                    )
                )
            }
        }
    }

    override fun onEvent(event: UpdateState.Event) {
        when (event) {
            UpdateState.Event.Dismiss -> {
                setState { copy(status = UpdateState.State.Status.Idle) }
                setEffect(UpdateState.Effect.NavigateBack)
            }
            is UpdateState.Event.ConfirmUpdate -> downloadAndInstall(event.apkUrl)
            is UpdateState.Event.RetryUpdate -> retryInstall(event.apkUrl)
        }
    }

    private fun downloadAndInstall(apkUrl: String) {
        if (!PlatformCapabilities.supportsInAppUpdateInstall) return

        viewModelScope.launch {
            setState { copy(status = UpdateState.State.Status.Downloading(0f)) }
            runCatching {
                val file = apkDownloader.download(apkUrl) { progress ->
                    setState { copy(status = UpdateState.State.Status.Downloading(progress)) }
                }
                downloadedApk = file
                setState { copy(status = UpdateState.State.Status.Installing) }
                apkInstaller.install(file)
            }.onFailure { e ->
                setState {
                    copy(
                        status = UpdateState.State.Status.Error(
                            message = e.message ?: stringProvider.get(R.string.update_error_title),
                            apkUrl = apkUrl,
                        )
                    )
                }
            }
        }
    }

    private fun retryInstall(apkUrl: String) {
        if (!PlatformCapabilities.supportsInAppUpdateInstall) return

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
                setState {
                    copy(
                        status = UpdateState.State.Status.Error(
                            message = e.message ?: stringProvider.get(R.string.update_error_title),
                            apkUrl = apkUrl,
                        )
                    )
                }
            }
        }
    }
}
