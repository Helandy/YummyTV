package su.afk.yummy.tv.domain.update.usecase

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.update.model.UpdateDownloadState
import su.afk.yummy.tv.domain.update.repository.UpdateDownloadRepository
import javax.inject.Inject

/**
 * Наблюдает за состоянием фоновой загрузки APK обновления по [url]: прогресс, готовый файл
 * или ошибку. Позволяет заново подключиться к уже идущей или завершённой загрузке.
 */
class ObserveUpdateDownloadUseCase @Inject constructor(
    private val repository: UpdateDownloadRepository,
) {
    operator fun invoke(url: String): Flow<UpdateDownloadState> = repository.observe(url)
}
