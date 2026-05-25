package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.*
import su.afk.yummy.tv.domain.account.repository.*

/** Refreshes the stored Yani session and returns the latest account profile. */
class RefreshAccountUseCase(private val repository: AccountRepository) {
    suspend operator fun invoke(): YaniAccount? = repository.refreshToken()
}
