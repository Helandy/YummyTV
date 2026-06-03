package su.afk.yummy.tv.android.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import su.afk.yummy.tv.domain.home.usecase.RefreshHomeFeedUseCase

@HiltWorker
class HomeFeedRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val refreshHomeFeed: RefreshHomeFeedUseCase,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result =
        runCatching { refreshHomeFeed() }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
}
