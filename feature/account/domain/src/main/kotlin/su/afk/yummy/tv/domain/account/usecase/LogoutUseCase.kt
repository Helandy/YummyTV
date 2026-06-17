package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.AccountRepository
import javax.inject.Inject

/** Выходит из аккаунта Yani и очищает сохранённую сессию. */
class LogoutUseCase @Inject constructor(private val repository: AccountRepository) {
    suspend operator fun invoke() = repository.logout()
}
