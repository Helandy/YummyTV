package su.afk.yummy.tv.android.startup

import androidx.tracing.trace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import su.afk.yummy.tv.android.episodepush.NewEpisodePushScheduler
import su.afk.yummy.tv.android.outbox.AndroidPendingMutationSyncScheduler
import su.afk.yummy.tv.android.outbox.PendingMutationOnlineFlusher
import su.afk.yummy.tv.core.tv.HomeFeedRefreshScheduler
import su.afk.yummy.tv.core.utils.coroutines.TrustedAppClock
import su.afk.yummy.tv.core.utils.coroutines.di.DefaultApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Запускает фоновые работы процесса вне main-потока: доверенное время, периодические задачи
 * WorkManager и синхронизацию пуш-подписки с сессией.
 */
@Singleton
class BackgroundWorkStarter @Inject constructor(
    private val trustedAppClock: TrustedAppClock,
    private val homeFeedRefreshScheduler: HomeFeedRefreshScheduler,
    private val newEpisodePushScheduler: NewEpisodePushScheduler,
    private val pendingMutationSyncScheduler: AndroidPendingMutationSyncScheduler,
    private val pendingMutationOnlineFlusher: PendingMutationOnlineFlusher,
    @DefaultApplicationScope private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch { trace("App.trustedTime") { trustedAppClock.start() } }
        // Периодические задачи с KEEP: первый WorkManager.getInstance() поднимает WorkManager и
        // его Room-базу, на main это стоило бы каждому холодному старту.
        scope.launch {
            trace("App.schedulers") {
                homeFeedRefreshScheduler.schedule()
                newEpisodePushScheduler.scheduleLibraryPush()
                pendingMutationSyncScheduler.schedule()
                pendingMutationOnlineFlusher.start()
            }
        }
        scope.launch { newEpisodePushScheduler.syncProfilePushWithSession() }
    }
}
