package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.YaniAccount
import su.afk.yummy.tv.domain.account.repository.AccountRepository

/** Authenticates with Yani credentials and returns the signed-in account. */
class LoginUseCase(private val repository: AccountRepository) {
    suspend operator fun invoke(
        login: String,
        password: String,
        captchaResponse: String? = null,
    ): YaniAccount = repository.login(login, password, captchaResponse)
}
