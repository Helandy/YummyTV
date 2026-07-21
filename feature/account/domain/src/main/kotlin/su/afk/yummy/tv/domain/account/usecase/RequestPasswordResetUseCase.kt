package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.ProfileSettingsRepository
import javax.inject.Inject

/** Запрашивает восстановление пароля аккаунта по адресу электронной почты. */
class RequestPasswordResetUseCase @Inject constructor(private val repository: ProfileSettingsRepository) {
    suspend operator fun invoke(email: String, captchaResponse: String? = null) =
        repository.requestPasswordReset(email, captchaResponse)
}
