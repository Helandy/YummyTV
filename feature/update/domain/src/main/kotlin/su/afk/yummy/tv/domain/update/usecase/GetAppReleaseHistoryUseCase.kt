package su.afk.yummy.tv.domain.update.usecase

import su.afk.yummy.tv.domain.update.model.AppReleaseNotes
import su.afk.yummy.tv.domain.update.repository.UpdateRepository
import javax.inject.Inject

/**
 * Возвращает списки изменений релизов не новее установленной версии, от новых к старым.
 * Pre-release попадают в историю только при [includePrerelease] (включённый бета-канал).
 */
class GetAppReleaseHistoryUseCase @Inject constructor(
    private val updateRepository: UpdateRepository,
) {
    suspend operator fun invoke(currentVersion: String, includePrerelease: Boolean): List<AppReleaseNotes> =
        updateRepository.releaseHistory(currentVersion, includePrerelease)
}
