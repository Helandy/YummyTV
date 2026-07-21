package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.ProfileSettingsRepository
import javax.inject.Inject

/** Заменяет пароль текущего пользователя после проверки старого пароля. */
class ChangePasswordUseCase @Inject constructor(private val repository: ProfileSettingsRepository) {
    suspend operator fun invoke(oldPassword: String, newPassword: String) =
        repository.changePassword(oldPassword, newPassword)
}
