package su.afk.yummy.tv.data.update.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.data.update.R
import su.afk.yummy.tv.domain.update.model.UpdateDownloadState
import su.afk.yummy.tv.domain.update.repository.ApkDownloadRepository
import javax.inject.Inject
import kotlin.math.roundToInt

/** Скачивает APK и публикует прогресс и итог в [UpdateDownloadStateHolder]; общий для сервиса и запасного пути. */
internal class UpdateDownloadRunner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apkDownloadRepository: ApkDownloadRepository,
    private val stateHolder: UpdateDownloadStateHolder,
) {
    /** [onPercent] вызывается при изменении целого процента прогресса. Возвращает итоговое состояние. */
    suspend fun run(url: String, onPercent: (Int) -> Unit = {}): UpdateDownloadState {
        stateHolder.update(url, UpdateDownloadState.Downloading(progress = 0f))
        var lastPercent = -1
        val result = try {
            val file = apkDownloadRepository.download(url) { progress ->
                val percent = (progress * PERCENT_MAX).roundToInt()
                if (percent == lastPercent) return@download
                lastPercent = percent
                stateHolder.update(url, UpdateDownloadState.Downloading(progress))
                onPercent(percent)
            }
            UpdateDownloadState.Downloaded(file)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UpdateDownloadState.Failed(e.message ?: context.getString(R.string.update_download_error_unknown))
        }
        stateHolder.update(url, result)
        return result
    }

    private companion object {
        const val PERCENT_MAX = 100
    }
}
