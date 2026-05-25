package su.afk.yummy.tv.domain.account

/** Authenticates with Yani credentials and returns the signed-in account. */
class LoginUseCase(private val repository: AccountRepository) {
    suspend operator fun invoke(login: String, password: String): YaniAccount = repository.login(login, password)
}
