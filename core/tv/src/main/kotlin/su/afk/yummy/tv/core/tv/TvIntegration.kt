package su.afk.yummy.tv.core.tv

import android.content.Intent
import android.media.tv.TvContract
import android.os.Build
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.analytics.api.coroutine.ErrorCoroutineAnalytics
import su.afk.yummy.tv.core.preferences.settings.AppLifecycleSettingsStore
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStorage
import su.afk.yummy.tv.core.tv.api.ITvIntegration
import su.afk.yummy.tv.core.tv.api.TvChannelContentProvider
import su.afk.yummy.tv.core.utils.coroutines.di.IoApplicationScope
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
internal class TvIntegration @Inject constructor(
    private val watchProgressStore: WatchProgressStorage,
    private val watchNextManager: WatchNextManager,
    private val previewChannelManager: PreviewChannelManager,
    private val channelContentProvider: TvChannelContentProvider,
    private val settingsStore: AppLifecycleSettingsStore,
    private val errorCoroutineAnalytics: ErrorCoroutineAnalytics,
    @IoApplicationScope private val ioApplicationScope: CoroutineScope,
) : ITvIntegration {

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        errorCoroutineAnalytics.reportCoroutineError(owner = "TvIntegration", throwable = throwable)
    }

    // Тот же Job, что и у ioApplicationScope — это осознанно (TvIntegration Singleton, живёт весь
    // процесс, отдельного onDestroy/cancel для него нет), поэтому добавление exception handler
    // поверх безопасно: scope здесь никогда не отменяется.
    private val scope =
        CoroutineScope(ioApplicationScope.coroutineContext + coroutineExceptionHandler)

    override val browsableChannelRequest: SharedFlow<Long> = previewChannelManager.browsableRequest
    override val previewChannelBrowsable: StateFlow<Boolean> = previewChannelManager.isBrowsable

    override fun requestPreviewChannelBrowsable() {
        previewChannelManager.requestBrowsable()
    }

    override fun refreshPreviewChannelStatus() {
        scope.launch { previewChannelManager.checkBrowsable() }
    }

    override fun bindBrowsableChannelRequests(
        activityResultCaller: ActivityResultCaller,
        scope: CoroutineScope,
    ) {
        val launcher = activityResultCaller.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { refreshPreviewChannelStatus() }

        scope.launch {
            browsableChannelRequest.collect { channelId ->
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return@collect
                runCatching {
                    launcher.launch(
                        Intent(TvContract.ACTION_REQUEST_CHANNEL_BROWSABLE)
                            .putExtra(TvContract.EXTRA_CHANNEL_ID, channelId)
                    )
                }
            }
        }
    }

    override fun start() {
        scope.launch { previewChannelManager.checkBrowsable() }

        scope.launch {
            combine(
                watchProgressStore.observeContinueWatching().distinctUntilChanged(),
                settingsStore.watchNextEnabled.distinctUntilChanged(),
            ) { localEntries, enabled ->
                if (enabled) {
                    localEntries
                } else {
                    emptyList()
                }
            }
                .collect { entries -> watchNextManager.sync(entries) }
        }

        scope.launch {
            // Задержка, чтобы не соревноваться за сеть/CPU с загрузкой домашнего экрана при холодном
            // старте — синхронизация preview-канала не блокирует ничего и может подождать.
            delay(5.seconds)
            runSuspendCatching { channelContentProvider.newReleases() }
                .onSuccess(previewChannelManager::syncNewContent)
        }
    }
}
