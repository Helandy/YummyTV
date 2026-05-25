package su.afk.yummy.tv.domain.account

/** Refreshes the stored Yani session and returns the latest account profile. */
class RefreshAccountUseCase(private val repository: AccountRepository) {
    suspend operator fun invoke(): YaniAccount? = repository.refreshToken()
}
