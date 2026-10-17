package su.afk.yummy.tv.core.tv

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.common.widget.HomeScreenWidgetRefresher
import su.afk.yummy.tv.core.tv.api.TvChannelContentProvider

/**
 * Периодически обновляет ленту главной из сети и заодно перерисовывает виджеты рабочего стола,
 * чтобы они не устаревали, пока приложение закрыто.
 */
@HiltWorker
class HomeFeedRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val channelContentProvider: TvChannelContentProvider,
    private val widgetRefresher: HomeScreenWidgetRefresher,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val result = runSuspendCatching { channelContentProvider.refresh() }
        widgetRefresher.refresh()
        return result.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
    }
}
