package su.afk.yummy.tv.domain.update.usecase

import su.afk.yummy.tv.domain.update.repository.UpdateDownloadRepository
import javax.inject.Inject

/**
 * Запускает фоновую загрузку APK обновления. Загрузка продолжается, пока приложение свёрнуто,
 * а повторный запуск для уже идущей загрузки её не перезапускает.
 */
class StartUpdateDownloadUseCase @Inject constructor(
    private val repository: UpdateDownloadRepository,
) {
    operator fun invoke(url: String) = repository.start(url)
}
