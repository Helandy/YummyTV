package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.*
import su.afk.yummy.tv.domain.account.repository.*

/** Signs out from Yani and clears the stored account session. */
class LogoutUseCase(private val repository: AccountRepository) {
    suspend operator fun invoke() = repository.logout()
}
