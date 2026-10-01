package su.afk.yummy.tv.data.update.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import su.afk.yummy.tv.data.update.notification.UpdateDownloadNotificationService
import su.afk.yummy.tv.domain.update.model.UpdateDownloadState
import javax.inject.Inject

/** Foreground-сервис загрузки APK: живёт, пока файл скачивается, даже если приложение свёрнуто. */
@AndroidEntryPoint
internal class UpdateDownloadService : Service() {

    @Inject
    lateinit var runner: UpdateDownloadRunner

    @Inject
    lateinit var notifications: UpdateDownloadNotificationService

    @Inject
    lateinit var stateHolder: UpdateDownloadStateHolder

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadJob: Job? = null
    private var downloadUrl: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> intent.getStringExtra(EXTRA_URL)?.let(::startDownload) ?: stopSelf()
            ACTION_CANCEL -> cancelDownload()
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startDownload(url: String) {
        // Обязателен в течение нескольких секунд после startForegroundService, поэтому первым делом.
        startInForeground(notifications.createProgress(progressPercent = 0))
        if (downloadJob?.isActive == true && downloadUrl == url) return

        downloadJob?.cancel()
        downloadUrl = url
        downloadJob = scope.launch {
            val result = runner.run(url) { percent ->
                notifications.notify(notifications.createProgress(percent))
            }
            finish()
            if (result is UpdateDownloadState.Downloaded) notifications.showDownloaded()
        }
    }

    private fun cancelDownload() {
        downloadUrl?.let { stateHolder.update(it, UpdateDownloadState.Idle) }
        downloadJob?.cancel()
        finish()
    }

    private fun startInForeground(notification: Notification) {
        ServiceCompat.startForeground(
            this,
            UpdateDownloadNotificationService.NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    private fun finish() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "su.afk.yummy.tv.update.download.START"
        const val ACTION_CANCEL = "su.afk.yummy.tv.update.download.CANCEL"
        const val EXTRA_URL = "url"
    }
}
