package su.afk.yummy.tv.data.videodownload.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import su.afk.yummy.tv.domain.videodownload.usecase.PauseVideoDownloadUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.RestartVideoDownloadUseCase
import javax.inject.Inject

@AndroidEntryPoint
class VideoDownloadNotificationReceiver : BroadcastReceiver() {
    @Inject
    lateinit var pauseVideoDownload: PauseVideoDownloadUseCase

    @Inject
    lateinit var restartVideoDownload: RestartVideoDownloadUseCase

    override fun onReceive(context: Context, intent: Intent) {
        val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, 0L).takeIf { it > 0L } ?: return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_PAUSE -> pause(downloadId)
                    ACTION_RESUME -> resume(downloadId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun pause(downloadId: Long) {
        pauseVideoDownload(downloadId)
    }

    private suspend fun resume(downloadId: Long) {
        restartVideoDownload(downloadId)
    }

    companion object {
        const val ACTION_PAUSE = "su.afk.yummy.tv.action.PAUSE_VIDEO_DOWNLOAD"
        const val ACTION_RESUME = "su.afk.yummy.tv.action.RESUME_VIDEO_DOWNLOAD"
        const val EXTRA_DOWNLOAD_ID = "download_id"
    }
}
