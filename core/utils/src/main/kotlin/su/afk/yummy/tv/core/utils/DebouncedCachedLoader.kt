package su.afk.yummy.tv.core.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.time.Duration.Companion.milliseconds

class DebouncedCachedLoader<K, V : Any>(
    private val loadValue: suspend (K) -> V?,
    maxConcurrentPrefetch: Int = DEFAULT_PREFETCH_LIMIT,
) {
    private var focusedLoadJob: Job? = null
    private val cache = mutableMapOf<K, V>()
    private val prefetching = mutableSetOf<K>()
    private val prefetchSemaphore = Semaphore(maxConcurrentPrefetch.coerceAtLeast(1))

    fun focus(
        scope: CoroutineScope,
        key: K,
        debounceMs: Long = DEFAULT_FOCUS_DEBOUNCE_MS,
        isCurrentFocus: () -> Boolean,
        onCachedValue: (V?, Map<K, V>) -> Unit,
        onLoadedValue: (DebouncedCachedLoadResult<K, V>) -> Unit,
    ) {
        focusedLoadJob?.cancel()
        onCachedValue(cache[key], cache.toMap())
        if (cache.containsKey(key)) return

        focusedLoadJob = scope.launch {
            delay(debounceMs.milliseconds)
            runCatching { loadValue(key) }.onSuccess { value ->
                if (value == null) return@onSuccess
                cache[key] = value
                onLoadedValue(
                    DebouncedCachedLoadResult(
                        key = key,
                        value = value,
                        cache = cache.toMap(),
                        isCurrentFocus = isCurrentFocus(),
                    )
                )
            }
        }
    }

    fun cancelFocus() {
        focusedLoadJob?.cancel()
        focusedLoadJob = null
    }

    fun prefetch(
        scope: CoroutineScope,
        key: K,
        onCacheChanged: (Map<K, V>) -> Unit,
    ) {
        if (cache.containsKey(key) || !prefetching.add(key)) return
        scope.launch {
            try {
                prefetchSemaphore.withPermit {
                    if (cache.containsKey(key)) return@withPermit
                    runCatching { loadValue(key) }.onSuccess { value ->
                        if (value == null) return@onSuccess
                        cache[key] = value
                        onCacheChanged(cache.toMap())
                    }
                }
            } finally {
                prefetching.remove(key)
            }
        }
    }

    fun prefetchAll(
        scope: CoroutineScope,
        keys: List<K>,
        onCacheChanged: (Map<K, V>) -> Unit,
    ) {
        keys.forEach { key -> prefetch(scope, key, onCacheChanged) }
    }

    private companion object {
        const val DEFAULT_FOCUS_DEBOUNCE_MS = 600L
        const val DEFAULT_PREFETCH_LIMIT = 2
    }
}

data class DebouncedCachedLoadResult<K, V>(
    val key: K,
    val value: V,
    val cache: Map<K, V>,
    val isCurrentFocus: Boolean,
)
