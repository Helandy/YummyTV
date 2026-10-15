package su.afk.yummy.tv.data.update.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.data.update.R
import su.afk.yummy.tv.domain.update.model.UpdateDownloadState
import su.afk.yummy.tv.domain.update.repository.ApkDownloadRepository
import su.afk.yummy.tv.core.featuretoggle.api.FeatureFlags
import su.afk.yummy.tv.core.featuretoggle.api.FeatureToggleProvider
import java.io.File
import javax.inject.Inject
import kotlin.math.roundToInt

/** Скачивает APK и публикует прогресс и итог в [UpdateDownloadStateHolder]; общий для сервиса и запасного пути. */
internal class UpdateDownloadRunner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apkDownloadRepository: ApkDownloadRepository,
    private val stateHolder: UpdateDownloadStateHolder,
    private val featureToggleProvider: FeatureToggleProvider,
) {
    /** [onPercent] вызывается при изменении целого процента прогресса. Возвращает итоговое состояние. */
    suspend fun run(url: String, onPercent: (Int) -> Unit = {}): UpdateDownloadState {
        stateHolder.update(url, UpdateDownloadState.Downloading(progress = 0f))
        val result = try {
            val file = try {
                download(url, url, onPercent)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Основная ссылка не скачалась — резервная прямая ссылка; состояние остаётся под ключом [url].
                val fallback = fallbackUrl(url) ?: throw e
                stateHolder.update(url, UpdateDownloadState.Downloading(progress = 0f))
                download(fallback, url, onPercent)
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

    private suspend fun download(
        source: String,
        stateKey: String,
        onPercent: (Int) -> Unit,
    ): File {
        var lastPercent = -1
        return apkDownloadRepository.download(source) { progress ->
            val percent = (progress * PERCENT_MAX).roundToInt()
            if (percent == lastPercent) return@download
            lastPercent = percent
            stateHolder.update(stateKey, UpdateDownloadState.Downloading(progress))
            onPercent(percent)
        }
    }

    private fun fallbackUrl(primary: String): String? =
        featureToggleProvider.getString(FeatureFlags.updateFallbackApkUrl).trim()
            .takeIf { it.startsWith("https://", ignoreCase = true) && it != primary }

    private companion object {
        const val PERCENT_MAX = 100
    }
}
