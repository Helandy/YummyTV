package su.afk.yummy.tv.core.tv

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.watchprogress.ContinueWatchingMerge
import su.afk.yummy.tv.core.storage.watchprogress.RemoteContinueWatchingStore
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.core.tv.api.ITvIntegration
import su.afk.yummy.tv.domain.home.usecase.GetHomeFeedUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class TvIntegration @Inject constructor(
    private val watchProgressStore: WatchProgressStore,
    private val remoteContinueWatchingStore: RemoteContinueWatchingStore,
    private val watchNextManager: WatchNextManager,
    private val previewChannelManager: PreviewChannelManager,
    private val getHomeFeed: GetHomeFeedUseCase,
    private val settingsStore: SettingsStore,
) : ITvIntegration {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val browsableChannelRequest: SharedFlow<Long> = previewChannelManager.browsableRequest
    override val previewChannelBrowsable: StateFlow<Boolean> = previewChannelManager.isBrowsable

    override fun requestPreviewChannelBrowsable() {
        previewChannelManager.requestBrowsable()
    }

    override fun refreshPreviewChannelStatus() {
        scope.launch { previewChannelManager.checkBrowsable() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun start() {
        scope.launch { previewChannelManager.checkBrowsable() }

        scope.launch {
            val remoteContinueWatching = combine(
                settingsStore.yaniUserId,
                settingsStore.yaniContentLanguage,
            ) { userId, language ->
                val accountKey = if (userId > 0) "user:$userId" else "anon"
                accountKey to language.apiCode
            }
                .distinctUntilChanged()
                .flatMapLatest { (accountKey, languageCode) ->
                    remoteContinueWatchingStore.observe(accountKey, languageCode)
                }

            combine(
                watchProgressStore.observeContinueWatching().distinctUntilChanged(),
                remoteContinueWatching.distinctUntilChanged(),
                settingsStore.watchNextEnabled.distinctUntilChanged(),
            ) { localEntries, remoteEntries, enabled ->
                if (enabled) {
                    ContinueWatchingMerge.merge(
                        feedEntries = remoteEntries,
                        localEntries = localEntries,
                    )
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
