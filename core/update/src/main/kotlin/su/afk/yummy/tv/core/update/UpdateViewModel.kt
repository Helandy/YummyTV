package su.afk.yummy.tv.core.update

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.analytics.AnalyticsEvents
import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.analyticsParamsOf
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.update.apk.ApkDownloader
import su.afk.yummy.tv.core.update.apk.ApkInstaller
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
    private val analyticsTracker: AnalyticsTracker,
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
                    )
                )
            }
        }
    }

    override fun onEvent(event: UpdateState.Event) {
        when (event) {
            UpdateState.Event.Dismiss -> {
                trackUpdateAction("dismiss")
                setState { copy(status = UpdateState.State.Status.Idle) }
                setEffect(UpdateState.Effect.NavigateBack)
            }

            is UpdateState.Event.ConfirmUpdate -> {
                trackUpdateAction("confirm")
                downloadAndInstall(event.apkUrl)
            }

            is UpdateState.Event.RetryUpdate -> {
                trackUpdateAction("retry")
                retryInstall(event.apkUrl)
            }
        }
    }

    private fun downloadAndInstall(apkUrl: String) {
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

    private fun trackUpdateAction(action: String) {
        analyticsTracker.track(
            AnalyticsEvents.updateAction(
                action = action,
                params = updateAnalyticsParams(),
            )
        )
    }

    private fun updateAnalyticsParams(): Map<String, String> {
        val version = (currentState.status as? UpdateState.State.Status.Available)?.version
        return analyticsParamsOf("version" to version)
    }
}
