package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.YaniAccount
import su.afk.yummy.tv.domain.account.repository.AccountRepository
import javax.inject.Inject

/** Обновляет сохранённую сессию Yani и возвращает актуальный профиль. */
class RefreshAccountUseCase @Inject constructor(private val repository: AccountRepository) {
    suspend operator fun invoke(): YaniAccount? = repository.refreshToken()
}
