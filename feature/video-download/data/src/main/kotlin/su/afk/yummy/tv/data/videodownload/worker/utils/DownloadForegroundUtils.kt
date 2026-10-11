package su.afk.yummy.tv.data.videodownload.worker.utils

import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import kotlin.coroutines.cancellation.CancellationException

/**
 * Переводит воркер в foreground, а если система это запретила, продолжает работу без него.
 *
 * С Android 12 `startForegroundService()` из фона бросает `ForegroundServiceStartNotAllowedException`
 * (наследник [IllegalStateException]). WorkManager перезапускает воркер именно из фона: после
 * возврата сети (авиарежим, смена сети), после остановки по ограничениям и после смерти процесса.
 * Неперехваченное исключение завершало воркер результатом FAILURE, и загрузка не возобновлялась.
 * Без foreground работа идёт как обычная фоновая: остановку системой повторяет WorkManager.
 *
 * @return true, если воркер переведён в foreground.
 */
internal suspend fun CoroutineWorker.setForegroundIfAllowed(info: ForegroundInfo): Boolean =
    try {
        setForeground(info)
        true
    } catch (error: CancellationException) {
        // CancellationException тоже IllegalStateException: отмену не гасим.
        throw error
    } catch (error: IllegalStateException) {
        false
    }
