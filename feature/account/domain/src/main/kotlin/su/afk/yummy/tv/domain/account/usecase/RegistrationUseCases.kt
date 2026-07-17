package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.UserRegistration
import su.afk.yummy.tv.domain.account.repository.AccountRepository
import javax.inject.Inject

class RegisterUserUseCase @Inject constructor(private val repository: AccountRepository) {
    suspend operator fun invoke(registration: UserRegistration) = repository.register(registration)
}

class VerifyRegistrationUseCase @Inject constructor(private val repository: AccountRepository) {
    suspend operator fun invoke(hash: String) = repository.verifyRegistration(hash)
}
