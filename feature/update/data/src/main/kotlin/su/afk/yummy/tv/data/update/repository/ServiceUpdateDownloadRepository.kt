package su.afk.yummy.tv.data.update.repository

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import su.afk.yummy.tv.data.update.service.UpdateDownloadRunner
import su.afk.yummy.tv.data.update.service.UpdateDownloadService
import su.afk.yummy.tv.data.update.service.UpdateDownloadStateHolder
import su.afk.yummy.tv.domain.update.model.UpdateDownloadState
import su.afk.yummy.tv.domain.update.repository.UpdateDownloadRepository
import javax.inject.Inject

internal class ServiceUpdateDownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stateHolder: UpdateDownloadStateHolder,
    private val runner: UpdateDownloadRunner,
) : UpdateDownloadRepository {

    private val fallbackScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var fallbackJob: Job? = null
    private var fallbackUrl: String? = null

    override fun start(url: String) {
        val intent = Intent(context, UpdateDownloadService::class.java)
            .setAction(UpdateDownloadService.ACTION_START)
            .putExtra(UpdateDownloadService.EXTRA_URL, url)
        // Если система отказала в старте сервиса, качаем без него: загрузка пойдёт, пока жив процесс.
        runCatching { ContextCompat.startForegroundService(context, intent) }
            .onFailure { downloadWithoutService(url) }
    }

    override fun observe(url: String): Flow<UpdateDownloadState> = stateHolder.observe(url)

    @Synchronized
    private fun downloadWithoutService(url: String) {
        if (fallbackJob?.isActive == true && fallbackUrl == url) return
        fallbackJob?.cancel()
        fallbackUrl = url
        fallbackJob = fallbackScope.launch { runner.run(url) }
    }
}
