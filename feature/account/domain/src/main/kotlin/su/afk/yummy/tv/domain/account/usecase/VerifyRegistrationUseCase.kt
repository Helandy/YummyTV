package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.AccountRepository
import javax.inject.Inject

/** Подтверждает регистрацию аккаунта Yani по проверочному хешу. */
class VerifyRegistrationUseCase @Inject constructor(private val repository: AccountRepository) {
    suspend operator fun invoke(hash: String) = repository.verifyRegistration(hash)
}
