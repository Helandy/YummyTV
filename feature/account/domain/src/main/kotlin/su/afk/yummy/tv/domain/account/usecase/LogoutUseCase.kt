package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.*
import su.afk.yummy.tv.domain.account.repository.*
import javax.inject.Inject

/** Signs out from Yani and clears the stored account session. */
class LogoutUseCase @Inject constructor(private val repository: AccountRepository) {
    suspend operator fun invoke() = repository.logout()
}
