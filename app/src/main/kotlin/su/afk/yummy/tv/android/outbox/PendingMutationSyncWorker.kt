package su.afk.yummy.tv.android.outbox

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Фоновый прогон очереди отложенных мутаций; сама логика и порядок записей — в [PendingMutationFlushRunner]. */
@HiltWorker
class PendingMutationSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val flushRunner: PendingMutationFlushRunner,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result =
        if (flushRunner.flush()) Result.success() else Result.retry()
}
