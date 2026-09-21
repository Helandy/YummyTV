package su.afk.yummy.tv.domain.update.usecase

import su.afk.yummy.tv.domain.update.model.AppRelease
import su.afk.yummy.tv.domain.update.repository.UpdateRepository
import javax.inject.Inject

/** Возвращает последний опубликованный релиз приложения для проверки обновлений. */
class GetLatestAppReleaseUseCase @Inject constructor(
    private val updateRepository: UpdateRepository,
) {
    suspend operator fun invoke(currentVersion: String, includePrerelease: Boolean): AppRelease? =
        updateRepository.latestRelease(currentVersion, includePrerelease)
}
