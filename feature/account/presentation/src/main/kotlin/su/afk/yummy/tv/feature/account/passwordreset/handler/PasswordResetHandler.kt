package su.afk.yummy.tv.feature.account.passwordreset.handler

import su.afk.yummy.tv.domain.account.usecase.RequestPasswordResetUseCase
import javax.inject.Inject

class PasswordResetHandler @Inject constructor(
    private val requestPasswordReset: RequestPasswordResetUseCase,
) {
    suspend fun request(email: String, captchaResponse: String?) =
        requestPasswordReset(email, captchaResponse)
}
