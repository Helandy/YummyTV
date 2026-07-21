package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.ProfileUpdate
import su.afk.yummy.tv.domain.account.repository.ProfileSettingsRepository
import javax.inject.Inject

/** Обновляет редактируемые данные профиля текущего пользователя. */
class UpdateProfileUseCase @Inject constructor(private val repository: ProfileSettingsRepository) {
    suspend operator fun invoke(update: ProfileUpdate) = repository.updateProfile(update)
}
