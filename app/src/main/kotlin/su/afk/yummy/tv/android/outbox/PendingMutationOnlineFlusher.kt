package su.afk.yummy.tv.android.outbox

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.network.connectivity.NetworkConnectivityMonitor
import su.afk.yummy.tv.core.utils.coroutines.di.DefaultApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Дожимает очередь отложенных мутаций сразу, как только появилась сеть: при старте приложения и
 * при каждом возврате связи. Без этого мутация после сбоя ждала бы backoff WorkManager.
 */
@Singleton
class PendingMutationOnlineFlusher @Inject constructor(
    private val connectivity: NetworkConnectivityMonitor,
    private val scheduler: AndroidPendingMutationSyncScheduler,
    // Живёт весь процесс, отдельного cancel() нет — осознанно, как у остальных координаторов.
    @DefaultApplicationScope private val scope: CoroutineScope,
) {
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            connectivity.isOnline
                .filter { it }
                .collect { scheduler.flushNow() }
        }
    }
}
