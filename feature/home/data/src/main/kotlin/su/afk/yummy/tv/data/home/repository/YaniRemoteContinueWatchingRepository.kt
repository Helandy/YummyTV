package su.afk.yummy.tv.data.home.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.core.preferences.settings.currentLanguageCode
import su.afk.yummy.tv.core.storage.document.DocumentCacheStorage
import su.afk.yummy.tv.core.storage.document.getOrFetchJson
import su.afk.yummy.tv.data.home.dto.YaniLastWatchesDto
import su.afk.yummy.tv.data.home.mapper.toHomeContinueWatchingItems
import su.afk.yummy.tv.data.home.network.YaniHomeApi
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.repository.RemoteContinueWatchingRepository

private const val LAST_WATCHES_CACHE_NAMESPACE = "last-watches"
private const val LAST_WATCHES_TTL_MS = 60 * 1000L
private const val TAG = "YaniLastWatches"

class YaniRemoteContinueWatchingRepository(
    private val api: YaniHomeApi,
    private val documentCache: DocumentCacheStorage,
    private val settingsStore: YaniAccountSettingsStore,
    private val analyticsTracker: AnalyticsTracker,
) : RemoteContinueWatchingRepository {

    private val refreshRequests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeLastWatches(): Flow<List<HomeContinueWatchingItem>> =
        settingsStore.yaniUserId
            .map { it.coerceAtLeast(0) }
            .distinctUntilChanged()
            .flatMapLatest { userId ->
                if (userId == 0) flowOf(emptyList()) else observeForUser(userId)
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)

    override fun requestRefresh() {
        refreshRequests.tryEmit(Unit)
    }

    // Сперва кэш любой давности, чтобы карточки появились сразу, затем свежий ответ сервера.
    private fun observeForUser(userId: Int): Flow<List<HomeContinueWatchingItem>> = flow {
        load(userId, ttlMs = Long.MAX_VALUE)?.let { emit(it) }
        load(userId, ttlMs = LAST_WATCHES_TTL_MS)?.let { emit(it) }
        refreshRequests.collect {
            load(userId, ttlMs = LAST_WATCHES_TTL_MS)?.let { emit(it) }
        }
    }

    /** null — ни сети, ни кэша: оставляем то, что уже показано. */
    private suspend fun load(userId: Int, ttlMs: Long): List<HomeContinueWatchingItem>? =
        runSuspendCatching {
            documentCache.getOrFetchJson<YaniLastWatchesDto>(
                cacheKey = cacheKey(userId),
                ttlMs = ttlMs,
                fetch = { api.getLastWatches() },
            ).response.toHomeContinueWatchingItems()
        }.onFailure { error ->
            analyticsTracker.log(TAG, error) { "Last watches unavailable for user=$userId" }
        }.getOrNull()

    private suspend fun cacheKey(userId: Int): String =
        "user:$userId:$LAST_WATCHES_CACHE_NAMESPACE:${settingsStore.currentLanguageCode()}"
}
