package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.ProfileSettingsRepository
import javax.inject.Inject

/** Загружает редактируемые данные профиля текущего пользователя. */
class GetEditableProfileUseCase @Inject constructor(private val repository: ProfileSettingsRepository) {
    suspend operator fun invoke() = repository.getProfile()
}
