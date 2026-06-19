package su.afk.yummy.tv.core.tv

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.analytics.ErrorAnalyticsReporter
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.core.tv.api.ITvIntegration
import su.afk.yummy.tv.domain.home.usecase.GetHomeFeedUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class TvIntegration @Inject constructor(
    private val watchProgressStore: WatchProgressStore,
    private val watchNextManager: WatchNextManager,
    private val previewChannelManager: PreviewChannelManager,
    private val getHomeFeed: GetHomeFeedUseCase,
    private val settingsStore: SettingsStore,
    private val errorAnalyticsReporter: ErrorAnalyticsReporter,
) : ITvIntegration {

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        errorAnalyticsReporter.reportCoroutineError(owner = "TvIntegration", throwable = throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + coroutineExceptionHandler)

    override val browsableChannelRequest: SharedFlow<Long> = previewChannelManager.browsableRequest
    override val previewChannelBrowsable: StateFlow<Boolean> = previewChannelManager.isBrowsable

    override fun requestPreviewChannelBrowsable() {
        previewChannelManager.requestBrowsable()
    }

    override fun refreshPreviewChannelStatus() {
        scope.launch { previewChannelManager.checkBrowsable() }
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
            delay(5_000)
            runCatching { getHomeFeed() }.onSuccess { feed ->
                val newItems = feed.sections
                    .firstOrNull { it.title.contains("нов", ignoreCase = true) }
                    ?.items
                    ?: feed.sections.firstOrNull()?.items
                    ?: emptyList()
                previewChannelManager.syncNewContent(newItems)
            }
        }
    }
}
