package su.afk.yummy.tv.core.tv

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import su.afk.yummy.tv.core.tv.api.TvChannelContentProvider
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching

@HiltWorker
class HomeFeedRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val channelContentProvider: TvChannelContentProvider,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result =
        runSuspendCatching { channelContentProvider.refresh() }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
}
